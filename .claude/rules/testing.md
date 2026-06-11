## 테스트 프레임워크
- 단위 테스트: JUnit 5 + `@DataJpaTest` + AssertJ + Mockito (`@MockitoBean`, `@MockitoSpyBean`)
- 통합 테스트: JUnit 5 + `@SpringBootTest` + `@Transactional` + AssertJ + Mockito

## 테스트 구조
- Given-When-Then 패턴을 따른다.
- @DisplayName에 한글로 테스트 의도를 명시한다.
- 메서드명은 영문 snake_case로 작성한다.

## 테스트 전제 조건
- 모든 테스트는 DB(MySQL), Redis가 실행 중인 환경에서 실행하며, 인메모리 DB나 Mock으로 대체하지 않는다.

## 테스트 어노테이션 선택 기준

| 상황 | 어노테이션 |
|---|---|
| JPA 레포지토리만 검증 | `@DataJpaTest` + `@Import(서비스클래스)` |
| 전체 흐름 통합 검증 | `@SpringBootTest(properties="spring.data.redis.repositories.enabled=false")` + `@Import(TestConfig.class)` |
| 컨트롤러 레이어만 검증 | `@WebMvcTest` |

## @SpringBootTest 사용 시 필수 설정
```java
@ActiveProfiles("test")
@SpringBootTest(properties = "spring.data.redis.repositories.enabled=false")
@Import(TestConfig.class)  // Firebase, Redis, JWT mock 제공
```
- `TestConfig`는 `@Import`로 명시해야 자동 적용됨 (`@TestConfiguration`은 자동 스캔 안 됨)
- `spring.data.redis.repositories.enabled=false` 없으면 `redisKeyValueAdapter` 초기화 실패

## REQUIRES_NEW 트랜잭션 테스트 패턴
`@Transactional(REQUIRES_NEW)` 메서드는 테스트 `@Transactional` 롤백으로 정리되지 않음.
테스트 클래스에 `@Transactional` 없이, `TransactionTemplate`으로 직접 제어한다.

```java
// @BeforeEach: 데이터 생성 (commit)
tx.execute(status -> { ... return null; });

// @AfterEach: 데이터 정리 (commit)
tx.execute(status -> { repository.deleteById(id); return null; });

// 검증은 별도 tx로
tx.execute(status -> {
    assertThat(repo.findById(id).orElseThrow().getStatus()).isEqualTo(EXPECTED);
    return null;
});
```

## JPA native UPDATE + save() 충돌 주의
`@Modifying(clearAutomatically = true)` 쿼리 실행 후 같은 엔티티를 `save()` 하면
detached 상태의 **구버전 필드값이 DB를 덮어씀**.

```java
// 잘못된 패턴
repository.markProcessing(id);   // native UPDATE → IN_PROGRESS
entity.updateSum(total);
repository.save(entity);         // ← HOLDING으로 되돌림! (detached 상태 merge)

// 올바른 패턴
repository.markProcessing(id);
entity.updateTotalStatus(TotalStatus.IN_PROGRESS); // 인메모리 상태 동기화
entity.updateSum(total);
repository.save(entity);         // IN_PROGRESS 유지
```