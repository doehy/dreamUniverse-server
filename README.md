# dreamUniverse-server

## 모듈 구조

하나의 Spring Boot 애플리케이션 안에서 기능별 패키지로 모듈을 구분합니다. `auth`는 Google·카카오 인증과 토큰/세션을, `user`는 회원 식별과 개인정보 저장을 담당합니다. `auth`는 `UserService`와 `UserProfile`을 통해 회원 모듈을 사용하며 회원 저장소·엔티티를 직접 참조하지 않습니다. Gradle 하위 프로젝트나 Spring Modulith로 경계를 강제하지는 않습니다.

```text
com.dreamuniverse_server
├── auth
│   ├── controller        # AuthController (로그인 분기·가입·갱신·로그아웃)
│   ├── service           # AuthService, GoogleAuthService, KakaoAuthService, SignupService, AccessTokenService, RefreshTokenService
│   ├── dto/request       # GoogleLoginRequest, KakaoLoginRequest, SignupRequest, RefreshTokenRequest
│   ├── dto/response      # LoginResponse, TokenResponse, UserInfo (제공자 공통 인증 결과를 내부에서 전달)
│   ├── entity            # RefreshSession, RefreshToken, SignupSession
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
| `KAKAO_APP_ID` | 카카오디벨로퍼스의 숫자 앱 ID. Android SDK에서 사용하는 카카오 앱과 같아야 함 |
| `JWT_SECRET` | 최소 32바이트의 랜덤 값을 Base64로 인코딩한 서비스 토큰 서명키 |
| `DB_USERNAME`, `DB_PASSWORD` | 예제 설정에서 사용하는 MySQL 계정 |
| `DB_URL` | 예제 설정의 선택값. 기본 `jdbc:mysql://localhost:3306/dreamuniverse` |

`openssl rand -base64 32`로 서명키를 생성하고 실행 환경변수에 저장합니다. 여러 서버 인스턴스에서는 같은 키와 DB를 사용합니다. `.env` 파일 자체를 Spring Boot가 자동으로 읽지는 않습니다.

```yaml
app:
  auth:
    google:
      client-id: ${GOOGLE_CLIENT_ID}
    kakao:
      app-id: ${KAKAO_APP_ID:}
    token:
      secret: ${JWT_SECRET}
      ttl-seconds: 900
      refresh-ttl-seconds: 2592000
```

기본 액세스 토큰 유효기간은 **15분**, 리프레시 세션은 **로그인 시점부터 30일**입니다. 갱신해도 30일의 종료 시각은 연장하지 않습니다.

### DB 준비

`users`, `auth_refresh_sessions`, `auth_refresh_tokens`, `auth_signup_sessions` 테이블을 사용합니다. 현재 로컬 설정의 `ddl-auto: update`는 앱 시작 시 JPA 테이블을 생성합니다. 예제 파일처럼 `ddl-auto: none`을 사용하면 먼저 `docs/auth-schema.sql`을 해당 MySQL DB에 적용해야 합니다. 이 SQL은 자동 실행되지 않으며 기존 테이블 구조를 변경하지 않습니다.

회원·로그인 세션·리프레시 토큰의 PK는 `Long` + `@GeneratedValue(strategy = GenerationType.IDENTITY)`이며 DB에서는 `BIGINT AUTO_INCREMENT`를 사용합니다. 회원/세션을 참조하는 ID도 `Long`으로 통일합니다. 리프레시 토큰의 SHA-256 해시는 `NOT NULL` + `UNIQUE`로 유지해 인증 시 해시로 조회하고, JWT의 `jti`(개별 토큰 식별자)는 UUID를 유지합니다. 회원은 가입 완료 API에서 JPA save로 생성하며 MySQL이 ID를 생성합니다. Google 인증만으로 회원을 생성하지 않습니다.

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

사용자는 `(provider, provider_user_id)`로 구분하고 이메일만으로 계정을 합치지 않습니다. 제공자 표시 이름·이메일은 가입 세션에서 보관하다가 가입 완료 시 회원 DB에 저장합니다. 일반 로그인에서는 닉네임과 회원 정보를 덮어쓰지 않습니다. 사진은 저장하지 않습니다.

## OAuth 제공자별 사용자 식별

