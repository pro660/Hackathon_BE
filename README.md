# 입을래? Backend

2026 중앙해커톤 서비스 **입을래?**의 Spring Boot 백엔드 저장소입니다.

> - 문서 기준일: 2026-08-11
> - 구현 기준: `main`의 `d77ef96`
> - API 공통 규칙 원본: [`API_CONVENTIONS.md`](./API_CONVENTIONS.md)

---

## 프로젝트 소개

**입을래?**는 20~30대 명품 관심 사용자와 보유자가 제품을 구매하기 전부터 보유한 이후까지 더 자주, 다양하게 활용하고 관리할 수 있도록 돕는 모바일 웹 서비스입니다.

취향 분석과 제품 추천에서 시작해 구매 전 활용 가능성 분석, 스타일 플랜, 장소 추천, 보유 제품 관리와 활용 지원까지 하나의 흐름으로 연결하는 것을 목표로 합니다.

### MVP 상위 범위

| 영역 | 주요 내용 | 현재 상태 |
| --- | --- | --- |
| 로그인·취향 분석 | `loginId + password` 일반 로그인, Kakao·Naver 소셜 로그인, 단계형 취향 분석 | 정책 확정·미구현 |
| 제품 추천 | 취향 기반 샘플 MCM 제품 추천, 필터, 찜 | 확정·미구현 |
| 구매 전 활용성 분석 | 내 아이템과 구매 예정 제품의 활용 가능성 분석 | 확정·미구현 |
| 오늘의 스타일 플랜 | 제품과 조건을 기반으로 스타일 플랜 생성·저장 | 확정·미구현 |
| 장소 추천 | 스타일에 맞는 장소 추천·필터·저장·외부 지도 연결 | 확정·미구현 |
| 마이 아이템 | 보유 제품 등록·조회·검색·필터 | 확정·미구현 |
| 제품 패스포트 | 보유 제품별 정보와 이력 확인 | 확정·미구현 |
| 사용 기록 | 착용 기록 저장 | 확정·미구현 |
| 활용 지원 | 활용도 분석, 다시 활용할 제품, 스마트 착용 추천 | 확정·미구현 |
| 마이페이지 | 사용자 관련 정보와 저장 항목 관리 | 확정·미구현 |

체험 모드와 비회원 이용은 제공하지 않으며, 휴대폰 인증과 Google 로그인은 MVP 범위에서 제외합니다.

현재는 **4·5·6번 정책 확정 및 문서 동기화를 완료하고, 7번 실행 환경·환경변수 세팅 보완으로 넘어가는 단계**입니다.

---

## 현재 개발 진행 상황

상태는 다음과 같이 구분합니다.

- **완료**: 현재 `main` 코드에 구현 및 검증 완료
- **정책 확정·미구현**: 정책과 계약은 확정됐지만 실제 기능 코드는 아직 구현되지 않음
- **보완 필요**: 기존 공통 기반은 있으나 확정된 후속 정책에 맞춘 추가 설정이 필요함
- **대기**: 진행 순서 또는 선행 작업 때문에 구현 대기
- **미확정**: 세부 정책 또는 기술 선택을 추가로 결정해야 함

