# 입을래? Backend

2026 중앙해커톤 서비스 **입을래?**의 Spring Boot 백엔드 저장소입니다.

> - 문서 기준일: **2026-08-13**
> - 구현 기준: `main`의 `8fd093c` — `[FEAT] 도메인 DB 스키마 및 확정 정책 반영 (#22)`
> - API 공통 규칙 원본: [`API_CONVENTIONS.md`](./API_CONVENTIONS.md)
> - README는 **진행 현황과 실행 방법 요약**입니다. 세부 API 계약은 `API_CONVENTIONS.md`와 최신 팀 합의를 우선합니다.

---

## 프로젝트 소개

**입을래?**는 20~30대 명품 관심 사용자와 보유자가 제품을 구매하기 전부터 보유한 이후까지 더 자주, 다양하게 활용하고 관리할 수 있도록 돕는 모바일 웹 서비스입니다.

취향 분석과 MCM 제품 추천에서 시작해 구매 전 활용 가능성 분석, 마이 아이템 관리, 스마트 착용 추천, 장소 추천, 제품 패스포트와 활용 지원까지 하나의 흐름으로 연결하는 것을 목표로 합니다.

### MVP 상위 범위

| 영역 | 주요 내용 | 현재 상태 |
| --- | --- | --- |
| 로그인·회원 | 일반 로그인, Kakao·Naver 소셜 로그인, 회원 정보 관리·탈퇴 | 일반 인증 API 구현 · OAuth/회원 관리 미구현 |
| 취향 분석 | 단계형 취향 입력·조회·수정·재분석 | DB 기반 확정 · 기능 미구현 |
| MCM 제품 | 샘플 제품 목록·상세, 카테고리·색상·가격대 필터 | DB 기반 완료 · API 미구현 |
| 제품 추천 | 취향 기반 MCM 제품 추천 | 정책/DB 기반 확정 · 기능 미구현 |
| 제품 찜 | 찜 등록·취소·목록, 중복 방지 | DB 기반 완료 · API 미구현 |
| 구매 전 활용성 | 취향·보유 아이템 등을 이용한 활용 가능성 분석 | 정책/DB 기반 확정 · 기능 미구현 |
| 마이 아이템 | 등록·목록·검색·필터·상세·수정·삭제 | DB 기반 완료 · API 미구현 |
| 이미지·AI 제품 분석 | 사용자 이미지 업로드, AI Job 생성·조회·실패·재시도 흐름 | 정책/DB 기반 확정 · 기능 미구현 |
| 제품 패스포트 | 제품/구매 정보와 사용 이력을 한 화면에서 조회 | 기존 DB로 구현 가능 · API 미구현 |
| 사용·활용 지원 | 착용 기록, 활용도 분석, 오래 사용하지 않은 제품 안내 | 기존 DB로 구현 가능 · 기능 미구현 |
| 스마트 착용 추천 | 보유 제품과 조건을 이용한 착용 추천 생성·저장 | 정책/DB 기반 확정 · 기능 미구현 |
| 장소 추천 | Kakao Local 기반 장소 검색·추천·저장 | 정책/DB 기반 확정 · 기능 미구현 |
| 마이페이지·홈 | 사용자 정보와 주요 기능 결과 조합 | 기능 미구현 |

체험 모드·비회원 이용, 휴대폰 인증, Google 로그인은 현재 MVP 범위에서 제외합니다.

---

## 현재 개발 진행 상황

| 영역 | 상태 | 현재 내용 |
| --- | --- | --- |
| MVP 기능 범위 | ✅ 완료 | 핵심 MVP 범위 정리 |
| Git/PR 협업 규칙 | ✅ 완료 | 기능 브랜치 → PR → Squash and merge |
| API 공통 규칙 | ✅ 완료 | `API_CONVENTIONS.md` 기준 |
| 공통 응답·예외 처리 | ✅ 완료 | 성공/오류 응답, Validation, Query/Path 예외 처리 |
| 날짜·시간/JPA Auditing | ✅ 완료 | UTC, `Clock.systemUTC()`, `BaseTimeEntity` |
| Swagger/OpenAPI | ✅ 완료 | springdoc 기반 구성 |
| GitHub Actions CI | ✅ 완료 | `main` PR/Push 시 `clean check` |
| H2 테스트 기반 | ✅ 완료 | Flyway OFF, `create-drop` |
| Flyway/Testcontainers 기반 | ✅ 완료 | MySQL 8.4 Integration Test 분리 |
| 운영 Migration | ✅ 완료 | **V1~V13** |
| 최종 DB 스키마 검증 | ✅ 완료 | V1~V13 전체 Migration 및 주요 제약조건 검증 |
| 일반 인증 API | ✅ 완료 | 이메일 인증, 회원가입, 로그인, JWT, Refresh Rotation, 로그아웃 |
| 실행 환경·외부 서비스 설정 | 🔄 보완 필요 | 인증 메일 발송과 Cloudinary, OpenAI, Resend, Kakao Local 실제 연동 필요 |
| local/prod JPA Schema 설정 | ✅ 완료 | Flyway 단일 변경 기준, JPA `ddl-auto=validate` 적용 |
| 개발용 백엔드 배포 | ⏳ 대기 | Railway 배포 및 운영 DB 연결 검증 필요 |
| 도메인 Entity/Repository/Service/API | ⏳ 대기 | DB 스키마 완료 후 실제 기능 개발 시작 단계 |
| FE 원격 연동 | ⏳ 대기 | 개발 서버 배포·도메인 API 구현 이후 진행 |

