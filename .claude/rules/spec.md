## 기술 스택
- Java 21, Spring Boot 3.5.4
- Database: MySQL via Spring Data JPA
- Cache: Redis
- Infra: Docker Compose
- Messaging: Kafka (변경 가능)
- Auth: Spring Security + OAuth2 Client
- Utilities: Lombok, Bean Validation
- 개발 환경: `localhost:8080`

## 주의 사항
- javax 패키지 대신 jakarta 패키지를 사용한다.
- WebSecurityConfigurerAdapter는 사용하지 않는다 (deprecated).
- QueryDSL은 JPAQueryFactory를 빈으로 등록해서 사용한다.