| 단계 | 작업 | 상태 | 현재 내용 |
| --- | --- | --- | --- |
| 1 | MVP 기능 범위 확정 | ✅ 완료 | 상위 MVP 기능 범위 확정 |
| 2 | 백엔드 협업 규칙 확정 | ✅ 완료 | 작업 브랜치·PR·Squash merge 기준 확정 |
| 3 | FE/BE API 공통 규칙 확정 | ✅ 완료 | `API_CONVENTIONS.md` 반영 |
| 4 | 로그인·사용자 정책 확정 | ✅ 정책 확정·미구현 | 일반 로그인, Kakao·Naver OAuth, Access/Refresh Token 정책 확정 |
| 5 | 이미지·AI·외부 서비스 정책 확정 | ✅ 정책 확정·미구현 | Cloudinary, OpenAI, Resend, Kakao Local 및 이미지·AI 처리 정책 확정 |
| 6 | ERD와 도메인 구조 확정 | ✅ 정책 확정·미구현 | 28개 Entity, 관계·제약조건·삭제·Flyway 방향 확정 |
| 7 | 실행 환경과 환경변수 세팅 보완 | 🔄 보완 필요 | 기존 `local`/`prod` 공통 기반에 4·5·6번 확정 정책용 설정 보완 필요 |
| 8 | 테스트 환경 구축 | ✅ 완료 | H2 기반 독립 테스트 환경 구축 |
| 9 | 백엔드 CI 구축 | ✅ 완료 | GitHub Actions 전체 테스트 자동화 |
| 10 | 개발용 백엔드 첫 배포 | ⏳ 대기 | Railway용 운영 설정은 준비, 실제 배포 대기 |
| 11 | 공통 응답·예외 처리 구현 | ✅ 완료 | 성공/오류 응답 및 Query/Path 예외 처리 완료 |
| 12 | Swagger/OpenAPI 구축 | ✅ 완료 | springdoc 기반 Swagger UI/OpenAPI 구성 |
| 13 | 샘플 데이터와 DB 변경 관리 | ⏳ 대기 | 확정된 ERD·Flyway 정책 기준 후속 구현 예정 |
| 14 | 프론트 원격 연동 테스트 | ⏳ 대기 | 개발 서버 첫 배포 이후 진행 |
| 15 | 실제 기능 개발 시작 | ⏳ 대기 | 7번 환경 보완·10번 첫 배포·13번 DB 변경 관리 등 선행 작업 이후 시작 |

---

## 현재 구현된 공통 기반

### Spring Boot 기본 환경

- Java 21
- Spring Boot 4.1.0
- Gradle Wrapper 9.5.1
- Spring Web MVC
- Spring Data JPA
- Jakarta Validation
- MySQL
- H2 Test DB

### 공통 API 응답

성공 응답:

```json
{
  "success": true,
  "data": {}
}
```

오류 응답:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "오류 메시지"
  }
}
```

Validation 오류:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "입력값을 확인해 주세요.",
    "fields": [
      {
        "field": "page",
        "reason": "잘못된 입력값입니다."
      }
    ]
  }
}
```

### 공통 예외 처리

현재 다음 예외 흐름이 공통 응답 형식으로 처리됩니다.

- 비즈니스 예외
- Request Body Validation 실패
- 잘못된 JSON Body
- 존재하지 않는 Endpoint
- 예상하지 못한 서버 예외
- Query Parameter 타입 변환 실패
- Path Variable 타입 변환 실패
- Query Parameter Validation 실패
- Path Variable Validation 실패
- 필수 Query Parameter 누락

Query/Path 관련 잘못된 요청은 공통적으로 `400 Bad Request`와 `VALIDATION_ERROR` 응답을 사용합니다.

### 날짜·시간 정책

서버·DB·API의 기준 시간대는 **UTC**입니다.

- 정확한 시점: `Instant`
- 날짜: `LocalDate`
- 시간: `LocalTime`
- 기준 Clock: `Clock.systemUTC()`
- Hibernate JDBC timezone: UTC
- Jackson timezone: UTC
- 프론트엔드 화면 표시: `Asia/Seoul`

JPA Entity의 생성·수정 시각은 `BaseTimeEntity`와 JPA Auditing을 이용해 자동으로 관리합니다.

---

## 확정된 4·5·6번 정책

아래 내용은 **정책과 계약이 최종 확정된 상태**를 의미하며, 실제 인증 코드·외부 서비스 Adapter·Flyway Migration·JPA Entity가 현재 `main`에 구현됐다는 의미는 아닙니다.

### 로그인·사용자

- 체험 모드와 비회원 이용은 제공하지 않음
- 일반 로그인은 사용자가 정한 `loginId + password` 사용
- 소셜 로그인은 Kakao·Naver 지원
- Google 로그인과 휴대폰 인증은 MVP 제외
- 일반 회원가입은 이메일 인증 후 최종 가입
- 신규 소셜 사용자는 가입 완료 전 `PendingSocialSignup`으로 관리
- Access Token은 JWT이며 응답 Body로 전달
- Refresh Token은 HttpOnly Cookie 기반
- 운영 브라우저는 Vercel의 동일 Origin `/api/**`를 호출하고 Vercel이 Railway로 rewrite
- OAuth Callback도 프론트 도메인의 `/api/auth/oauth/**`를 통해 백엔드로 전달
- OAuth Provider Callback은 일회용 `oauth_state`로 검증
- 일반 가입·로그인·소셜 가입 완료·Refresh·Logout POST는 허용된 신뢰 Origin을 검증
- Origin 검증 실패는 `403 ORIGIN_NOT_ALLOWED`