### 현재 위치

```text
공통 기반 구축
    ↓
정책·ERD 확정
    ↓
Flyway/Testcontainers 기반 구축
    ↓
V1~V13 운영 DB 스키마 구현·검증
    ↓
PR #22 main 병합 완료
    ↓
현재: 실제 도메인 기능 개발 시작
    ↓
다음 우선 작업: MCM 제품 카탈로그 API
```

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
- Flyway
- H2
- Testcontainers
- springdoc-openapi
- GitHub Actions

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

### 날짜·시간 정책

서버·DB·API의 기준 시간대는 **UTC**입니다.

- 정확한 시점: `Instant`
- 날짜: `LocalDate`
- 시간: `LocalTime`
- 기준 Clock: `Clock.systemUTC()`
- Hibernate JDBC timezone: UTC
- Jackson timezone: UTC
- 프론트엔드 화면 표시: `Asia/Seoul`

JPA Entity의 생성·수정 시각은 `BaseTimeEntity`와 JPA Auditing으로 관리합니다.

---

## 데이터베이스

DB Schema 변경은 **Flyway Migration**으로 관리합니다.

### 현재 Migration

| Version | 내용 |
| --- | --- |
| V1 | 사용자·인증 관련 테이블 |
| V2 | AI Job |
| V3 | MCM 제품 카탈로그 |
| V4 | 취향 분석·찜 |
| V5 | 마이 아이템·이미지 |
| V6 | 착용 기록 및 초기 관리 기록 구조 |
| V7 | 제품 추천·구매 활용도 분석 |
| V8 | 장소·스마트 착용 추천 내부 저장 구조 |
| V9 | ProductTag `display_name` 제거 |
| V10 | ProductTag 최종 19개 기준 데이터 |
| V11 | 마이 아이템 관리 스키마 단순화 |
| V12 | 구매 활용도 `duplicate_similarity_score` 제거 |
| V13 | 회원 탈퇴 재인증 토큰 |

### Migration 운영 원칙

- 이미 공유·적용된 V1~V13 Migration은 수정하지 않습니다.
- 향후 Schema 변경은 **V14 이후 신규 Migration**으로 추가합니다.
- 공유 DB에서 `Flyway clean`을 사용하지 않습니다.
- DB 변경은 PR에서 Migration과 Integration Test를 함께 검토합니다.

### 테스트 전략

빠른 애플리케이션 테스트:

```text
H2 In-memory
Flyway OFF
ddl-auto=create-drop
```

DB Integration Test:

```text
Testcontainers MySQL 8.4
Flyway ON
운영 Migration 적용
FK / CHECK / UNIQUE / Schema 정합성 검증
```

Gradle Task:

```text
test
→ integration Tag 제외

integrationTest
→ integration Tag만 실행

check
→ test + integrationTest
```

**`integrationTest`와 `check`를 실행하려면 Docker가 실행 중이어야 합니다.**

### 현재 설정상 주의점

`application-local.properties`와 `application-prod.properties`는 다음 정책을 사용합니다.

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Flyway가 Schema 변경의 단일 기준이며 JPA는 `ddl-auto=validate`로 Entity와 Schema의 정합성만 검증합니다.

---

## 주요 확정 정책

### 추천 계산

- MCM 제품 추천 점수·순위는 **백엔드 Rule-Based**로 계산합니다.
- 구매 전 활용성 점수는 **백엔드 Rule-Based**로 계산합니다.
- OpenAI는 구매 전 활용성의 개인화 설명 생성에 사용하며, 점수 계산 자체를 담당하지 않습니다.
- 장소 추천은 Kakao Local REST API 검색 결과를 기반으로 **백엔드 Rule-Based**로 순위를 계산합니다.

### ProductTag

최종 기준 태그는 19개입니다.

