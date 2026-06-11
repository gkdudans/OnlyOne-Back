package com.example.onlyone.domain.settlement.service;

import com.example.onlyone.domain.club.entity.Club;
import com.example.onlyone.domain.club.entity.ClubRole;
import com.example.onlyone.domain.club.entity.UserClub;
import com.example.onlyone.domain.club.repository.ClubRepository;
import com.example.onlyone.domain.club.repository.UserClubRepository;
import com.example.onlyone.domain.interest.entity.Category;
import com.example.onlyone.domain.interest.entity.Interest;
import com.example.onlyone.domain.interest.repository.InterestRepository;
import com.example.onlyone.domain.notification.service.NotificationService;
import com.example.onlyone.domain.schedule.entity.Schedule;
import com.example.onlyone.domain.schedule.entity.ScheduleRole;
import com.example.onlyone.domain.schedule.entity.ScheduleStatus;
import com.example.onlyone.domain.schedule.entity.UserSchedule;
import com.example.onlyone.domain.schedule.repository.ScheduleRepository;
import com.example.onlyone.domain.schedule.repository.UserScheduleRepository;
import com.example.onlyone.domain.settlement.dto.event.OutboxEvent;
import com.example.onlyone.domain.settlement.entity.*;
import com.example.onlyone.domain.settlement.repository.OutboxRepository;
import com.example.onlyone.domain.settlement.repository.SettlementRepository;
import com.example.onlyone.domain.settlement.repository.UserSettlementRepository;
import com.example.onlyone.domain.user.entity.Gender;
import com.example.onlyone.domain.user.entity.Status;
import com.example.onlyone.domain.user.entity.User;
import com.example.onlyone.domain.user.repository.UserRepository;
import com.example.onlyone.domain.user.service.UserService;
import com.example.onlyone.domain.wallet.entity.Wallet;
import com.example.onlyone.domain.wallet.repository.WalletRepository;
import com.example.onlyone.global.exception.CustomException;
import com.example.onlyone.global.exception.ErrorCode;
import com.example.onlyone.config.TestConfig;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.data.redis.repositories.enabled=false")
@Import(TestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SettlementRefactoredTest {

    @Autowired SettlementService settlementService;
    @Autowired UserSettlementService userSettlementService;
    @Autowired UserRepository userRepository;
    @Autowired WalletRepository walletRepository;
    @Autowired ClubRepository clubRepository;
    @Autowired UserClubRepository userClubRepository;
    @Autowired InterestRepository interestRepository;
    @Autowired ScheduleRepository scheduleRepository;
    @Autowired UserScheduleRepository userScheduleRepository;
    @Autowired SettlementRepository settlementRepository;
    @Autowired UserSettlementRepository userSettlementRepository;
    @Autowired OutboxRepository outboxRepository;
    @Autowired PlatformTransactionManager txManager;

    @MockitoBean UserService userService;
    @MockitoBean NotificationService notificationService;

    private TransactionTemplate tx;

    // 테스트에서 생성한 엔티티 ID (AfterEach 정리용)
    private Long leaderId, memberId, interestId, clubId, scheduleId, settlementId;
    private Long leaderWalletId, memberWalletId;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);

        tx.execute(status -> {
            Interest interest = interestRepository.save(
                    Interest.builder().category(Category.EXERCISE).build());
            interestId = interest.getInterestId();

            User leader = userRepository.save(User.builder()
                    .kakaoId(System.nanoTime())
                    .nickname("테스트리더")
                    .status(Status.ACTIVE)
                    .gender(Gender.MALE)
                    .birth(LocalDate.of(1990, 1, 1))
                    .build());
            leaderId = leader.getUserId();

            User member = userRepository.save(User.builder()
                    .kakaoId(System.nanoTime())
                    .nickname("테스트멤버")
                    .status(Status.ACTIVE)
                    .gender(Gender.FEMALE)
                    .birth(LocalDate.of(1995, 1, 1))
                    .build());
            memberId = member.getUserId();

            Wallet leaderWallet = walletRepository.save(
                    Wallet.builder().user(leader).postedBalance(500_000L).pendingOut(0L).build());
            leaderWalletId = leaderWallet.getWalletId();

            Wallet memberWallet = walletRepository.save(
                    Wallet.builder().user(member).postedBalance(500_000L).pendingOut(10_000L).build());
            memberWalletId = memberWallet.getWalletId();

            Club club = clubRepository.save(Club.builder()
                    .name("테스트모임").interest(interest).description("desc")
                    .userLimit(10).memberCount(2L)
                    .city("서울").district("강남구")
                    .build());
            clubId = club.getClubId();

            userClubRepository.save(UserClub.builder().user(leader).club(club).clubRole(ClubRole.LEADER).build());
            userClubRepository.save(UserClub.builder().user(member).club(club).clubRole(ClubRole.MEMBER).build());

            Schedule schedule = scheduleRepository.save(Schedule.builder()
                    .club(club).name("테스트정모").location("강남")
                    .cost(10_000L).userLimit(10)
                    .scheduleStatus(ScheduleStatus.ENDED)
                    .scheduleTime(LocalDateTime.now().minusHours(2))
                    .build());
            scheduleId = schedule.getScheduleId();

            userScheduleRepository.save(UserSchedule.builder().user(leader).schedule(schedule).scheduleRole(ScheduleRole.LEADER).build());
            userScheduleRepository.save(UserSchedule.builder().user(member).schedule(schedule).scheduleRole(ScheduleRole.MEMBER).build());

            Settlement settlement = settlementRepository.save(Settlement.builder()
                    .schedule(schedule).receiver(leader)
                    .sum(0L).totalStatus(TotalStatus.HOLDING)
                    .build());
            settlementId = settlement.getSettlementId();

            userSettlementRepository.save(UserSettlement.builder()
                    .user(member).settlement(settlement)
                    .settlementStatus(SettlementStatus.HOLD_ACTIVE)
                    .build());

            return null;
        });
    }

    @AfterEach
    void cleanUp() {
        tx.execute(status -> {
            userSettlementRepository.deleteAllBySettlementId(settlementId);
            outboxRepository.deleteAll(outboxRepository.findAll().stream()
                    .filter(e -> e.getAggregateId() != null && e.getAggregateId().equals(settlementId))
                    .toList());
            settlementRepository.deleteById(settlementId);
            userScheduleRepository.deleteAll(userScheduleRepository.findAll().stream()
                    .filter(us -> us.getSchedule().getScheduleId().equals(scheduleId))
                    .toList());
            scheduleRepository.deleteById(scheduleId);
            userClubRepository.deleteAll(userClubRepository.findAll().stream()
                    .filter(uc -> uc.getClub().getClubId().equals(clubId))
                    .toList());
            clubRepository.deleteById(clubId);
            walletRepository.deleteById(leaderWalletId);
            walletRepository.deleteById(memberWalletId);
            userRepository.deleteById(leaderId);
            userRepository.deleteById(memberId);
            interestRepository.deleteById(interestId);
            return null;
        });
    }

    // ──────────────────────────────────────────────────────────────────
    // [P1-1] 리더 권한 체크
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("멤버가 정산 요청 시 MEMBER_CANNOT_CREATE_SETTLEMENT 예외")
    @Order(1)
    void 멤버가_정산_요청하면_예외() {
        User member = userRepository.findById(memberId).orElseThrow();
        Mockito.when(userService.getCurrentUser()).thenReturn(member);

        CustomException ex = assertThrows(CustomException.class,
                () -> settlementService.automaticSettlement(clubId, scheduleId));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEMBER_CANNOT_CREATE_SETTLEMENT);
    }

    // ──────────────────────────────────────────────────────────────────
    // [P0-1] automaticSettlement: Outbox 기록 + Settlement IN_PROGRESS
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("리더가 정산 요청 시 Settlement IN_PROGRESS, Outbox 이벤트 기록")
    @Order(2)
    void 리더_정산_요청_시_outbox_기록() {
        User leader = userRepository.findById(leaderId).orElseThrow();
        Mockito.when(userService.getCurrentUser()).thenReturn(leader);

        settlementService.automaticSettlement(clubId, scheduleId);

        tx.execute(status -> {
            Settlement s = settlementRepository.findById(settlementId).orElseThrow();
            assertThat(s.getTotalStatus()).isEqualTo(TotalStatus.IN_PROGRESS);

            List<OutboxEvent> events = outboxRepository.findAll().stream()
                    .filter(e -> "SettlementProcessEvent".equals(e.getEventType()))
                    .filter(e -> settlementId.equals(e.getAggregateId()))
                    .toList();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getStatus()).isEqualTo(OutboxStatus.NEW);
            return null;
        });
    }

    @Test
    @DisplayName("동시 요청 시 두 번째 요청은 ALREADY_SETTLING_SCHEDULE 예외")
    @Order(3)
    void 동시_요청_선점_실패() {
        User leader = userRepository.findById(leaderId).orElseThrow();
        Mockito.when(userService.getCurrentUser()).thenReturn(leader);

        settlementService.automaticSettlement(clubId, scheduleId);

        CustomException ex = assertThrows(CustomException.class,
                () -> settlementService.automaticSettlement(clubId, scheduleId));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ALREADY_SETTLING_SCHEDULE);
    }

    // ──────────────────────────────────────────────────────────────────
    // [P0-2] processParticipantSettlement: 성공 / 잔액 부족 관대 모드
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("잔액 충분한 참가자: true 반환, UserSettlement COMPLETED, 잔액 차감")
    @Order(4)
    void 참가자_정산_성공() {
        boolean result = userSettlementService.processParticipantSettlement(
                settlementId, leaderId, leaderWalletId, memberId, 10_000L);

        assertThat(result).isTrue();

        tx.execute(status -> {
            UserSettlement us = userSettlementRepository
                    .findBySettlement_SettlementIdAndUser_UserId(settlementId, memberId)
                    .orElseThrow();
            assertThat(us.getSettlementStatus()).isEqualTo(SettlementStatus.COMPLETED);

            Wallet w = walletRepository.findById(memberWalletId).orElseThrow();
            assertThat(w.getPostedBalance()).isEqualTo(490_000L); // 500000 - 10000
            return null;
        });
    }

    @Test
    @DisplayName("잔액 부족 참가자: false 반환, UserSettlement FAILED, 홀드 해제 (관대 모드)")
    @Order(5)
    void 참가자_잔액_부족_관대_모드() {
        // 잔액 부족 상태: pendingOut 유지, postedBalance를 5000으로 낮춤
        tx.execute(status -> {
            Wallet w = walletRepository.findById(memberWalletId).orElseThrow();
            w.updateBalance(5_000L); // captureHold 조건 불만족 (posted < amount)
            walletRepository.save(w);
            return null;
        });

        boolean result = userSettlementService.processParticipantSettlement(
                settlementId, leaderId, leaderWalletId, memberId, 10_000L);

        assertThat(result).isFalse(); // 예외 없이 false 반환

        tx.execute(status -> {
            UserSettlement us = userSettlementRepository
                    .findBySettlement_SettlementIdAndUser_UserId(settlementId, memberId)
                    .orElseThrow();
            assertThat(us.getSettlementStatus()).isEqualTo(SettlementStatus.FAILED);

            Wallet w = walletRepository.findById(memberWalletId).orElseThrow();
            assertThat(w.getPendingOut()).isEqualTo(0L); // 홀드 해제 확인
            return null;
        });
    }

    // ──────────────────────────────────────────────────────────────────
    // [P0-1] recoverSettlementToFailed: IN_PROGRESS → FAILED 복구
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("정산 실패 복구: IN_PROGRESS → FAILED 상태 변경")
    @Order(6)
    void 정산_실패_복구() {
        tx.execute(status -> {
            settlementRepository.markProcessing(settlementId); // → IN_PROGRESS
            return null;
        });

        userSettlementService.recoverSettlementToFailed(settlementId);

        tx.execute(status -> {
            Settlement s = settlementRepository.findById(settlementId).orElseThrow();
            assertThat(s.getTotalStatus()).isEqualTo(TotalStatus.FAILED);
            return null;
        });

        // FAILED → 재요청 가능 검증
        tx.execute(status -> {
            int rows = settlementRepository.markProcessing(settlementId);
            assertThat(rows).isEqualTo(1); // FAILED → IN_PROGRESS 재선점 성공
            // 원복
            settlementRepository.markFailed(settlementId);
            return null;
        });
    }
}
