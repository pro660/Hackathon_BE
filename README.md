# 입을래? Backend

2026 중앙해커톤 서비스 **입을래?**의 Spring Boot 백엔드 저장소입니다.

> - 문서 기준일: 2026-08-08
> - 구현 기준: `main`의 [`39b1bd7`](https://github.com/pro660/Hackathon_BE/commit/39b1bd7baad85de8e74e6028f82afc7bad8e43b7)
> - API 규칙 원본: [`API_CONVENTIONS.md`](./API_CONVENTIONS.md)

## 프로젝트 소개

입을래?는 20~30대 개인 사용자가 명품을 구매하기 전부터 보유한 이후까지 더 잘 활용하도록 돕는 모바일 웹 서비스입니다.

구매 전 탐색과 취향 분석, 제품 추천에서 시작해 보유 제품의 착용·관리·재활용까지 하나의 흐름으로 연결하는 것을 목표로 합니다.

### MVP 상위 범위

| 영역 | 주요 내용 | 현재 상태 |
| --- | --- | --- |
| 체험·취향 분석 | 취향 입력과 분석 결과 제공 | 확정·미구현 |
| 제품 추천 | 샘플 MCM 제품 추천과 구매 전 활용 가능성 분석 | 확정·미구현 |
| 오늘의 스타일 플랜 | 상황에 맞는 스타일 계획 제공 | 확정·미구현 |
| 장소 추천 | 스타일 플랜과 연결된 장소 추천 | 확정·미구현 |
| 마이 아이템 | 보유 제품 등록·조회와 아이템 패스포트 제공 | 확정·미구현 |
| 사용·관리 기록 | 착용 기록과 관리 기록 저장 | 확정·미구현 |
| 활용 지원 | 활용도, 재활용, 스마트 착용 추천. 최종 담당자는 아직 미정 | 확정·미구현 |

상위 MVP 범위는 확정됐지만, 기능명세서의 세부 기능 상태는 아직 `작성중`입니다. 개별 Endpoint, 요청·응답 DTO, Enum, 정렬·필터 조건, 예외 흐름은 기능별 설계 과정에서 확정합니다.

## 현재 개발 상태

상태는 다음 세 가지로 구분합니다.

- **확정·구현**: 결정된 내용이 현재 `main` 코드에 반영됨
- **확정·미구현**: 팀 결정은 완료됐지만 현재 `main`에는 아직 없음
- **미확정**: 선택지나 세부 정책을 아직 결정하지 않음

| 항목 | 상태 | 현재 내용 |
| --- | --- | --- |
| Spring Boot 기본 프로젝트 | **확정·구현** | Java 21, Spring Boot 4.1.0, Gradle Wrapper 9.5.1 |
| 개발 데이터베이스 | **확정·구현** | MySQL, Spring Data JPA, Hibernate, `DB_PASSWORD` 환경변수 |
| 테스트 데이터베이스 | **확정·구현** | `test` 프로필에서 H2 인메모리 DB와 `create-drop` 사용 |
| 상태 확인 API | **확정·구현** | `GET /api/health` |
| 로컬 CORS | **확정·구현** | `http://localhost:3000`에서 `/api/**` 호출 허용 |
| 공통 성공·오류 응답 | **확정·구현** | `ApiResponse`, `ErrorResponse`, Validation의 `error.fields` |
| 전역 예외 처리 | **확정·구현** | 비즈니스 예외, 요청 Body 검증·파싱 오류, 없는 Endpoint, 예상 밖 오류 처리 |
| Query·Path 오류 처리 보강 | **확정·미구현** | Query Parameter·Path Variable 관련 Spring 예외를 공통 400 형식으로 보강 예정 |
| 날짜·시간 공통 기준 | **확정·구현** | 서버·DB·API는 UTC, Java 기준 시각은 `Clock.systemUTC()` |
| JPA Auditing | **확정·구현** | `BaseTimeEntity`의 `createdAt`, `updatedAt`을 `Instant`로 자동 기록 |
| API 공통 규칙 | **확정·구현** | 최신 규칙이 `API_CONVENTIONS.md`에 반영됨 |
| 실제 서비스 도메인 | **확정·미구현** | 사용자, 취향, 제품, 추천, 찜, 마이 아이템, 기록, 스타일 플랜, 장소, 마이페이지 등 |
| 기능 중심 패키지 | **확정·미구현** | `package-by-domain` 사용 예정이며 아직 도메인 패키지는 없음 |
| Lombok | **확정·미구현** | 사용하기로 했지만 현재 `build.gradle` 의존성에는 없음 |
| Swagger/OpenAPI | **확정·미구현** | 도입 예정이며 현재 springdoc 의존성·설정 없음 |
| GitHub Actions CI | **확정·미구현** | 도입 예정이며 현재 Workflow 없음 |
| 이미지 URL 저장 흐름 | **확정·미구현** | URL을 저장하는 방향은 확정됐지만 업로드 구현은 없음 |
| 이미지 업로드 기술 | **미확정** | 저장소, Multipart 방식, 파일 형식·용량 제한 미결정 |
| 회원 진입 기능 | **확정·미구현** | 이메일·휴대폰 인증번호와 Kakao·Naver·Google 소셜 로그인 방향, 관련 API·코드는 아직 없음 |
| AI 기반 기능 | **확정·미구현** | 서비스 범위에는 포함되지만 공급자·모델·호출 정책은 미확정 |
| AI 세부 기술 | **미확정** | 공급자, 모델, Prompt, 비용, Timeout, Retry, Fallback 미결정 |
| 인증 기술 | **미확정** | Spring Security, JWT, Refresh Token, Cookie·Header 방식 등 미결정 |
| ERD·도메인 Entity | **미확정** | 관계와 제약조건을 포함한 도메인 모델 설계 전 |
| 배포 환경 | **미확정** | 플랫폼, 운영 DB, 도메인, 운영 CORS, 배포 주소 미결정 |

현재 공개된 서비스 API는 상태 확인용 `GET /api/health` 하나입니다. 사용자·제품·추천·마이 아이템 등 실제 도메인의 Controller, Service, Repository, Entity는 아직 구현되지 않았습니다.

## 기술 스택

| 구분 | 기술 | 현재 기준 |
| --- | --- | --- |
| Language | Java | 21 |
| Framework | Spring Boot | 4.1.0 |
| Build | Gradle Wrapper | 9.5.1 |
| Web | Spring Web MVC | REST API |
| Persistence | Spring Data JPA, Hibernate | MySQL 연동 |
| Validation | Jakarta Validation | 요청 DTO 검증 |
| Development DB | MySQL | `hackathon_db` |
| Test DB | H2 | 인메모리 `hackathon_test` |
| Test | JUnit 5, Spring Boot Test | Controller·직렬화·시간·JPA 기반 테스트 |

## 현재 프로젝트 구조

```text
src/
├─ main/
│  ├─ java/org/likelionhsu/hackathon/
│  │  ├─ common/
│  │  │  ├─ config/
│  │  │  │  ├─ ClockConfig.java
│  │  │  │  ├─ JpaAuditingConfig.java
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
│     └─ application.properties
└─ test/
   ├─ java/org/likelionhsu/hackathon/
   │  ├─ common/config/DateTimePolicyTest.java
   │  ├─ common/entity/
   │  │  ├─ BaseTimeEntityTest.java
   │  │  └─ TestAuditEntity.java
   │  ├─ common/exception/GlobalExceptionHandlerTest.java
   │  ├─ common/response/CommonResponseSerializationTest.java
   │  └─ HackathonBeApplicationTests.java
   └─ resources/
      └─ application-test.properties
```

도메인 기능을 추가할 때는 기능 중심 패키지 구조를 사용합니다.

```text
org.likelionhsu.hackathon
├─ common/
├─ user/
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

## API 공통 규칙

API 계약의 단일 기준은 [`API_CONVENTIONS.md`](./API_CONVENTIONS.md)입니다. README와 API 규칙이 다르면 최신 `main`의 `API_CONVENTIONS.md`를 우선합니다.

| 항목 | 규칙 |
| --- | --- |
| API 기본 경로 | `/api` |
| Endpoint | 소문자 `kebab-case`, 가능한 한 복수 명사 |
| JSON 필드 | `lowerCamelCase` |
| DB 컬럼 | `snake_case` |
| 성공 응답 | `{ "success": true, "data": ... }` |
| 오류 응답 | `{ "success": false, "error": { "code", "message" } }` |
| Validation 오류 | `error.fields`에 `field`, `reason` 제공 |
| 날짜·시간 | UTC 기반 ISO 8601 |
| Enum | 영문 대문자 `SNAKE_CASE` |
| ID | API에서 문자열로 전달 |
| 금액 | MVP에서는 KRW 원 단위 정수, `currency` 필드는 기본 생략 |
| 목록 | 증가 가능한 목록은 `page`, `size`, `sort` 기반 페이지네이션 |

### 성공 응답 예시

```json
{
  "success": true,
  "data": {
    "productId": "123",
    "name": "토트백"
  }
}
```

### 오류 응답 예시

```json
{
  "success": false,
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "제품을 찾을 수 없습니다."
  }
}
```

### Validation 오류 예시

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "입력값을 확인해 주세요.",
    "fields": [
      {
        "field": "name",
        "reason": "이름은 필수입니다."
      }
    ]
  }
}
```

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

