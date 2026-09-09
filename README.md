# dreamUniverse-server

## 모듈 구조

하나의 Spring Boot 애플리케이션 안에서 기능별 패키지로 모듈을 구분합니다. `auth`는 Google 인증과 토큰/세션을, `user`는 회원 식별과 개인정보 저장을 담당합니다. `auth`는 `UserService`와 `UserProfile`을 통해 회원 모듈을 사용하며 회원 저장소·엔티티를 직접 참조하지 않습니다. Gradle 하위 프로젝트나 Spring Modulith로 경계를 강제하지는 않습니다.

```text
com.dreamuniverse_server
├── auth
│   ├── controller        # AuthController
│   ├── service           # AuthService, GoogleAuthService, AccessTokenService, RefreshTokenService
│   ├── dto/request       # GoogleLoginRequest, RefreshTokenRequest
│   ├── dto/response      # TokenResponse, UserInfo (제공자 공통 인증 결과를 내부에서 전달)
│   ├── entity            # RefreshSession, RefreshToken
│   ├── repository        # 로그인 세션/리프레시 토큰 저장소
│   ├── exception         # InvalidRefreshTokenException
│   └── config            # 인증 설정, 속성, JWT 검증기
└── user
    ├── service           # UserService: 회원 등록/조회 공개 API
    ├── dto               # UserProfile
    ├── entity            # User, OAuthProvider
    └── repository        # UserRepository
```

생성자 주입은 Lombok `@RequiredArgsConstructor`를 사용하며 `lombok.config`에서 `@Qualifier`를 생성자 매개변수로 복사합니다. JPA 엔티티는 Lombok의 protected 기본 생성자를 사용합니다.

## 실행 설정

Java 21과 MySQL이 필요합니다. 로컬 `application.yml`, `application-local.yml`은 Git에서 제외됩니다. 새 체크아웃에서는 `application.yml.example`을 `src/main/resources/application.yml`로 복사하세요. 기존 DB 설정이 있다면 유지합니다.

IntelliJ Run → Edit Configurations → Environment variables에 설정합니다.

| 환경변수 | 값 |
| --- | --- |
| `GOOGLE_CLIENT_ID` | Google Cloud의 **DreamUniverse Server (웹 애플리케이션)** 전체 클라이언트 ID |
| `JWT_SECRET` | 최소 32바이트의 랜덤 값을 Base64로 인코딩한 서비스 토큰 서명키 |
| `DB_USERNAME`, `DB_PASSWORD` | 예제 설정에서 사용하는 MySQL 계정 |
| `DB_URL` | 예제 설정의 선택값. 기본 `jdbc:mysql://localhost:3306/dreamuniverse` |

`openssl rand -base64 32`로 서명키를 생성하고 실행 환경변수에 저장합니다. 여러 서버 인스턴스에서는 같은 키와 DB를 사용합니다. `.env` 파일 자체를 Spring Boot가 자동으로 읽지는 않습니다.

```yaml
app:
  auth:
    google:
      client-id: ${GOOGLE_CLIENT_ID}
    token:
      secret: ${JWT_SECRET}
      ttl-seconds: 900
      refresh-ttl-seconds: 2592000
```

기본 액세스 토큰 유효기간은 **15분**, 리프레시 세션은 **로그인 시점부터 30일**입니다. 갱신해도 30일의 종료 시각은 연장하지 않습니다.

### DB 준비

`users`, `auth_refresh_sessions`, `auth_refresh_tokens` 테이블을 사용합니다. 현재 로컬 설정의 `ddl-auto: update`는 앱 시작 시 JPA 테이블을 생성합니다. 예제 파일처럼 `ddl-auto: none`을 사용하면 먼저 `docs/auth-schema.sql`을 해당 MySQL DB에 적용해야 합니다. 이 SQL은 자동 실행되지 않으며 기존 테이블 구조를 변경하지 않습니다.

회원·로그인 세션·리프레시 토큰의 PK는 `Long` + `@GeneratedValue(strategy = GenerationType.IDENTITY)`이며 DB에서는 `BIGINT AUTO_INCREMENT`를 사용합니다. 회원/세션을 참조하는 ID도 `Long`으로 통일합니다. 리프레시 토큰의 SHA-256 해시는 `NOT NULL` + `UNIQUE`로 유지해 인증 시 해시로 조회하고, JWT의 `jti`(개별 토큰 식별자)는 UUID를 유지합니다. 회원 등록의 네이티브 upsert는 ID 열을 생략하므로 MySQL이 ID를 생성합니다. 중복 로그인으로 AUTO_INCREMENT 번호에 빈 구간이 생길 수 있으며 정상입니다.