### 이미지·AI·외부 서비스

- 사용자 이미지 저장: Cloudinary
- AI: OpenAI
- 이메일: Resend SMTP
- 장소 검색: Kakao Local REST API
- MCM 카탈로그 이미지는 `ProductImage`, 사용자 업로드 이미지는 `ImageAsset`으로 분리
- `ITEM` 이미지는 아이템당 최대 3장, `sortOrder`는 `0~2`
- 사용자당 ACTIVE `PROFILE` 이미지는 최대 1장
- `AI_INPUT`은 업로드 완료 후에도 `TEMPORARY`
- AI Job 입력 이미지는 현재 Job에 연결된 이미지만 조회

```text
purpose = AI_INPUT
AND status = TEMPORARY
AND owner_user_id = AiJob.userId
AND ai_job_id = currentJobId
```

- Job에 연결되지 않은 `AI_INPUT`은 다음 조건으로 24시간 후 정리

```text
purpose = AI_INPUT
AND status = TEMPORARY
AND ai_job_id IS NULL
AND created_at < 현재 시각 - 24시간
```

- Job에 연결된 이미지는 위 Scheduler가 삭제하지 않고 Job 성공·실패 후 기존 lifecycle에 따라 `DELETE_PENDING → DELETED` 처리
- Cloudinary 업로드에는 성공했지만 완료 API가 호출되지 않아 DB에 등록되지 않은 사용자 업로드도 별도의 고아 리소스 정리 대상

### ERD·DB

- 최종 Entity는 28개이며 `PendingSocialSignup`, `SavedPlace`를 포함
- DB Schema 변경은 Flyway로 관리
- 실제 MySQL Schema는 `ddl-auto=validate`로 검증
- 빠른 Controller·Service·Validation 테스트는 H2 + Flyway OFF + `create-drop`
- Migration·FK·Check·Unique·Entity 정합성 테스트는 Testcontainers MySQL + Flyway ON + `validate`
- `User`, `PreferenceProfile` 등 확정 대상은 낙관적 잠금 정책 적용
- 확정된 nullable `ai_job_id` FK는 `ON DELETE SET NULL`

4·5·6번 정책에 필요한 실제 의존성, Secret, 환경변수와 Profile 설정 보완은 **7번 실행 환경·환경변수 세팅 보완 단계에서 처리합니다.**

---

## 기술 스택

| 구분 | 기술 | 현재 기준 |
| --- | --- | --- |
| Language | Java | 21 |
| Framework | Spring Boot | 4.1.0 |
| Build | Gradle Wrapper | 9.5.1 |
| Web | Spring Web MVC | REST API |
| Persistence | Spring Data JPA, Hibernate | MySQL |
| Validation | Jakarta Validation | 요청값 검증 |
| Local DB | MySQL | `hackathon_db` |
| Test DB | H2 | In-memory |
| API Docs | springdoc-openapi | 3.1.0 |
| Test | JUnit 5, Spring Boot Test | 자동 테스트 |
| CI | GitHub Actions | PR/main 자동 테스트 |
| Deployment | Railway | 운영 설정 준비, 실제 배포 대기 |

---

## 현재 프로젝트 구조

