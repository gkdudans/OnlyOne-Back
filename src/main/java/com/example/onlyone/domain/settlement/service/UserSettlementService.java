package com.example.onlyone.domain.settlement.service;

import com.example.onlyone.domain.schedule.repository.ScheduleRepository;
import com.example.onlyone.domain.settlement.dto.event.WalletCaptureFailedEvent;
import com.example.onlyone.domain.settlement.dto.event.WalletCaptureSucceededEvent;
import com.example.onlyone.domain.settlement.entity.SettlementStatus;
import com.example.onlyone.domain.settlement.entity.UserSettlement;
import com.example.onlyone.domain.settlement.repository.SettlementRepository;
import com.example.onlyone.domain.settlement.repository.UserSettlementRepository;
import com.example.onlyone.domain.user.entity.User;
import com.example.onlyone.domain.user.repository.UserRepository;
import com.example.onlyone.domain.wallet.entity.Wallet;
import com.example.onlyone.domain.wallet.repository.WalletRepository;
import com.example.onlyone.domain.wallet.service.WalletService;
import com.example.onlyone.global.exception.CustomException;
import com.example.onlyone.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class UserSettlementService {
    private final UserSettlementRepository userSettlementRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final SettlementRepository settlementRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    private final OutboxAppender outboxAppender;
    private final FailedEventAppender failedEventAppender;

    /**
     * 참가자별 개별 정산 처리 (독립 트랜잭션)
     * - true  반환: 차감 성공 (금액 누적 대상)
     * - false 반환: 잔액 부족 — 관대 모드로 FAILED 처리 후 건너뜀 (예외 미전파)
     * - 예외 throw: 시스템 오류 — FAILED 마킹 없이 상위 재시도에 위임
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean processParticipantSettlement(Long settlementId,
                                                Long leaderId,
                                                Long leaderWalletId,
                                                Long participantId,
                                                Long amount) {
        UserSettlement us = userSettlementRepository
                .findBySettlement_SettlementIdAndUser_UserId(settlementId, participantId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_SETTLEMENT_NOT_FOUND));

        if (us.getSettlementStatus() == SettlementStatus.COMPLETED) {
            return true; // 멱등 스킵
        }

        User participant = userRepository.findById(participantId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Wallet memberWallet = walletRepository.findByUserWithoutLock(participant)
                .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
        Long memberWalletId = memberWallet.getWalletId();
        String operationId = ("stl:%d:usr:%d:v1").formatted(settlementId, participantId);

        try {
            int captured = walletRepository.captureHold(participantId, amount);
            if (captured != 1) {
                throw new CustomException(ErrorCode.WALLET_HOLD_CAPTURE_FAILED);
            }
            long postDeductBalance = memberWallet.getPostedBalance() - amount; // 차감 후 잔액
            us.updateUserSettlement(SettlementStatus.COMPLETED, LocalDateTime.now());
            userSettlementRepository.save(us);
            var payload = new HashMap<String, Object>();
            payload.put("type", "SUCCESS");
            payload.put("operationId", operationId);
            payload.put("occurredAt", java.time.Instant.now().toString());
            payload.put("settlementId", settlementId);
            payload.put("userSettlementId", us.getUserSettlementId());
            payload.put("participantId", participantId);
            payload.put("memberWalletId", memberWalletId);
            payload.put("leaderId", leaderId);
            payload.put("leaderWalletId", leaderWalletId);
            payload.put("amount", amount);
            payload.put("memberBalance", postDeductBalance);
            outboxAppender.append("UserSettlement", us.getUserSettlementId(),
                    "ParticipantSettlementResult", String.valueOf(memberWalletId), payload);
            return true;
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.WALLET_HOLD_CAPTURE_FAILED) {
                // 잔액 부족: 홀드 해제 + FAILED 처리 후 건너뜀 (예외 미전파)
                walletRepository.releaseHoldBalance(participantId, amount);
                us.updateUserSettlement(SettlementStatus.FAILED, LocalDateTime.now());
                userSettlementRepository.save(us);
                failedEventAppender.appendFailedUserSettlementEvent(
                        settlementId, us.getUserSettlementId(), participantId,
                        memberWalletId, leaderId, leaderWalletId, amount,
                        memberWallet.getPostedBalance() // 차감 실패 → 잔액 그대로
                );
                return false;
            }
            throw e;
        }
        // 그 외 Exception: FAILED 마킹 없이 throw → REQUIRES_NEW 롤백 후 재시도 가능
    }

//
//    /**
//     * 참가자별 개별 정산 처리 (독립 트랜잭션)
//     */
//    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
//    public void processParticipantSettlement(Long settlementId, Long leaderWalletId, Long participantId, Long amount) {
//        redisLuaService.withWalletGate(participantId, "capture", 10, () -> {
//            UserSettlement userSettlement = userSettlementRepository
//                    .findBySettlement_SettlementIdAndUser_UserId(settlementId, participantId)
//                    .orElseThrow(() -> new CustomException(ErrorCode.USER_SETTLEMENT_NOT_FOUND));
//
//            // 이미 처리된 경우 스킵 (멱등성)
//            if (userSettlement.getSettlementStatus() == SettlementStatus.COMPLETED) {
//                return;
//            }
//            // 홀드 캡처 (차감)
//            int captured = walletRepository.captureHold(participantId, amount);
//            if (captured != 1) {
//                throw new CustomException(ErrorCode.WALLET_HOLD_CAPTURE_FAILED);
//            }
//            // 상태 변경
//            userSettlement.updateUserSettlement(SettlementStatus.COMPLETED, LocalDateTime.now());
//            userSettlementRepository.save(userSettlement);
//            // 트랜잭션 기록
//            User participant = userRepository.findById(participantId)
//                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
//            Wallet memberWallet = walletRepository.findByUserWithoutLock(participant)
//                    .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
//            walletService.createSuccessfulWalletTransactions(memberWallet.getWalletId(), leaderWalletId, amount, userSettlement);
//        });
//    }

    /**
     * 리더에게 총액 가산 (독립 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void creditToLeader(Long leaderId, long totalAmount) {
        int credited = walletRepository.creditByUserId(leaderId, totalAmount);
        if (credited != 1) {
            throw new CustomException(ErrorCode.WALLET_CREDIT_APPLY_FAILED);
        }
    }

    /**
     * 정산 처리 중 시스템 오류 발생 시 Settlement 상태를 FAILED로 복구 (독립 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverSettlementToFailed(Long settlementId) {
        settlementRepository.markFailed(settlementId);
    }
}
