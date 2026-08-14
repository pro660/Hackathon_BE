# 로컬 인증 API Postman 테스트

## 1. 서버 실행

MySQL을 실행하고 `.env.example`을 참고해 환경 변수를 설정한 뒤 애플리케이션을 시작합니다.

```powershell
.\gradlew.bat bootRun
```

로컬 기본 주소는 `http://localhost:8080`입니다. 인증번호는 로컬 프로필에서 애플리케이션 로그의 `email verification code` 항목으로 확인할 수 있습니다.

## 2. Postman 공통 설정

- `baseUrl`: `http://localhost:8080`
- 회원가입, 로그인, 재발급, 로그아웃 요청에는 `Origin: http://localhost:3000` 헤더가 필요합니다.
- Postman의 cookie jar를 켜 두면 `refresh_token` 쿠키가 자동 저장·교체됩니다.
- 운영에서는 `JWT_SECRET`을 32자 이상의 별도 비밀값으로 설정하고 `REFRESH_COOKIE_SECURE=true`, `AUTH_LOG_VERIFICATION_CODE=false`를 사용합니다.

## 3. 테스트 순서

### 3.1 인증번호 요청

`POST {{baseUrl}}/api/auth/email-verifications`

```json
{
  "email": "postman@example.com",
  "purpose": "SIGNUP"
}
```

응답은 `202 Accepted`이며, 서버 로그에서 6자리 인증번호를 확인합니다.

### 3.2 인증번호 확인

`POST {{baseUrl}}/api/auth/email-verifications/confirm`

```json
{
  "email": "postman@example.com",
  "purpose": "SIGNUP",
  "verificationCode": "로그에서 확인한 6자리 값"
}
```

응답의 `data.signupToken`을 Postman 환경 변수 `signupToken`으로 저장합니다.

### 3.3 로그인 아이디 중복 확인

`GET {{baseUrl}}/api/auth/login-ids/user_1234/availability`

`data.available`이 `true`인지 확인합니다.

### 3.4 회원가입

`POST {{baseUrl}}/api/auth/signup`

헤더: `Origin: http://localhost:3000`

```json
{
  "signupToken": "{{signupToken}}",
  "loginId": "user_1234",
  "password": "password123",
  "passwordConfirm": "password123",
  "termsAgreements": [
    {
      "termsType": "SERVICE_TERMS",
      "termsVersion": "2026-08-01",
      "agreed": true
    },
    {
      "termsType": "PRIVACY_POLICY",
      "termsVersion": "2026-08-01",
      "agreed": true
    },
    {
      "termsType": "EMAIL_MARKETING",
      "termsVersion": "2026-08-01",
      "agreed": false
    }
  ],
  "nickname": "오늘뭐입지",
  "gender": "NOT_SPECIFIED"
}
```

`201 Created`, 응답 본문의 `accessToken`, 그리고 `refresh_token` 쿠키 생성을 확인합니다.

### 3.5 로그인

`POST {{baseUrl}}/api/auth/login`

헤더: `Origin: http://localhost:3000`

```json
{
  "loginId": "user_1234",
  "password": "password123"
}
```

응답의 `data.accessToken`을 `accessToken` 환경 변수로 저장합니다.

### 3.6 토큰 재발급

`POST {{baseUrl}}/api/auth/refresh`

헤더: `Origin: http://localhost:3000`

본문은 없습니다. 저장된 `refresh_token` 쿠키로 요청되며, 성공하면 쿠키가 새 값으로 교체됩니다. 교체 전 쿠키를 다시 사용하면 `REFRESH_TOKEN_INVALID`가 반환되어야 합니다.

### 3.7 로그아웃

`POST {{baseUrl}}/api/auth/logout`

헤더:

- `Origin: http://localhost:3000`
- `Authorization: Bearer {{accessToken}}`

본문은 없습니다. `204 No Content`와 `refresh_token` 쿠키 삭제를 확인합니다.