**기존 UUID 스키마가 있다면 새 코드를 실행하기 전에 마이그레이션해야 합니다.** `ddl-auto: update`로 UUID 문자열을 숫자로 직접 변환하면 안 됩니다. `docs/auth-schema.sql`은 새 DB용입니다.

기존 데이터용 `docs/migrate-auth-ids-to-bigint.sql`은 MySQL 8.0.16+의 기존 3개 테이블을 대상으로 합니다. 모든 앱 인스턴스와 쓰기 작업을 중지하고 DB 백업 후 실행합니다. 별도로 추가한 외래키·트리거·다른 테이블 참조가 있다면 먼저 이관 대상에 포함해야 합니다. 오류 시 다음 구문으로 계속 진행하지 않습니다.

스크립트는 숫자 ID를 가진 새 테이블에 회원과 세션을 복사하고 토큰의 session_id를 매핑합니다. 건수를 확인한 뒤 테이블 이름을 한 번에 교체합니다. 원본 테이블은 `_uuid_backup`으로, UUID 매핑은 새 테이블의 nullable `legacy_uuid` 열로 남깁니다. 반복 실행용 스크립트가 아니며 실제 DB에는 자동 적용하지 않습니다. 토큰 해시·소비 이력·세션 만료/폐기 상태를 유지하므로 기존 리프레시 토큰으로 새 숫자 ID의 액세스 토큰을 발급할 수 있습니다.

`/api/auth/me`의 `userId` 응답도 문자열 UUID에서 JSON 숫자로 바뀌므로 Android DTO를 `Long`으로 맞춥니다. JWT 표준에 맞춰 `sub` 자체는 문자열이고 서버에서 양수 Long 범위를 검증한 뒤 변환합니다.

### 기존 DB의 member → user 명칭 변경

패키지와 클래스는 `user`, `User`, `UserRepository`, `UserService`, `UserProfile`로 통일하고, 테이블은 `users`, 로그인 세션의 참조 열은 `user_id`를 사용합니다. `/api/auth/me`의 JSON 필드는 `userId`이며 Android DTO도 같은 이름으로 바꿉니다. 숫자 ID 값과 JWT의 `sub` 형식은 유지됩니다.

기존 DB에는 `docs/rename-member-to-user.sql`을 적용해야 합니다. 앱과 쓰기 작업을 중지하고 백업 후 실행하며, 테이블 이름 변경 전후에 앱을 실행하지 않습니다. `ddl-auto: update`는 테이블 이름 변경으로 인식하지 못해 빈 `users` 테이블을 만들 수 있으므로 마이그레이션을 먼저 적용합니다.

아직 UUID 스키마라면 **`migrate-auth-ids-to-bigint.sql` → `rename-member-to-user.sql` 순서**로 적용합니다. 숫자 ID 전환 스크립트의 `members`/`member_id`는 기존 스키마를 대상으로 하는 이력이라 그대로 유지했습니다. 이미 숫자 ID 스키마라면 명칭 변경 스크립트만 실행합니다. 새 DB라면 최신 `auth-schema.sql`만 사용합니다. SQL은 자동 실행하지 않습니다.

### 리프레시 토큰 PK 통일

기존 `token_hash` PK 테이블은 `docs/migrate-refresh-token-pk-to-identity.sql`로 `id BIGINT AUTO_INCREMENT` PK로 전환합니다. 기존 해시·세션 참조·소비 이력은 유지되므로 이 변경만으로 재로그인할 필요는 없습니다. 토큰은 계속 해시로 조회하며 새 숫자 ID를 인증 자격 증명으로 사용하지 않습니다. 고유 해시 인덱스가 별도로 필요하므로 전체 저장 공간이 반드시 줄어드는 변경은 아닙니다.

기존 DB의 전체 적용 순서는 필요한 단계만 골라 **`migrate-auth-ids-to-bigint.sql` → `rename-member-to-user.sql` → `migrate-refresh-token-pk-to-identity.sql`**입니다. 앞의 스크립트는 당시 스키마를 유지하며 마지막 단계에서 리프레시 토큰 PK를 바꿉니다. 모든 쓰기 작업 중지와 백업 후 적용하며 `ddl-auto: update`에 PK 전환을 맡기지 않습니다. 신규 DB는 최신 `auth-schema.sql`만 사용합니다.

