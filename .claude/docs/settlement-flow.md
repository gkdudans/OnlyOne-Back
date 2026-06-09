# 정산 기능 흐름 분석 (Phase 1)

## 전체 흐름

```
[HTTP 요청]
    │
    ▼
① SettlementController.createSettlement()
    │  POST /clubs/{clubId}/schedules/{scheduleId}/settlements
    │
    ▼
② SettlementService.automaticSettlement()
    │  - 클럽/스케줄/정산 존재 확인
    │  - 스케줄 종료 여부 확인 (ENDED or 시작시간 경과)
    │  - markProcessing(): UPDATE total_status='IN_PROGRESS'
    │    WHERE total_status IN ('HOLDING','FAILED')  ← DB 레벨 선점 (동시성 제어)
    │  - cost=0 또는 참가자=0이면 → 스케줄 CLOSED 후 종료
    │  - OutboxAppender.append("SettlementProcessEvent") → outbox 테이블 저장
    │
    ▼
③ OutboxRelayConfig (200ms 폴링, @Scheduled)
    │  - SELECT FOR UPDATE SKIP LOCKED 으로 NEW 이벤트 배치 픽업
    │  - kafkaTemplate.executeInTransaction() 으로 settlement.process.v1 토픽 발행
    │  - 발행 후 status → PUBLISHED
    │
    ▼
④ SettlementKafkaEventListener.onSettlementProcess()
    │  - settlement.process.v1 구독 (concurrency=3, batch)
    │  - SettlementProcessEvent 로 파싱
    │  - processSettlementWithStructuredScope()
    │    └─ 참가자별 가상 스레드 fork (Java 21 StructuredTaskScope)
    │       └─ Semaphore(32) 로 동시 실행 수 백프레셔 제어
    │
    ▼
⑤ UserSettlementService.processParticipantSettlement() [REQUIRES_NEW 독립 트랜잭션]
    │  - Redis Lua Gate: withWalletGate(participantId, "capture")  ← 분산 락
    │  - 이미 COMPLETED 면 멱등 스킵
    │  - walletRepository.captureHold()  ← DB UPDATE 로 홀드 잔액 차감
    │  - 성공: UserSettlement.COMPLETED + OutboxAppender("ParticipantSettlementResult", SUCCESS)
    │  - 실패: UserSettlement.FAILED  + FailedEventAppender("ParticipantSettlementResult", FAILED)
    │
    ▼
⑥ SettlementKafkaEventListener.completeSettlement()
    │  - 전원 성공 시에만 실행
    │  - creditToLeader(): Redis Lua Gate 로 리더 지갑 가산
    │  - Schedule → CLOSED
    │  - Settlement → COMPLETED
    │
    ▼
⑦ OutboxRelayConfig (재발행)
    │  - ParticipantSettlementResult 이벤트를 user-settlement.result.v1 토픽 발행
    │
    ▼
⑧ KafkaService → LedgerWriter.writeBatch()
    - user-settlement.result.v1 구독
    - operationId 기반 멱등 체크 (이미 존재하면 스킵)
    - WalletTransaction (OUTGOING/INCOMING) + Transfer 배치 저장 (거래 기록)
```

---

## 핵심 컴포넌트 역할

| 컴포넌트 | 역할 |
|---|---|
| `SettlementService.automaticSettlement()` | 진입점. 유효성 검사 + DB 선점 + Outbox 기록 |
| `OutboxAppender` | 이벤트를 outbox 테이블에 트랜잭션 내 저장 |
| `OutboxRelayConfig` | 200ms 폴링으로 outbox → Kafka 릴레이 |
| `SettlementKafkaEventListener` | Kafka 컨슈머. 가상 스레드로 참가자 병렬 처리 |
| `UserSettlementService.processParticipantSettlement()` | 참가자별 독립 트랜잭션. Redis 분산 락 + 홀드 차감 |
| `FailedEventAppender` | 실패 이벤트를 REQUIRES_NEW 트랜잭션으로 outbox에 저장 |
| `LedgerWriter.writeBatch()` | 거래 결과를 WalletTransaction + Transfer 로 기록 |

---

## 동시성 제어 레이어

| 레이어 | 방식 | 목적 |
|---|---|---|
| DB 선점 | `markProcessing()` UPDATE WHERE status IN ('HOLDING','FAILED') | 정산 중복 실행 방지 |
| Redis Lua Gate | `withWalletGate(userId, "capture")` | 참가자 지갑 동시 접근 방지 |
| Semaphore | `Semaphore(32)` | Kafka 컨슈머 내 가상 스레드 수 제한 (백프레셔) |
| 멱등 처리 | `operationId` unique 체크 | LedgerWriter 중복 기록 방지 |

---

## Kafka 토픽 구조

| 토픽 | 발행자 | 구독자 | 파티션 |
|---|---|---|---|
| `settlement.process.v1` | OutboxRelayConfig | SettlementKafkaEventListener | 6 |
| `user-settlement.result.v1` | OutboxRelayConfig | KafkaService (→ LedgerWriter) | 32 |

---

## 발견된 문제점

### ① 리더 권한 체크 누락
`automaticSettlement()`에 현재 사용자가 리더인지 확인하는 코드가 없다.
테스트(`리더가_아닌_멤버가_정산_요청하면_예외가_발생한다`)는 `MEMBER_CANNOT_CREATE_SETTLEMENT`를 기대하지만 현재 코드에서 해당 에러는 발생하지 않는다.

### ② 정산 실패 시 Settlement 상태가 IN_PROGRESS에서 멈춤
`SettlementKafkaEventListener.processSettlementWithStructuredScope()`에서 예외 발생 시 Settlement 상태를 `FAILED`로 되돌리는 코드가 없다.
→ `markProcessing()` 재시도 조건(`HOLDING or FAILED`)을 만족 못 해 이후 재시도 불가.

### ③ 부분 성공 상태에서 롤백 불가
`processParticipantWithRetry()` 3회 실패 시 이미 성공한 참가자들의 홀드 차감은 `REQUIRES_NEW`로 이미 커밋된 상태.
→ 일부만 차감된 채로 정산이 실패할 수 있다.

### ④ LedgerWriter 잔액 스냅샷 타이밍 불일치
`balance: memberWallet.getPostedBalance()`가 실제 차감 이후 시점이 아닌 조회 시점 잔액을 기록한다.
→ 거래 기록의 잔액 정합성이 깨질 수 있다.

### ⑤ 데드 코드 (처리 완료)
- `SettlementProcessEventListener`: Spring 이벤트 기반 정산 처리. `SettlementKafkaEventListener`(Kafka 방식)로 대체됨. `@Component` 주석 처리 완료.
- `SettlementService` 미사용 필드: `userScheduleRepository`, `notificationService`, `walletService`, `eventPublisher` 주석 처리 완료.