```text
.
├─ .github/
│  └─ workflows/
│     └─ ci.yml
├─ .env.example
├─ API_CONVENTIONS.md
├─ build.gradle
├─ gradlew
├─ gradlew.bat
├─ README.md
└─ src/
   ├─ main/
   │  ├─ java/org/likelionhsu/hackathon/
   │  │  ├─ common/
   │  │  │  ├─ config/
   │  │  │  │  ├─ ClockConfig.java
   │  │  │  │  ├─ JpaAuditingConfig.java
   │  │  │  │  ├─ OpenApiConfig.java
   │  │  │  │  └─ WebConfig.java
   │  │  │  ├─ controller/
   │  │  │  │  └─ HealthController.java
   │  │  │  ├─ entity/
   │  │  │  │  └─ BaseTimeEntity.java
   │  │  │  ├─ exception/
   │  │  │  │  ├─ BusinessException.java
   │  │  │  │  ├─ ErrorCode.java
   │  │  │  │  └─ GlobalExceptionHandler.java
   │  │  │  └─ response/
   │  │  │     ├─ ApiResponse.java
   │  │  │     ├─ ErrorDetail.java
   │  │  │     ├─ ErrorResponse.java
   │  │  │     └─ FieldErrorResponse.java
   │  │  └─ HackathonBeApplication.java
   │  └─ resources/
   │     ├─ application.properties
   │     ├─ application-local.properties
   │     └─ application-prod.properties
   └─ test/
      ├─ java/org/likelionhsu/hackathon/
      │  ├─ common/config/
      │  ├─ common/entity/
      │  ├─ common/exception/
      │  │  └─ GlobalExceptionHandlerTest.java
      │  ├─ common/response/
      │  └─ HackathonBeApplicationTests.java
      └─ resources/
         └─ application-test.properties
```

실제 도메인 기능을 추가할 때는 **package-by-domain** 구조를 사용합니다.

```text
org.likelionhsu.hackathon
├─ common/
├─ user/
│  ├─ controller/
│  ├─ service/
│  ├─ repository/
│  ├─ entity/
│  └─ dto/
├─ product/
│  ├─ controller/
│  ├─ service/
│  ├─ repository/
│  ├─ entity/
│  └─ dto/
└─ [domain]/
   ├─ controller/
   ├─ service/
   ├─ repository/
   ├─ entity/
   └─ dto/
```

---

## API 공통 규칙

API 계약의 단일 기준은 [`API_CONVENTIONS.md`](./API_CONVENTIONS.md)입니다.

README와 API 규칙이 다를 경우 최신 `main`의 `API_CONVENTIONS.md`를 우선합니다.

| 항목 | 규칙 |
| --- | --- |
| API 기본 경로 | `/api` |
| Endpoint | 소문자 `kebab-case`, 가능한 한 복수 명사 |
| Path Variable | `lowerCamelCase` |
| Query Parameter | `lowerCamelCase` |
| JSON 필드 | `lowerCamelCase` |
| DB 컬럼 | `snake_case` |
| 성공 응답 | `{ "success": true, "data": ... }` |
| 오류 응답 | `{ "success": false, "error": { "code", "message" } }` |
| Validation 오류 | `error.fields`에 `field`, `reason` 제공 |
| 날짜·시간 | UTC 기반 ISO 8601 |
| Enum | 영문 대문자 `SNAKE_CASE` |
| ID | API에서 문자열로 전달 |
| 금액 | KRW 원 단위 정수 |
| 페이지 번호 | `page=0`부터 시작 |
| 페이지 크기 | `1~100` |
| 목록 | 증가 가능한 목록은 `page`, `size`, `sort` 기반 페이지네이션 |

---

## 실행 환경

Spring Profile은 환경별로 분리되어 있습니다.

### 기본

```text
application.properties
```

기본 profile:

```properties
spring.profiles.default=local
```

### 로컬 개발

```text
application-local.properties
```

사용 환경변수:

```env
DB_USERNAME=hackathon
DB_PASSWORD=change-me
CORS_ALLOWED_ORIGIN=http://localhost:3000
```

`DB_USERNAME`을 지정하지 않으면 기본값 `hackathon`을 사용합니다.

### 운영 환경

```text
application-prod.properties
```

Railway 환경을 기준으로 다음 환경변수를 사용합니다.

```env
SPRING_PROFILES_ACTIVE=prod

PORT=8080

MYSQLHOST=change-me
MYSQLPORT=3306
MYSQLDATABASE=change-me
MYSQLUSER=change-me
MYSQLPASSWORD=change-me

CORS_ALLOWED_ORIGIN=https://frontend-domain.example
```

실제 비밀번호와 운영 환경 값은 Git에 저장하지 않습니다.