```text
STYLE: CASUAL, FORMAL, NEAT, GLAMOROUS
SEASON: SPRING, SUMMER, AUTUMN, WINTER, ALL_SEASON
OCCASION: DAILY, DATE, TRAVEL, GATHERING, CEREMONY, OUTDOOR, OTHER
FEATURE: COMPACT, SPACIOUS, MULTIWAY
```

DB에는 코드만 저장하고 사용자 화면용 한글명은 프론트엔드에서 매핑합니다.

### 마이 아이템·제품 패스포트

- 사용자용 제품 상태 기능은 제거했습니다.
- 관리 기록 이력은 MVP에서 제거했습니다.
- 다음 관리 예정일은 `user_items.next_care_date`로 관리합니다.
- 제품 패스포트는 제품 정보, 구매 정보, 착용 이력을 조합해 제공합니다.
- 활용도와 오래 사용하지 않은 제품 여부는 착용 기록을 이용해 계산합니다.

### 이미지·AI Job

- 사용자 업로드 이미지 저장 서비스는 Cloudinary를 사용합니다.
- ITEM 이미지는 현재 정책상 아이템당 최대 3장을 기준으로 합니다.
- 이미지 업로드 실패 시 마이 아이템 저장 자체는 성공할 수 있으며, 같은 `myItemId`로 이미지 업로드를 재시도할 수 있습니다.
- AI Job 상태는 `PENDING → PROCESSING → SUCCEEDED / FAILED`입니다.
- `FAILED`는 polling API에서 정상적인 작업 결과 상태로 다루며 실제 요청/시스템 오류와 구분합니다.
- 프론트엔드 polling은 2초 간격, 최대 약 30초를 기준으로 합니다.

### 장소 추천

- 장소 원본 데이터: Kakao Local REST API
- 화면 지도: OpenFreeMap
- 사용자 위치에 따른 거리는 요청 시 계산하며 DB에 고정값으로 저장하지 않습니다.

### 스마트 착용 추천

사용자 화면에서는 **스마트 착용 추천**이라는 명칭을 사용합니다.

DB 및 내부 코드의 기존 `style_plans` 명칭은 당장 변경하지 않고 내부 저장 구조로 유지합니다.

---

## 기술 스택

| 구분 | 기술 | 현재 기준 |
| --- | --- | --- |
| Language | Java | 21 |
| Framework | Spring Boot | 4.1.0 |
| Build | Gradle Wrapper | 9.5.1 |
| Web | Spring Web MVC | REST API |
| Persistence | Spring Data JPA, Hibernate | MySQL |
| Migration | Flyway | V1~V13 |
| Validation | Jakarta Validation | 요청값 검증 |
| Fast Test DB | H2 | In-memory |
| DB Integration Test | Testcontainers MySQL | MySQL 8.4 |
| API Docs | springdoc-openapi | 3.1.0 |
| CI | GitHub Actions | PR/main `clean check` |
| Backend Deployment | Railway | 예정 |
| Frontend Deployment | Vercel | 예정 |
| User Image | Cloudinary | 연동 예정 |
| AI | OpenAI | 연동 예정 |
| Email | Resend | 연동 예정 |
| Place Search | Kakao Local REST API | 연동 예정 |
| Map Rendering | OpenFreeMap | 프론트엔드 사용 예정 |

---

## 현재 프로젝트 구조

현재 실제 애플리케이션 Java 코드는 공통 기반 중심이며, 도메인별 Entity/Repository/Service/Controller는 앞으로 추가합니다.

```text
.
├─ .github/workflows/ci.yml
├─ .env.example
├─ API_CONVENTIONS.md
├─ README.md
├─ build.gradle
└─ src/
   ├─ main/
   │  ├─ java/org/likelionhsu/hackathon/
   │  │  ├─ common/
   │  │  └─ HackathonBeApplication.java
   │  └─ resources/
   │     ├─ db/migration/
   │     │  ├─ V1__...
   │     │  ├─ ...
   │     │  └─ V13__...
   │     ├─ application.properties
   │     ├─ application-local.properties
   │     └─ application-prod.properties
   └─ test/
      ├─ java/org/likelionhsu/hackathon/
      │  └─ common/
      └─ resources/application-test.properties
```

실제 도메인 기능은 **package-by-domain** 구조를 사용합니다.

---

## API 공통 규칙

API 계약의 단일 기준은 [`API_CONVENTIONS.md`](./API_CONVENTIONS.md)입니다.

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
| 날짜·시간 | UTC 기반 ISO 8601 |
| Enum | 영문 대문자 `SNAKE_CASE` |
| ID | API에서 문자열로 전달 |
| 금액 | KRW 원 단위 정수 |
| 페이지 번호 | `page=0`부터 시작 |
| 페이지 크기 | `1~100` |

---

## 실행 환경

### 로컬 개발