`User`는 `provider`(`OAuthProvider.GOOGLE`, `OAuthProvider.KAKAO`)와 `providerUserId`로 외부 계정을 식별합니다. DB에서는 `(provider, provider_user_id)` 조합에만 고유 제약을 둡니다. 예를 들어 `GOOGLE/123`과 `KAKAO/123`은 서로 다른 사용자입니다. 동일 이메일로도 자동 병합하지 않으며 사용자당 로그인 제공자는 하나입니다.

제공자별 검증 서비스는 `UserInfo(provider, providerUserId, email, name)`이라는 공통 결과를 반환합니다. 현재 `GoogleAuthService`는 검증한 Google subject를 `providerUserId`로, 제공자를 `GOOGLE`로 설정합니다. `AuthService`는 이 검증 결과를 `SignupService`에 전달하여 가입 완료 사용자 여부를 확인합니다. 제공자 값은 클라이언트 요청에서 받아 신뢰하지 않고 검증 서비스가 결정합니다. 카카오는 `KakaoAuthService`에서 앱 ID·토큰 유효성·사용자 ID·이메일 유효성 및 인증 여부를 확인한 후 `KAKAO`와 카카오 회원번호를 전달합니다. 가입 필수값인 이메일을 받지 못하면 로그인 단계에서 403을 반환합니다.

기존 `users.google_subject` 스키마는 재실행 전에 `docs/migrate-users-oauth-provider.sql`을 적용해야 합니다. 이전에 필요한 ID/명칭/토큰 PK 마이그레이션을 먼저 적용한 후 이 스크립트를 마지막으로 실행합니다. 기존 계정은 `provider = GOOGLE`로 채우고 `google_subject`를 `provider_user_id`로 바꾸며 이전 단일 고유 인덱스를 제거합니다. 사용자 ID·리프레시 세션은 유지됩니다. 모든 쓰기 작업을 중지하고 백업 후 적용하며, 실제 DB에 자동 실행되지 않습니다. 새 DB는 최신 `auth-schema.sql`만 사용합니다. `ddl-auto: update`로 기존 열의 이름 변경과 값 이관을 대신하지 않습니다.

## Android 연동

1. Android Google 클라이언트의 패키지명과 서명 인증서 설정을 맞춥니다.
2. Credential Manager의 `setServerClientId(...)`에 **웹용 클라이언트 ID**를 사용해 Google ID 토큰을 받습니다.
3. ID 토큰을 `POST /api/auth/google`에 보냅니다. `LOGIN_SUCCESS`이면 `tokens`를 저장하고, `SIGNUP_REQUIRED`이면 가입 화면에서 필수 정보를 받아 `POST /api/auth/signup`을 호출합니다.
4. 일반 API에는 `Authorization: Bearer <accessToken>`을 보냅니다.
5. 액세스 토큰 만료 시 `POST /api/auth/refresh`를 호출하고 **응답의 새 토큰 두 개를 함께 저장**한 뒤 원래 요청을 한 번 재시도합니다.
6. 로그아웃 API를 호출한 후 앱에서도 두 토큰을 삭제합니다.

Google 클라이언트 시크릿이나 백엔드 리다이렉트 URI는 필요하지 않습니다. 서비스 리프레시 토큰은 우리 서버가 발급하며 Google API 접근용 리프레시 토큰과 다릅니다. 배포 환경에서는 HTTPS를 사용합니다.

로그인·가입 완료·갱신·로그아웃에는 **Authorization 헤더를 보내지 않습니다**. 만료된 액세스 토큰을 붙이면 Spring Security가 먼저 `401`을 반환할 수 있습니다. Android 공통 인터셉터에서 `/api/auth/google`, `/api/auth/kakao`, `/api/auth/signup`, `/api/auth/refresh`, `/api/auth/logout`을 제외하세요.

앱에서 동시에 여러 요청이 `401`을 받아도 **갱신은 한 번만 실행**하고 나머지는 그 결과를 기다려야 합니다. 이미 소비한 리프레시 토큰으로 재시도하면 세션이 폐기됩니다. 갱신 응답을 네트워크 문제로 잃어버린 경우에도 이전 토큰을 다시 사용하면 재로그인이 필요할 수 있습니다. 갱신의 `401`은 재로그인으로 처리하고 네트워크 오류/서버 `5xx`를 곧바로 로그아웃으로 취급하지 않습니다. 리프레시 토큰은 요청 로그에 남기지 않고 앱의 보호된 저장소에 보관합니다.