환경변수 이름과 예시는 [`.env.example`](./.env.example)을 참고합니다.

4·5·6번에서 확정된 JWT, OAuth, Cookie, Cloudinary, OpenAI, Resend, Kakao Local 등에는 추가 실행 설정이 필요합니다.

구체적인 환경변수 이름·구조와 Profile 적용 방식은 이번 문서 동기화에서 새로 확정하지 않고 **7번 실행 환경·환경변수 세팅 보완 단계에서 검토하고 반영합니다.**

---

## 시작하기

### 1. 요구 환경

- JDK 21
- MySQL
- Git

Gradle은 저장소의 Wrapper를 사용하므로 별도로 설치하지 않아도 됩니다.

### 2. 저장소 Clone

```bash
git clone https://github.com/pro660/Hackathon_BE.git
cd Hackathon_BE
```

### 3. MySQL 준비

```sql
CREATE DATABASE hackathon_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

기본 DB 사용자 `hackathon`을 사용하는 경우:

```sql
CREATE USER 'hackathon'@'localhost'
IDENTIFIED BY '자신의_DB_비밀번호';

GRANT ALL PRIVILEGES
ON hackathon_db.*
TO 'hackathon'@'localhost';

FLUSH PRIVILEGES;
```

기존 MySQL 사용자가 있다면 `DB_USERNAME` 환경변수를 이용할 수 있습니다.

### 4. 환경변수 설정

Windows PowerShell:

```powershell
$env:DB_USERNAME="hackathon"
$env:DB_PASSWORD="자신의_DB_비밀번호"
$env:CORS_ALLOWED_ORIGIN="http://localhost:3000"
```

macOS / Linux:

```bash
export DB_USERNAME='hackathon'
export DB_PASSWORD='자신의_DB_비밀번호'
export CORS_ALLOWED_ORIGIN='http://localhost:3000'
```

IntelliJ:

```text
Run
→ Edit Configurations
→ HackathonBeApplication
→ Environment variables
```

### 5. 테스트

Windows PowerShell:

```powershell
.\gradlew test
```

macOS / Linux:

```bash
./gradlew test
```

테스트 환경은 `test` profile의 **H2 In-memory DB**를 사용하므로 로컬 MySQL 데이터에 접근하지 않습니다.

현재 테스트에서는 다음 공통 기반을 검증합니다.

- Spring Application Context
- H2 테스트 DB 연결
- 공통 성공·오류 응답 JSON
- Request Body Validation
- Query/Path Validation
- Query/Path 타입 변환 오류
- 필수 Query Parameter 누락
- 비즈니스 예외
- 없는 Endpoint
- 안전한 500 응답
- UTC 날짜·시간 정책
- JPA Auditing

6번 정책에서 다음 DB 테스트 구조가 추가로 확정됐으며 실제 도입은 후속 구현에서 진행합니다.

```text
빠른 Controller·Service·Validation 테스트
→ H2
→ Flyway OFF
→ ddl-auto=create-drop