회원 등록은 MySQL `INSERT ... ON DUPLICATE KEY UPDATE`와 `(provider, provider_user_id)` 조합의 유일성 제약을 사용하여 동시에 최초 로그인해도 같은 내부 회원 ID를 사용합니다. 이메일이 같다는 이유로 서로 다른 Google 계정을 합치지 않습니다. Google의 이름·이메일은 DB에 저장하고 로그인할 때 갱신하며 사진은 저장하지 않습니다.

## OAuth 제공자별 사용자 식별

`User`는 `provider`(`OAuthProvider.GOOGLE`, `OAuthProvider.KAKAO`)와 `providerUserId`로 외부 계정을 식별합니다. DB에서는 `(provider, provider_user_id)` 조합에만 고유 제약을 둡니다. 예를 들어 `GOOGLE/123`과 `KAKAO/123`은 서로 다른 사용자입니다. 동일 이메일로도 자동 병합하지 않으며 사용자당 로그인 제공자는 하나입니다.

제공자별 검증 서비스는 `UserInfo(provider, providerUserId, email, name)`이라는 공통 결과를 반환합니다. 현재 `GoogleAuthService`는 검증한 Google subject를 `providerUserId`로, 제공자를 `GOOGLE`로 설정합니다. `AuthService`는 이 결과의 제공자와 식별자를 그대로 `UserService.registerOAuthUser(...)`에 전달합니다. 제공자 값은 클라이언트 요청에서 받아 신뢰하지 않고 검증 서비스가 결정합니다. 카카오 로그인 API와 토큰 검증은 아직 구현하지 않았고, 이메일 필수 조건 등 제공자별 입력 정책은 카카오 연동 시 결정합니다.

기존 `users.google_subject` 스키마는 재실행 전에 `docs/migrate-users-oauth-provider.sql`을 적용해야 합니다. 이전에 필요한 ID/명칭/토큰 PK 마이그레이션을 먼저 적용한 후 이 스크립트를 마지막으로 실행합니다. 기존 계정은 `provider = GOOGLE`로 채우고 `google_subject`를 `provider_user_id`로 바꾸며 이전 단일 고유 인덱스를 제거합니다. 사용자 ID·리프레시 세션은 유지됩니다. 모든 쓰기 작업을 중지하고 백업 후 적용하며, 실제 DB에 자동 실행되지 않습니다. 새 DB는 최신 `auth-schema.sql`만 사용합니다. `ddl-auto: update`로 기존 열의 이름 변경과 값 이관을 대신하지 않습니다.

## Android 연동

1. Android Google 클라이언트의 패키지명과 서명 인증서 설정을 맞춥니다.
2. Credential Manager의 `setServerClientId(...)`에 **웹용 클라이언트 ID**를 사용해 Google ID 토큰을 받습니다.
3. ID 토큰을 `POST /api/auth/google`에 보내 서비스 액세스·리프레시 토큰을 받습니다.
4. 일반 API에는 `Authorization: Bearer <accessToken>`을 보냅니다.
5. 액세스 토큰 만료 시 `POST /api/auth/refresh`를 호출하고 **응답의 새 토큰 두 개를 함께 저장**한 뒤 원래 요청을 한 번 재시도합니다.
6. 로그아웃 API를 호출한 후 앱에서도 두 토큰을 삭제합니다.

Google 클라이언트 시크릿이나 백엔드 리다이렉트 URI는 필요하지 않습니다. 서비스 리프레시 토큰은 우리 서버가 발급하며 Google API 접근용 리프레시 토큰과 다릅니다. 배포 환경에서는 HTTPS를 사용합니다.

로그인·갱신·로그아웃에는 **Authorization 헤더를 보내지 않습니다**. 만료된 액세스 토큰을 붙이면 Spring Security가 먼저 `401`을 반환할 수 있습니다. Android 공통 인터셉터에서 이 세 경로를 제외하세요.

앱에서 동시에 여러 요청이 `401`을 받아도 **갱신은 한 번만 실행**하고 나머지는 그 결과를 기다려야 합니다. 이미 소비한 리프레시 토큰으로 재시도하면 세션이 폐기됩니다. 갱신 응답을 네트워크 문제로 잃어버린 경우에도 이전 토큰을 다시 사용하면 재로그인이 필요할 수 있습니다. 갱신의 `401`은 재로그인으로 처리하고 네트워크 오류/서버 `5xx`를 곧바로 로그아웃으로 취급하지 않습니다. 리프레시 토큰은 요청 로그에 남기지 않고 앱의 보호된 저장소에 보관합니다.

## API

### POST /api/auth/google

```json
{"idToken":"ANDROID_GOOGLE_ID_TOKEN"}
```