## API

### POST /api/auth/google

```json
{"idToken":"ANDROID_GOOGLE_ID_TOKEN"}
```

Google 검증 성공 후 `200`으로 기존 사용자/가입 필요 여부를 반환합니다. 응답은 `Cache-Control: no-store`입니다.

가입을 완료한 기존 사용자:

```json
{
  "status": "LOGIN_SUCCESS",
  "tokens": {
    "accessToken": "SERVICE_ACCESS_TOKEN",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "refreshToken": "RANDOM_REFRESH_TOKEN",
    "refreshExpiresIn": 2592000
  },
  "signup": null
}
```

신규 사용자 또는 이전 자동 가입 계정 중 필수 정보 미입력 사용자:

```json
{
  "status": "SIGNUP_REQUIRED",
  "tokens": null,
  "signup": {
    "signupToken": "ONE_TIME_SIGNUP_TOKEN",
    "expiresIn": 600,
    "email": "user@gmail.com",
    "name": "Google 이름"
  }
}
```

이 단계에서는 신규 `users` 행과 서비스 토큰을 만들지 않습니다. 가입용 토큰은 32바이트 난수의 43자리 Base64URL 문자열이며 DB에는 해시만 저장합니다. 일반 API/리프레시 API에 사용할 수 없습니다. 동일 계정으로 소셜 인증을 다시 받으면 이전 가입용 토큰은 대체되어 사용할 수 없습니다.

`400`: 잘못된 요청, `401`: Google 인증 실패, `503`: 검증 인프라 일시 오류.

### POST /api/auth/kakao

Android 카카오 SDK 로그인 성공으로 받은 **카카오 액세스 토큰**을 보냅니다. Google ID 토큰, 카카오 ID 토큰, 인가 코드, 서비스 액세스 토큰을 보내는 API가 아닙니다. Authorization 헤더는 붙이지 않습니다.

```json
{"accessToken":"KAKAO_ACCESS_TOKEN"}
```

서버는 고정된 카카오 HTTPS API에만 요청합니다.

1. `/v1/user/access_token_info`에서 유효기간과 숫자 `app_id`를 확인합니다. `KAKAO_APP_ID`와 다르면 거부합니다.
2. `/v2/user/me`에서 회원번호가 앞의 조회와 같은지 확인하고 이메일·닉네임 정보만 요청합니다. 프로필 사진 필드는 저장하지 않습니다.
3. `is_email_valid`, `is_email_verified`가 모두 true이고 이메일이 있어야 공통 가입/로그인 흐름으로 진행합니다.
4. 기존 가입 완료 사용자는 `LOGIN_SUCCESS`, 신규 또는 가입 미완료 사용자는 `SIGNUP_REQUIRED`를 반환합니다. 응답 JSON 구조와 가입 완료 API는 Google과 동일합니다.

| HTTP | 의미 | Android 처리 |
| --- | --- | --- |
| 200 | 로그인 성공 또는 가입 필요 | `status`로 분기 |
| 400 | 빈 토큰·길이 초과·잘못된 형식/JSON | 요청 확인 |
| 401 | 카카오 토큰 만료/무효, 다른 앱의 토큰, 사용자 ID 불일치 | 카카오 인증 다시 진행 |
| 403 | 이메일 미제공·미인증·유효하지 않음 | 이메일 동의/계정 인증 확인 후 다시 진행 |
| 503 | 앱 ID 미설정, 카카오 통신·응답·요청 제한 등 검증 인프라 오류 | 설정 확인 또는 잠시 후 재시도 |

각 카카오 요청의 연결 제한은 3초, 읽기 제한은 5초입니다. 카카오 액세스 토큰은 서버 DB에 저장하지 않습니다. 우리 서버의 리프레시 토큰과 카카오 SDK의 토큰은 별개입니다. `/logout`은 서비스 세션만 폐기하며 카카오 계정 로그아웃/연결 해제는 수행하지 않습니다.

#### 카카오 콘솔·Android 준비

