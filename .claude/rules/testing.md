## 테스트 프레임워크
- 단위 테스트: JUnit 5 + `@DataJpaTest` + AssertJ + Mockito (`@MockitoBean`, `@MockitoSpyBean`)
- 통합 테스트: JUnit 5 + `@SpringBootTest` + `@Transactional` + AssertJ + Mockito

## 테스트 구조
- Given-When-Then 패턴을 따른다.
- @DisplayName에 한글로 테스트 의도를 명시한다.
- 메서드명은 영문 snake_case로 작성한다.

## 테스트 전제 조건
- 모든 테스트는 DB(MySQL), Redis가 실행 중인 환경에서 실행하며, 인메모리 DB나 Mock으로 대체하지 않는다.