```env
DB_USERNAME=hackathon
DB_PASSWORD=change-me
CORS_ALLOWED_ORIGIN=http://localhost:3000
JWT_ISSUER=hackathon-be
JWT_SECRET=replace-with-at-least-32-random-characters
REFRESH_COOKIE_SECURE=false
AUTH_LOG_VERIFICATION_CODE=true
```

### 운영

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
JWT_ISSUER=hackathon-be
JWT_SECRET=replace-with-a-production-secret-of-at-least-32-characters
REFRESH_COOKIE_SECURE=true
AUTH_LOG_VERIFICATION_CODE=false
```

실제 비밀번호·Token·Secret은 Git에 저장하지 않습니다.

Cloudinary, OpenAI, Resend, Kakao Local에 필요한 추가 환경변수는 실제 연동 구현과 함께 확정·추가합니다.

---

## 시작하기


### 요구 환경

- JDK 21
- MySQL
- Git
- Docker Desktop — `integrationTest`, `check` 실행 시 필요

### 저장소 Clone

```bash
git clone https://github.com/pro660/Hackathon_BE.git
cd Hackathon_BE
```

### MySQL 준비

```sql
CREATE DATABASE hackathon_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

### 환경변수 설정


```powershell
$env:DB_USERNAME="hackathon"
$env:DB_PASSWORD="자신의_DB_비밀번호"
$env:CORS_ALLOWED_ORIGIN="http://localhost:3000"
```
전체 환경변수 예시는 [.env.example](./.env.example)을 참고합니다.


### 테스트

빠른 테스트:

```powershell
.\gradlew test
```

DB Integration Test:

```powershell
.\gradlew integrationTest
```

PR 전 전체 검증:

```powershell
.\gradlew clean check
```

### 서버 실행

```powershell
.\gradlew bootRun
```

기본 주소: `http://localhost:8080`

---

## 현재 공개 API

현재 상태 확인과 일반 인증 API를 제공합니다.

```http
GET /api/health
POST /api/auth/email-verifications
POST /api/auth/email-verifications/confirm
GET /api/auth/login-ids/{loginId}/availability
POST /api/auth/signup
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
```

Postman 실행 순서는 [로컬 인증 API Postman 테스트](./docs/POSTMAN_AUTH_TEST.md)를 참고합니다.

---

## Swagger / OpenAPI

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

---

## GitHub Actions CI

다음 상황에서 백엔드 검증이 자동 실행됩니다.

```text
main 대상 Pull Request
main 브랜치 Push
```

CI 환경:

```text
OS: Ubuntu
Java: Temurin 21
Command: ./gradlew clean check --no-daemon
```

`check`에 `integrationTest`가 연결되어 있으므로 CI에서도 Flyway + Testcontainers MySQL Integration Test를 함께 수행합니다.

---

## Git과 협업 규칙

`main`에 직접 Push하지 않고 작업 브랜치를 사용합니다.

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
git switch -c feat/product-catalog
```

### PR 전 확인

- [ ] 최신 `main`에서 작업을 시작했습니다.
- [ ] 관련 테스트를 작성하거나 기존 테스트로 변경사항을 검증했습니다.
- [ ] `./gradlew clean check`가 성공합니다.
- [ ] `git diff --check`에 문제가 없습니다.
- [ ] 비밀번호·Token·실제 환경변수 파일이 포함되지 않았습니다.
- [ ] DB 변경 시 기존 Migration을 수정하지 않고 신규 Version을 추가했습니다.
- [ ] API 계약 변경 시 프론트엔드와 먼저 합의했습니다.

---

## 현재 착수 예정 기능 흐름

DB 스키마 구축 작업은 PR #22로 `main`에 병합 완료했습니다.

이제 실제 도메인 기능 개발을 시작합니다. 아래 흐름은 현재 먼저 착수할 제품·마이 아이템 계열 작업 기준이며, 다른 도메인은 병렬로 진행할 수 있습니다.

```text
MCM 제품 카탈로그
- 제품 목록
- 제품 상세
- 카테고리·색상·가격대 필터
- 샘플 MCM 제품 Import
        ↓
제품 찜
        ↓
구매 전 활용 가능성
        ↓
마이 아이템
        ↓
이미지·AI 제품 분석
        ↓
제품 패스포트
```

인증·사용자·취향·스마트 착용 추천·장소 추천·홈 등의 다른 도메인도 각 기능 브랜치에서 병렬로 구현합니다.

세부 역할분배는 회의에 따라 바뀔 수 있으므로 README에는 개인별 고정 담당자보다 **기능 흐름과 현재 구현 상태**를 중심으로 기록합니다.

---

## 관련 문서

- [API 공통 규칙](./API_CONVENTIONS.md)
- [환경변수 예시](./.env.example)
- [백엔드 저장소](https://github.com/pro660/Hackathon_BE)