`hackathon` 사용자가 없다면 생성하고 권한을 부여합니다.

```sql
CREATE USER 'hackathon'@'localhost'
IDENTIFIED BY '자신의_DB_비밀번호';

GRANT ALL PRIVILEGES
ON hackathon_db.*
TO 'hackathon'@'localhost';

FLUSH PRIVILEGES;
```

이미 사용자가 있다면 생성 명령은 다시 실행하지 않습니다.

### 4. 환경변수 설정

DB 비밀번호는 소스 코드나 Git에 저장하지 않고 `DB_PASSWORD` 환경변수로 전달합니다.

macOS 또는 Linux:

```bash
export DB_PASSWORD='자신의_DB_비밀번호'
```

Windows PowerShell:

```powershell
$env:DB_PASSWORD="자신의_DB_비밀번호"
```

IntelliJ에서는 다음 실행 설정에 추가할 수 있습니다.

```text
Run
→ Edit Configurations
→ HackathonBeApplication
→ Environment variables
→ DB_PASSWORD=자신의_DB_비밀번호
```

### 5. 빌드와 테스트

macOS 또는 Linux:

```bash
./gradlew clean build
```

Windows PowerShell:

```powershell
.\gradlew.bat clean build
```

테스트만 실행하려면 다음 명령을 사용합니다.