Migration·FK·Check·Unique·Entity 정합성 테스트
→ Testcontainers MySQL
→ Flyway ON
→ ddl-auto=validate
```

### 6. 서버 실행

Windows PowerShell:

```powershell
.\gradlew bootRun
```

macOS / Linux:

```bash
./gradlew bootRun
```

기본 실행 주소:

```text
http://localhost:8080
```

---

## 현재 공개 API

현재 실제 서비스 도메인 API 개발 전이며 공개된 API는 상태 확인용 API입니다.

### Health Check

```http
GET /api/health
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "status": "ok",
    "message": "Hackathon backend is running"
  }
}
```

---

## Swagger / OpenAPI

springdoc 기반 API 문서가 구성되어 있습니다.

### Swagger UI

```text
http://localhost:8080/swagger-ui/index.html
```

### OpenAPI JSON

```text
http://localhost:8080/v3/api-docs
```

현재 OpenAPI 기본 정보:

```text
Title: 입을래? API
Version: v1
```

실제 도메인 API가 구현되면 Controller별 `@Tag`, `@Operation` 등을 추가해 문서를 확장합니다.

---

## GitHub Actions CI

다음 상황에서 백엔드 전체 테스트가 자동 실행됩니다.

```text
main 대상 Pull Request
main 브랜치 Push
```

CI 환경:

```text
OS: Ubuntu
Java: Temurin 21
Command: ./gradlew clean test --no-daemon
```

중복 실행은 자동으로 취소하도록 설정되어 있습니다.

---

## CORS와 인증 Origin 검증

CORS Origin은 소스 코드에 고정하지 않고 환경변수로 관리합니다.

현재 `main`의 인증 도입 전 로컬 CORS 구현은 다음과 같습니다.

```text
Allowed origin: http://localhost:3000
Path: /api/**
Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Allowed headers: *
Credentials: false
Preflight max age: 3600 seconds
```

위 `Credentials: false`는 현재 코드 상태이며 최종 인증 정책 자체를 의미하지 않습니다.

운영 브라우저 요청은 다음 구조로 통일합니다.

```text
Browser
→ Vercel 동일 Origin /api/**
→ Railway Backend
```

운영에서는 Fetch 기본 `credentials: "same-origin"`을 사용할 수 있습니다.

로컬 `http://localhost:3000`에서 `http://localhost:8080`으로 Cookie가 필요한 요청을 직접 호출할 때는 다음 원칙을 사용합니다.

```text
Fetch: credentials: "include"
Axios: withCredentials: true
Backend: allowCredentials(true)
Access-Control-Allow-Origin: 정확한 허용 Origin
Wildcard Origin 사용 금지
```

다음 인증 POST 요청은 CORS 처리와 별도로 신뢰 Origin을 검증합니다.

```text
POST /api/auth/signup
POST /api/auth/login
POST /api/auth/oauth/signup
POST /api/auth/refresh
POST /api/auth/logout
```

`Origin`이 누락되거나 `null`이거나 허용 목록과 정확히 일치하지 않으면 `403 ORIGIN_NOT_ALLOWED`를 반환합니다.

OAuth Provider Callback은 위 POST Origin 검증 대상이 아니며 기존 `oauth_state` 검증을 적용합니다.

**CORS 허용 Origin과 인증 POST의 신뢰 Origin 검증은 별도 설정입니다.**

두 설정의 구체적인 환경변수 구조와 Swagger UI를 포함한 로컬 신뢰 Origin 관리 방식은 **7번 실행 환경·환경변수 세팅 보완 단계에서 확정합니다.**

---

## 데이터베이스

### 로컬

```text
Host: localhost
Port: 3306
Database: hackathon_db
Username: DB_USERNAME
Password: DB_PASSWORD
Timezone: UTC
Encoding: UTF-8
```

현재 `main`의 개발 환경 JPA 설정:

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

위 `ddl-auto=update`는 아직 Flyway가 구현되기 전의 현재 코드 상태입니다.

### 테스트

```text
Database: H2 In-memory
URL: jdbc:h2:mem:hackathon_test
Schema lifecycle: create-drop
Timezone: UTC
```

### 운영

Railway MySQL 환경변수를 사용할 수 있도록 `prod` profile이 준비되어 있습니다.

6번 정책에서 DB 변경 관리 방식은 다음과 같이 확정했습니다.

```text
Migration: Flyway
JPA Schema 검증: ddl-auto=validate
```

현재 코드의 `ddl-auto=update`는 최종 운영 정책이 아닙니다.

Flyway 의존성, Migration 파일, Testcontainers MySQL과 실제 `validate` 설정 변경은 아직 구현 전이며 후속 단계에서 반영합니다.

---

## 백엔드 담당 영역

### 정승원

도메인:

- 사용자
- 취향
- 제품
- 제품 추천
- 찜
- 구매 전 활용성 분석

공통 작업:

- ✅ 공통 응답 구조
- ✅ 공통 예외 처리
- ✅ 테스트 환경
- ✅ 시간·날짜 공통 설정
- ⏳ 샘플 데이터 관리

### 장재준

도메인:

- 마이 아이템
- 제품 패스포트
- 사용 기록
- 스타일 플랜
- 장소
- 마이페이지

공통 작업:

- ✅ Swagger/OpenAPI
- ✅ GitHub Actions CI
- ✅ 환경별 설정 및 환경변수 구성
- ⏳ 개발용 첫 배포

### 담당 미확정

- 활용도 분석
- 다시 활용할 제품 추천
- 스마트 착용 추천

---

## Git과 협업 규칙

`main`에 직접 Push하지 않고 작업 브랜치를 사용합니다.

기본 작업 흐름:

```text
main 최신화
→ 작업 브랜치 생성
→ 개발
→ 테스트
→ Commit
→ Push
→ Pull Request
→ 검토
→ Squash and merge
→ 작업 브랜치 삭제
```

작업 시작 예시:

```bash
git switch main
git pull --ff-only origin main
git switch -c feat/user-api
```

사용 가능한 브랜치 Prefix 예시:

```text
feat/
fix/
chore/
refactor/
test/
docs/
ci/
```

Merge 방식은 **Squash and merge**를 사용합니다.

### PR 전 확인

- [ ] 최신 `main`에서 작업을 시작했습니다.
- [ ] 관련 테스트를 작성하거나 기존 테스트로 변경사항을 검증했습니다.
- [ ] 전체 테스트가 성공합니다.
- [ ] `git diff --check`에 문제가 없습니다.
- [ ] 비밀번호·토큰·실제 환경변수 파일이 포함되지 않았습니다.
- [ ] DB 변경사항을 다른 백엔드 담당자에게 공유했습니다.
- [ ] API 계약 변경 시 프론트엔드와 먼저 합의했습니다.

---

## FE/BE API 변경 절차

프론트엔드 저장소에서 백엔드 API 계약을 독자적으로 변경하지 않습니다.

1. 변경이 필요한 API와 문제 상황을 공유합니다.
2. 프론트엔드와 백엔드가 요청·응답 형식을 협의합니다.
3. 백엔드 API 계약을 먼저 확정합니다.
4. 백엔드 코드와 문서를 수정하고 PR을 생성합니다.
5. 프론트엔드는 확정된 API 계약을 기준으로 구현합니다.

다음 변경은 특히 사전 협의가 필요합니다.

- Endpoint
- HTTP Method
- 요청·응답 필드
- 필드 자료형
- 필수/선택 여부
- Enum
- 상태 코드
- 오류 코드
- 페이지네이션
- `null` 허용 여부

세부 기준은 [`API_CONVENTIONS.md`](./API_CONVENTIONS.md)를 따릅니다.

---

## 현재 남은 주요 작업

### 7번 실행 환경·환경변수 세팅 보완

기존 `local`/`prod` Profile과 공통 환경변수 기반은 구현되어 있습니다.

4·5·6번 정책이 최종 확정됨에 따라 실제 구현 전에 다음 영역의 실행 설정 보완 검토가 남아 있습니다.

- JWT / Refresh Token
- Kakao·Naver OAuth
- Cookie
- CORS Credentials
- 인증 POST 신뢰 Origin 검증
- Cloudinary
- OpenAI
- Resend
- Kakao Local


CORS 허용 Origin과 인증 POST의 신뢰 Origin 검증은 별도 설정으로 유지하며, 구체적인 환경변수 이름·구조는 **7번에서 확정합니다.**

### 배포

Railway용 `prod` profile과 환경변수 구조는 준비되어 있습니다.

남은 항목:

- 실제 개발용 백엔드 첫 배포
- 운영 DB 연결 검증
- 배포 주소 확정
- 프론트엔드 원격 연동
- 운영 CORS 검증
- HTTPS/도메인 구성

---

## 다음 개발 순서

현재 기준 상태와 다음 작업은 다음과 같습니다.

```text
완료: 4·5·6번 정책 확정 및 문서 동기화

다음: 7번 실행 환경·환경변수 세팅 보완
        ↓
이후 현재 진행표의 다음 미완료 단계 진행
```

공통 응답, 예외 처리, H2 테스트 기반, 날짜·시간 설정, Swagger/OpenAPI, CI와 `local`/`prod` 실행 환경의 공통 기반은 현재 `main`에 구현되어 있습니다.

---

## 관련 문서

- [API 공통 규칙](./API_CONVENTIONS.md)
- [환경변수 예시](./.env.example)
- [백엔드 저장소](https://github.com/pro660/Hackathon_BE)