Google ID 토큰의 서명·발급자·대상 클라이언트·발급/만료 시간·subject·검증된 이메일을 확인한 뒤 회원을 등록/조회하고 토큰을 발급합니다. Google 액세스 토큰이나 authorization code를 보내는 API가 아닙니다.

성공 (`200`, `Cache-Control: no-store`):

```json
{
  "accessToken": "SERVICE_ACCESS_TOKEN",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshToken": "RANDOM_REFRESH_TOKEN",
  "refreshExpiresIn": 2592000
}
```

유효기간 단위는 초이며 `refreshExpiresIn`은 발급 시점의 세션 잔여 시간입니다. 이전 응답에 있던 `user`는 제거했습니다. 회원 정보가 필요하면 `/api/auth/me`를 호출합니다.

- `400`: Google ID 토큰 누락·공백·길이 초과 또는 잘못된 JSON
- `401`: Google 토큰 검증 실패 또는 검증된 이메일 누락
- `503`: Google 공개키 조회 등 검증 인프라의 일시적인 실패

### POST /api/auth/refresh

```json
{"refreshToken":"CURRENT_REFRESH_TOKEN"}
```

실제 리프레시 토큰은 43자리 Base64URL 문자열입니다. 위 문자열은 설명용 자리표시자입니다. 성공 시 로그인과 같은 형태의 **새 토큰 쌍**을 반환합니다. 기존 리프레시 토큰은 한 번만 사용할 수 있습니다.

- `400`: 잘못된 JSON 또는 토큰 형식
- `401`: 알 수 없는 토큰, 만료/폐기된 세션, 이미 소비한 토큰 재사용

### POST /api/auth/logout

```json
{"refreshToken":"CURRENT_REFRESH_TOKEN"}
```

`204 No Content`. 해당 로그인 세션 전체를 폐기합니다. 동일 세션의 이전 토큰으로도 폐기할 수 있으며 이미 폐기했거나 알 수 없는 토큰이어도 올바른 형식이면 `204`를 반환합니다. 다른 기기에서 별도로 로그인한 세션은 유지됩니다.

**이미 발급한 액세스 토큰은 만료까지 유효합니다.** 이 로그아웃은 리프레시 토큰을 통한 추가 발급을 중지하며 액세스 토큰 즉시 폐기는 구현하지 않습니다.

### GET /api/auth/me

`Authorization: Bearer <accessToken>`으로 호출합니다. 내부 회원 ID로 DB를 조회합니다.

```json
{
  "userId": 1,
  "email": "user@gmail.com",
  "name": "사용자 이름"
}
```

성공 `200`, 인증 실패/회원 부재 시 `401`. `name`은 없으면 null입니다.

## 토큰과 세션 동작

- 액세스 JWT에는 `sub`(서비스 내부 숫자 회원 ID를 문자열로 표현, 예: `"1"`), `iss`, `aud`, `iat`, `exp`, `jti`만 포함합니다. 이름·이메일·Google subject·사진은 넣지 않습니다.
- 기존 `google:{sub}` 또는 UUID 형식 액세스 토큰은 거부합니다. 숫자 ID 전환 후 기존 리프레시 토큰으로 갱신하거나 다시 로그인해야 합니다.
- 리프레시 토큰은 32바이트 난수로 생성하며 JWT가 아닙니다. DB에는 SHA-256 해시만 저장합니다.
- 갱신은 세션 행과 토큰 행을 동일한 순서로 잠그는 트랜잭션으로 처리합니다. 이전 토큰 소비와 새 토큰 저장은 함께 커밋됩니다.
- 소비한 토큰은 재사용 탐지를 위해 남깁니다. 재사용되면 해당 세션을 폐기하며, `401` 예외가 나더라도 폐기 변경사항은 커밋합니다.
- Google 공개키는 Spring Security/Nimbus로 조회·캐시합니다. JWT 시간 검증은 기본 60초 오차를 허용합니다.
- 만료된 세션과 토큰 이력의 정기 정리 작업은 아직 없습니다. 정리할 때는 세션 만료 후 해당 토큰 이력을 먼저 삭제하고 세션을 삭제합니다. 활성 세션의 소비된 토큰 이력은 재사용 탐지에 필요합니다.

## 확인 범위

MVP 요청에 따라 테스트 코드는 추가하거나 실행하지 않습니다. 컴파일을 확인했으며 실제 Google/Android 로그인과 MySQL의 회원 등록·갱신·재사용 탐지 동작은 별도 연동 확인이 필요합니다.

참고: [Google 서버 인증](https://developers.google.com/identity/sign-in/android/backend-auth), [OAuth 리프레시 토큰 보안 권고](https://www.rfc-editor.org/rfc/rfc9700.html#section-4.14).
