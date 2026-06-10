# OnlyOne-Back

시니어를 위한 모임 플랫폼 백엔드. Java 21 / Spring Boot 3.5 / MySQL / Redis / Kafka.
현재 집중 영역: **정산(Settlement)** 기능 리팩토링 (Phase 0~4).

## 테스트
- 기능 추가/수정 시 반드시 검증 테스트를 함께 작성
- 도메인 엔티티 테스트는 순수 단위 테스트 (JPA, Spring Context 의존 금지)
- Service 테스트는 `@SpringBootTest` + `@Transactional` 통합 테스트

## 참고 문서
- spec.md: 기술 스택 명세 
- plan.md: 개발 계획
- code-style.md: 패키지 구조, 코딩 스타일, 네이밍 규칙, 예외 처리, API 응답 형식
- testing.md: 테스트 형식
- domain.md: 도메인 명세
- docs/settlement-flow.md: 정산 기능 흐름 분석 (Phase 1) — 컴포넌트 역할, 동시성 레이어, 발견된 문제점
- docs/refactoring-plan.md: 리팩토링 계획 (Phase 3) — 우선순위별 문제점 목록 및 수정 방향

## Commands
### Build
./gradlew build
### Run application
./gradlew bootRun
### Run all tests
./gradlew test
### Clean build
./gradlew clean build
### 컴파일만 빠르게 확인
./gradlew compileJava compileTestJava
