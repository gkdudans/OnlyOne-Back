# Domain

## 서비스 개요
- 시니어를 위한 모임 플랫폼
- 사용자(User)는 모임(Club)에 가입해 정기모임(Schedule)을 통해 활돋하고, 정기모임에 대한 비용을 정산(Settlement)할 수 있다.
- 사용자에게는 지갑(Wallet)에 대한 포인트를 충전(Payment)할 수 있고, 이 포인트를 정산에 사용한다.
- 사용자가 정산하고 충전한 지갑의 거래 내용은 Wallet Transaction으로 기록된다.
---

## 도메인 용어

### USER
- 회원. 카카오 소셜 로그인으로 가입.

### CLUB (모임)
- 모임장: 모임의 생성자이자 관리자. 모임 수정/삭제에 대한 권한 보유.
- USER_CLUB: 회원-모임 관계 및 역할 정의.

### SCHEDULE (정기모임)
- 정기모임은 모임에 소속되는 관계
- 스케줄 관리(생성, 수정, 삭제)는 모임장만 권한을 가질 수 있음.
- 스케줄은 모집 중 → 진행 중 → 정산 중 → 종료로 상태 변화
- 참가비(cost): 스케줄 참여 시 부담해야 할 비용
- USER_SCHEDULE: 회원-스케줄 관계 및 역할 정의.

### SETTLEMENT (정산)
- 스케줄은 정산과 1:1 관계. 스케줄과 하나의 정산이 연결되는 구조.
- 정산은 정산 요청 → 진행 중 → 완료로 상태 변화.
- USER_SETTLEMENT: 개별 회원의 정산 처리 상태.

### WALLET (지갑)
- 유저와 1:1 관계.
- WALLET_TRANSACTION: 지갑 거래 내역.
    - TRANSFER: 타인 지갑으로 송금
    - CHARGE: 내 지갑에 포인트 충전

### PAYMENT (충전)
- 토스페이먼츠를 통한 포인트 충전.
- `payment_id`는 UUID. `toss_payment_key` unique. `WalletTransaction`을 FK로 참조하며, 항상 `CHARGE` 타입 거래에만 연결됨.