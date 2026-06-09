## 패키지 구조: 도메인 기반 패키지 구조
- 'domain' 패키지 안에 엔티티별로 디렉토리 분리
- 'domain' 패키지 중 'settlement' 엔티티 및 관련 엔티티에만 집중
- 패키지 구조
    - `controller`: REST Controller
    - `dto`: DTO 클래스
    - `entity`: 엔티티 클래스
    - `repository`: JPA Repository
    - `service`: Service 구현 클래스

### Service
- userServicervice 제외 다른 서비스 참조 금지
- 참조 시에는 Service가 아닌 Repository의 메서드 참조
- Mock 테스트 사용 금지, `@SpringBootTest` 통합 테스트로 작성

## 네이밍
- 서비스 메서드: 동사 + 목적어 (e.g., createOrder, findUserById)
- Boolean 반환 메서드: is/has/can 접두사 사용
- 컬렉션 반환: 복수형 사용 (e.g., findActiveUsers)


## 예외 처리
- 비즈니스 규칙 위반은 `throw new CustomException(ErrorCode.XXX)` 로 처리
- `ErrorCode` enum에 HTTP 상태 코드(`status`), 코드 문자열(`code`), 메시지(`message`)를 함께 정의
- `GlobalExceptionHandler`(`@RestControllerAdvice`)가 모든 예외를 중앙 처리
  - `CustomException` → ErrorCode 기반 상태 코드 + `ErrorResponse` 반환
  - `MethodArgumentNotValidException`, `BindException` → 400 + 필드별 `validation` 맵 포함
  - `Exception` → 500 INTERNAL_SERVER_ERROR
- `ErrorCode` 코드 문자열 형식: `{DOMAIN}_{STATUS}_{SEQ}` (예: `SETTLEMENT_403_1`)

## API 응답 형식
모든 API는 `CommonResponse<T>`로 감싸서 반환한다.

```json
// 성공
{ "success": true, "data": { ... } }

// 실패
{ "success": false, "data": { "code": "SETTLEMENT_403_1", "message": "...", "validation": { "field": "msg" } } }
```

- `CommonResponse.success(data)` / `CommonResponse.error(errorResponse)` 팩토리 메서드 사용
- 에러 시 `data` 필드는 `ErrorResponse` 타입: `code`, `message`, `validation`(선택)
- `validation`은 `@Valid` 검증 실패 시에만 포함됨
