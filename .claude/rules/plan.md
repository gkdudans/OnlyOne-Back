## Phase 0: 프로젝트 실행 ✅ 완료
- rules 문서 빈 부분 완성, 앱 기동 환경 수정
- Firebase soft-fail, Redis 비밀번호 제거, ES createIndex=false
- docker-compose: Kafka 이미지 교체(bitnami→apache), prometheus.yml 생성, ngrinder-net 네트워크 필요

## Phase 1: 현재 구현 코드에 대한 이해 ✅ 완료
- 정산 흐름 분석 완료 → docs/settlement-flow.md 참조

## Phase 2: 코드의 문제점, 개선 방향 파악 ✅ 완료
- Kafka + Redis Lua 조합 평가, 문제점 우선순위 분류
- 리팩토링 계획 → docs/refactoring-plan.md 참조

## Phase 3: 코드 리팩토링 ✅ 완료
- P0: Settlement FAILED 복구, 관대 모드(잔액 부족 graceful skip)
- P1: 리더 권한 체크, scope.join 타임아웃, Redis Lua Gate 제거
- P2: operationId 충돌 해결, DLQ 연결, 잔액 스냅샷, 중복 쿼리 제거
- 테스트: SettlementRefactoredTest 6개 케이스 전부 PASS
- 추가 버그 수정: markProcessing() 후 save()가 IN_PROGRESS 덮어쓰는 문제

## Phase 4: '결제' 기능에 대한 리팩토링 수행
- PaymentService 코드 분석 후 흐름 파악
- 문제점 파악 및 개선 방향 도출
- 리팩토링 수행

## Phase 5: 남은 테스트 정리 (선택)
- SettlementServiceTest: @Disabled 처리된 테스트들을 새 동작에 맞게 재작성
- 기존 테스트 컴파일 에러 완전 해소 후 전체 테스트 스위트 실행