```bash
./gradlew test
```

테스트는 `test` 프로필의 H2 인메모리 DB를 사용하므로 로컬 MySQL 데이터에 접근하지 않습니다.

### 6. 서버 실행

macOS 또는 Linux:

```bash
./gradlew bootRun
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

기본 실행 주소는 `http://localhost:8080`입니다.

### 7. 상태 확인

```http
GET http://localhost:8080/api/health
```

현재 정상 응답은 공통 성공 응답으로 감싸집니다.

```json
{
  "success": true,
  "data": {
    "status": "ok",
    "message": "Hackathon backend is running"
  }
}
```

## 데이터베이스와 시간 기준

### 개발 환경

```text
Host: localhost
Port: 3306
Database: hackathon_db
Username: hackathon
Password: DB_PASSWORD 환경변수
Server timezone: UTC
Character encoding: UTF-8
```

현재 JPA 설정은 다음과 같습니다.

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
spring.jackson.time-zone=UTC
```

`ddl-auto=update`는 현재 초기 개발 설정입니다. 장기 유지 여부와 운영 환경의 Migration 전략은 아직 확정되지 않았으므로, 배포 설정과 함께 별도로 결정해야 합니다.

### 테스트 환경

```text
Database: H2 in-memory
URL: jdbc:h2:mem:hackathon_test
Schema lifecycle: create-drop
Timezone: UTC
```

현재 테스트 코드는 다음을 검증합니다.

- Spring Application Context와 H2 연결
- 공통 성공·오류 응답의 JSON 직렬화
- Validation·비즈니스·공통 예외 응답
- UTC 날짜·시간 정책
- `BaseTimeEntity`의 생성·수정 시각 Auditing

## CORS

현재 로컬 프론트엔드 개발 주소만 허용합니다.

```text
Allowed origin: http://localhost:3000
Path: /api/**
Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Allowed headers: *
Credentials: false
Preflight max age: 3600 seconds
```

인증 방식과 배포 주소가 확정되면 Credentials와 운영 Origin을 함께 재검토해야 합니다.

## 담당 영역

| 담당자 | 기능 영역                                                                          | 비고                                                |
| --- |------------------------------------------------------------------------------------|-----------------------------------------------------|
| 정승원 | 사용자, 취향, 제품, 추천, 찜, 구매 전 활용성 분석                                  | 도메인 구현 전                                      |
| 장재준 | 마이 아이템, 아이템 패스포트, 사용·관리 기록, 스타일 플랜, 장소, 마이페이지        | 도메인 구현 전                                      |
| 정승원 | 공통 예외 처리, 공통 응답 구조, 테스트 환경, 시간 날짜 공통 설정, 샘플 데이터 관리 | 구현 완료                                           |
| 장재준 | Swagger/OpenAPI, GitHub Actions CI, 배포·환경변수 영역                             | 담당은 확정됐지만 현재 미구현 또는 세부 방식 미확정 |
| 미정 | 활용도, 재활용, 스마트 착용 추천                                                   | 최종 담당 미확정                                    |

## Git과 협업 규칙

본격적인 기능 개발에서는 `main`에 직접 Push하지 않습니다.

```text
main 최신화
→ 작업 브랜치 생성
→ 개발
→ 빌드·테스트
→ Commit·Push
→ Pull Request
→ 다른 백엔드 팀원의 교차 리뷰와 Approve
→ PR 작성자가 Squash and merge
→ 병합된 작업 브랜치 삭제
```

작업 시작 예시:

```bash
git switch main
git pull origin main
git switch -c feat/user-api
```

`feat/`, `fix/`, `chore/`, `refactor/`, `test/`, `docs/` 등은 사용할 수 있는 예시입니다. 브랜치·커밋 Prefix의 최종 허용 목록은 아직 확정되지 않았으므로, 새 규칙을 추가하거나 제한하기 전에 팀과 합의합니다.

PR 전 확인 사항:

- [ ] 최신 `main`을 반영했습니다.
- [ ] 관련 테스트를 추가하거나 기존 테스트로 변경 내용을 검증했습니다.
- [ ] `clean build`가 성공합니다.
- [ ] 새 API의 요청·응답과 오류 형식을 확인했습니다.
- [ ] DB 변경 내용을 다른 백엔드 담당자에게 공유했습니다.
- [ ] 비밀번호, 토큰, 개인정보, 실제 환경변수 파일이 포함되지 않았습니다.
- [ ] API 계약 변경 시 프론트엔드와 먼저 합의했습니다.

## FE/BE API 변경 절차

프론트엔드 저장소에서 API 계약을 독자적으로 확정하지 않습니다.

1. 프론트엔드가 백엔드 저장소에 API 변경 요청 Issue를 생성합니다.
2. 현재 문제와 원하는 요청·응답 예시를 작성합니다.
3. 프론트엔드와 백엔드 담당자가 변경 가능 여부와 형식을 합의합니다.
4. 백엔드 담당자가 협의안을 코드와 문서에 반영하고 PR을 생성합니다.
5. 프론트엔드는 확정된 백엔드 PR을 기준으로 구현합니다.

호환성이 깨지는 변경은 Endpoint, HTTP Method, 필드명·자료형, 필수값, Enum, `null` 가능 여부, 페이지네이션, 상태 코드·오류 코드의 의미 변경을 포함합니다. 자세한 분류와 PR 작성 기준은 [`API_CONVENTIONS.md`](./API_CONVENTIONS.md)를 따릅니다.

## 아직 확정하지 않은 사항

아래 내용은 구현 전에 반드시 별도 합의가 필요합니다.

- 도메인 ERD, Entity 관계, 제약조건, Index
- 기능별 Endpoint, DTO, Enum, 필터·정렬·페이지네이션 세부 계약
- JWT·Refresh Token·세션·Cookie·Header와 Spring Security 적용 방식
- 이메일·휴대폰 인증번호의 발급·검증 정책과 소셜 계정 통합 방식
- AI 공급자, 모델, 적용 기능, Prompt, 비용 제한, Timeout, Retry, Fallback
- 이미지 저장소, 업로드 API, Multipart 정책, 파일 형식·용량 제한
- 배포 플랫폼, 운영 DB, 도메인, HTTPS, 운영 CORS와 비밀값 관리 방식
- 개인정보 범위, 보관 기간, 삭제 정책
- `ddl-auto=update`의 장기 유지 여부와 Flyway 등 Migration 도구 도입 여부
- 성능 목표, 예상 트래픽, 외부 API 장애 정책
- 브랜치 보호 설정, GitHub에서 강제할 승인 수, 최종 Prefix 목록

미확정 사항은 임의로 구현 기준으로 삼지 않습니다. 기존 확정사항을 변경해야 한다면 기존 결정, 변경 이유, 영향 범위를 먼저 공유하고 팀 동의를 받은 뒤 반영합니다.

## 관련 문서

- [API 공통 규칙](./API_CONVENTIONS.md)
- [백엔드 저장소](https://github.com/pro660/Hackathon_BE)