- 카카오 로그인을 활성화하고 Android 패키지명·키 해시를 등록합니다.
- Android SDK에는 해당 앱의 네이티브 앱 키를 사용하고, 서버 `KAKAO_APP_ID`에는 **같은 앱의 숫자 ID**를 설정합니다. REST API 키나 네이티브 앱 키 문자열을 앱 ID에 넣지 않습니다.
- 카카오계정 이메일(`account_email`) 동의항목을 사용하도록 설정합니다. 앱에서 받을 수 있는 이메일 권한은 콘솔에서 확인해야 합니다. 이메일 제공을 받을 수 없다면 현재 필수 이메일 가입 정책으로 진행할 수 없습니다.
- 닉네임(`profile_nickname`)은 선택 사항입니다. 카카오 닉네임과 무관하게 가입 화면에서 서비스 닉네임을 별도로 받습니다. 프로필 사진 동의는 필요하지 않습니다.
- SDK 로그인 후 받은 `accessToken`을 위 API에 전달합니다. 이메일 추가 동의가 가능하면 Android에서 추가 동의 후 새 토큰으로 다시 요청합니다. 이메일 미인증은 동의만으로 해결되지 않으므로 카카오계정 이메일 상태를 확인해야 합니다.
- 이 서버 API는 Android SDK 토큰 검증 방식이므로 서버용 인가 코드 콜백, REST API 키, 클라이언트 시크릿을 사용하지 않습니다. SDK에 필요한 앱 복귀 설정은 Android에서 적용합니다.

`KAKAO_APP_ID`가 없어도 기존 Google 로그인은 사용할 수 있고, 카카오 API만 503을 반환합니다. 기존 `(provider, provider_user_id)` 스키마의 `KAKAO` 값을 사용하므로 이번 변경의 추가 DB 마이그레이션은 없습니다.

카카오 관련 필드/검증 기준: [카카오 로그인 REST API 공식 문서](https://developers.kakao.com/docs/ko/kakaologin/rest-api).

### POST /api/auth/signup

```json
{
  "signupToken": "ONE_TIME_SIGNUP_TOKEN",
  "nickname": "별을찾는사람",
  "birthDate": "2000-05-21",
  "termsOfServiceAgreed": true,
  "privacyPolicyAgreed": true
}
```

이메일은 Google 또는 카카오에서 검증한 `signup.email`을 화면에 보여주고 서버의 가입 세션 값으로 저장합니다. 클라이언트에서 이메일·provider·providerUserId를 입력받아 덮어쓰지 않습니다. 따라서 가입 요청에 이메일 필드는 없습니다. `name`은 제공자 표시 이름이며 서비스 닉네임과 별도로 저장합니다. 카카오에서는 동의받아 제공된 프로필 닉네임을 표시 이름으로 사용하고, 없으면 null입니다.

닉네임은 앞뒤 공백 제거 후 1~30자이며 현재 중복은 허용합니다. 생년월일은 필수 `YYYY-MM-DD` 날짜이며 미래 날짜를 거부합니다. 별도의 최소 연령 제한은 정하지 않았습니다. 서비스 이용약관과 개인정보 처리방침 동의는 둘 다 필수 Boolean이며 누락/null/false를 거부합니다. 동의 시각 두 개와 가입 완료 시각을 서버 시간으로 저장합니다. 약관 본문·버전은 아직 제공되지 않아 버전별 동의 이력은 포함하지 않았습니다.

성공 시 `201`로 아래 토큰 쌍을 직접 반환합니다 (`LOGIN_SUCCESS`의 `tokens` 내용과 동일).

```json
{
  "accessToken": "SERVICE_ACCESS_TOKEN",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshToken": "RANDOM_REFRESH_TOKEN",
  "refreshExpiresIn": 2592000
}
```

- `400`: 필수 정보 누락, 닉네임 길이 초과/공백, 잘못된 날짜/미래 날짜, 동의하지 않은 항목
- `401`: 잘못된·만료된·이미 소비한 가입용 토큰
- `409`: 이미 가입을 완료한 계정. 같은 제공자로 다시 로그인하여 기존 사용자 흐름 진행

가입 세션 행을 잠근 뒤 회원 저장·가입 토큰 소비·서비스 토큰 발급을 하나의 트랜잭션으로 처리합니다. 동시에 같은 가입 토큰을 제출해도 성공한 요청 이후에는 재사용할 수 없습니다. 실패해 롤백되면 토큰 소비와 회원 저장도 함께 취소됩니다. 응답을 잃어버려 재사용이 거부되면 같은 제공자의 로그인 API를 다시 호출하면 됩니다.

### POST /api/auth/refresh

```json
{"refreshToken":"CURRENT_REFRESH_TOKEN"}
```

실제 리프레시 토큰은 43자리 Base64URL 문자열입니다. 위 문자열은 설명용 자리표시자입니다. 성공 시 가입 완료 응답과 같은 형태의 **새 토큰 쌍**을 반환합니다. 기존 리프레시 토큰은 한 번만 사용할 수 있습니다.

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
  "name": "Google 이름",
  "nickname": "별을찾는사람",
  "birthDate": "2000-05-21"
}
```

성공 `200`, 인증 실패/회원 부재 시 `401`. `name`은 없으면 null입니다.

## 토큰과 세션 동작

- 액세스 JWT에는 `sub`(서비스 내부 숫자 회원 ID를 문자열로 표현, 예: `"1"`), `iss`, `aud`, `iat`, `exp`, `jti`, `token_use=access`만 포함합니다. 이름·이메일·Google subject·사진은 넣지 않습니다.
- 기존 `google:{sub}` 또는 UUID 형식 액세스 토큰은 거부합니다. 이번 가입 흐름 변경 전의 `token_use` 없는 액세스 토큰도 거부합니다. 기존 미완료 회원은 Google 인증 후 가입을 완료해야 합니다.
- 가입 세션은 기본 10분이며 `app.auth.token.signup-ttl-seconds`로 설정합니다.
- 리프레시 토큰은 32바이트 난수로 생성하며 JWT가 아닙니다. DB에는 SHA-256 해시만 저장합니다.
- 갱신은 세션 행과 토큰 행을 동일한 순서로 잠그는 트랜잭션으로 처리합니다. 이전 토큰 소비와 새 토큰 저장은 함께 커밋됩니다.
- 소비한 토큰은 재사용 탐지를 위해 남깁니다. 재사용되면 해당 세션을 폐기하며, `401` 예외가 나더라도 폐기 변경사항은 커밋합니다.
- Google 공개키는 Spring Security/Nimbus로 조회·캐시합니다. JWT 시간 검증은 기본 60초 오차를 허용합니다.
- 만료된 세션과 토큰 이력의 정기 정리 작업은 아직 없습니다. 정리할 때는 세션 만료 후 해당 토큰 이력을 먼저 삭제하고 세션을 삭제합니다. 활성 세션의 소비된 토큰 이력은 재사용 탐지에 필요합니다.

## 가입 흐름 DB 전환

이전 스키마는 필요한 기존 마이그레이션을 순서대로 적용한 다음 `docs/migrate-user-signup.sql`을 적용합니다. 앱/쓰기를 중지하고 백업 후 실행하며 SQL은 자동 실행하지 않습니다. 새 DB는 최신 `auth-schema.sql`만 사용합니다.

기존 사용자의 생년월일·동의 여부를 임의로 채우지 않기 위해 새 가입 필드는 DB에서 nullable로 추가합니다. `signupCompletedAt`이 없는 사용자는 로그인·리프레시로 서비스 토큰을 받을 수 없습니다. 가입 완료 시 기존 숫자 사용자 ID는 유지하고 이전 리프레시 세션은 폐기합니다. 새 사용자는 가입 API 검증을 통과한 후 필수 정보와 동의 시각을 모두 저장합니다.

가입용 토큰 이력도 자동 정리 작업은 아직 없습니다. 만료된 가입 세션에 보관된 이메일/이름의 정리 정책은 배포 전에 결정해야 합니다.

## 확인 범위

MVP 요청에 따라 테스트 코드는 추가하거나 실행하지 않습니다. 컴파일을 확인했으며 실제 Google·카카오 계정/Android 로그인과 MySQL의 회원 등록·갱신·재사용 탐지 동작은 별도 연동 확인이 필요합니다.

참고: [Google 서버 인증](https://developers.google.com/identity/sign-in/android/backend-auth), [OAuth 리프레시 토큰 보안 권고](https://www.rfc-editor.org/rfc/rfc9700.html#section-4.14).
