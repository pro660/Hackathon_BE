# 입을래? Backend

2026 중앙해커톤 서비스 **입을래?**의 Spring Boot 백엔드 저장소입니다.

> - 문서 기준일: **2026-08-15**
> - `main` 구현 기준: `6d8815d` — `[FEAT] MCM 제품 카탈로그 조회 및 Import 기능 구현 (#25)`
> - API 공통 규칙 원본: [`API_CONVENTIONS.md`](./API_CONVENTIONS.md)
> - README는 **현재 진행 상황, 실행 방법, 배포 계획 요약**입니다.
> - 세부 API 계약은 `API_CONVENTIONS.md`와 최신 팀 합의를 우선합니다.
> - 백엔드 배포 계획은 기존 Railway에서 **Gabia(가비아)**로 변경했습니다.

---

## 프로젝트 소개

**입을래?**는 20~30대 명품 관심 사용자와 보유자가 제품을 구매하기 전부터 보유한 이후까지 제품을 더 자주, 다양하게 활용하고 관리할 수 있도록 돕는 모바일 웹 서비스입니다.

취향 분석과 MCM 제품 추천에서 시작해 구매 전 활용 가능성 분석, 마이 아이템 관리, 제품 패스포트, 스마트 착용 추천, 장소 추천과 활용 지원까지 하나의 흐름으로 연결하는 것을 목표로 합니다.

### MVP 상위 범위

| 영역 | 주요 내용 | 현재 상태 |
| --- | --- | --- |
| 로그인·회원 | 일반 로그인, Kakao·Naver 소셜 로그인, 회원 정보 관리·탈퇴 | 🔄 일반 인증 API 별도 브랜치 구현 중 · `main` 미병합 |
| 취향 분석 | 단계형 취향 입력·조회·수정·재분석 | ⏳ DB 기반 확정 · 기능 미구현 |
| MCM 제품 | 제품 목록·상세, 카테고리·색상·가격 필터, 페이지네이션·다중 정렬 | ✅ `main` 구현 완료 |
| MCM 제품 데이터 | MCM 샘플 제품 60개, Cloudinary 이미지, 추천용 태그 | ✅ `main` 반영 완료 |
| 제품 추천 | 취향 기반 MCM 제품 추천 | ⏳ 정책/DB 기반 확정 · 기능 미구현 |
| 제품 찜 | 찜 등록·취소·목록, 중복 방지 | ⏳ DB 기반 완료 · API 미구현 |
| 구매 전 활용성 | 취향·보유 아이템 등을 이용한 활용 가능성 분석 | ⏳ 정책/DB 기반 확정 · 기능 미구현 |
| 마이 아이템 | 등록·목록·검색·필터·상세·수정·삭제 | ⏳ DB 기반 완료 · API 미구현 |
| 이미지·AI 제품 분석 | 사용자 이미지 업로드, AI Job 생성·조회·실패·재시도 | ⏳ 정책/DB 기반 확정 · 기능 미구현 |
| 제품 패스포트 | 제품/구매 정보와 사용 이력을 조합해 조회 | ⏳ 기존 DB로 구현 가능 · API 미구현 |
| 사용·활용 지원 | 착용 기록, 활용도 분석, 오래 사용하지 않은 제품 안내 | ⏳ 기존 DB로 구현 가능 · 기능 미구현 |
| 스마트 착용 추천 | 보유 제품과 조건을 이용한 착용 추천 생성·저장 | ⏳ 정책/DB 기반 확정 · 기능 미구현 |
| 장소 추천 | Kakao Local 기반 장소 검색·추천·저장 | ⏳ 정책/DB 기반 확정 · 기능 미구현 |
| 마이페이지·홈 | 사용자 정보와 주요 기능 결과 조합 | ⏳ 기능 미구현 |
| 백엔드 배포 | Spring Boot 운영 서버 배포 | ⏳ **Gabia 배포 예정** |

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
| GitHub Actions CI | ✅ 완료 | `main` 대상 PR/Push 시 `clean check` |
| H2 테스트 기반 | ✅ 완료 | Flyway OFF, `create-drop` |
| Flyway/Testcontainers 기반 | ✅ 완료 | MySQL 8.4 Integration Test 분리 |
| 운영 Migration | ✅ 완료 | **V1~V13** |
| 최종 DB 스키마 검증 | ✅ 완료 | V1~V13 전체 Migration 및 주요 제약조건 검증 |
| MCM 제품 조회 API | ✅ 완료 | 목록·상세·필터·페이지네이션·다중 정렬 |
| MCM 제품 Importer/Validator | ✅ 완료 | 60개 카탈로그 검증 및 MySQL 적재 |
| MCM 제품 최종 데이터 | ✅ 완료 | 상품 60개 · 이미지 60개 · 태그 매핑 341개 |
| 제품 카탈로그 PR | ✅ 완료 | PR #25 Squash and merge |
| 일반 인증 API | 🔄 진행 중 | `feat/auth-api`에서 구현 진행 · **현재 README의 구현 기준인 `main`에는 미병합** |
| local/prod JPA Schema 설정 | ⚠️ 보완 필요 | 현재 `main`은 `ddl-auto=update`, 최종 정책은 `validate` |
| 외부 서비스 연동 | ⏳ 대기 | 사용자 이미지 Cloudinary, OpenAI, Resend, Kakao Local 등 |
| 개발용 백엔드 배포 | ⏳ 대기 | **Gabia 서버 배포 및 운영 DB 연결 검증 예정** |
| FE 원격 연동 | ⏳ 대기 | 운영 API 주소 및 도메인/HTTPS 확정 후 진행 |

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
PR #22 main 병합
    ↓
MCM 제품 카탈로그 조회 API + Importer + 최종 60개 데이터
    ↓
PR #25 main 병합
    ↓
현재
├─ 일반 인증 API 병렬 개발 (`feat/auth-api`, main 미병합)
├─ 후속 도메인 기능 개발 준비
└─ Gabia 배포 환경 구성 준비
```

---

## 현재 구현된 핵심 기능

### 1. 공통 백엔드 기반

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

### 2. 공통 API 응답

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

### 3. 날짜·시간 정책

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

## MCM 제품 카탈로그

PR #25를 통해 제품 카탈로그 기능이 `main`에 반영되었습니다.

### 제품 조회 API

#### 제품 목록

```http
GET /api/products
```

지원 Query Parameter:

| Parameter | 설명 | 예시 |
| --- | --- | --- |
| `category` | 제품 카테고리 | `BAG` |
| `color` | 대표 색상 | `BLACK` |
| `minPrice` | 최소 가격 | `500000` |
| `maxPrice` | 최대 가격 | `2000000` |
| `page` | 0부터 시작하는 페이지 번호 | `0` |
| `size` | 페이지 크기, 1~100 | `20` |
| `sort` | 정렬 조건, 반복 전달 가능 | `price,asc` |

다중 정렬 예시:

```http
GET /api/products?category=BAG&color=BLACK&sort=price,asc&sort=createdAt,desc
```

`sort`가 없으면 기본적으로 `createdAt,desc`를 적용합니다.

현재 허용 정렬 필드:

```text
createdAt
name
price
```

#### 제품 상세

```http
GET /api/products/{productId}
```

존재하지 않거나 비활성화된 제품은 `PRODUCT_NOT_FOUND` 오류로 처리합니다.

### 최종 MCM 샘플 데이터

현재 `src/main/resources/data/mcm-products.json`에 최종 MCM 카탈로그를 포함합니다.

```text
전체 상품: 60
WOMEN: 30
MEN: 30
이미지: 60
상품당 대표 이미지: 1
태그 매핑: 341
```

카테고리:

```text
BAG
LEATHER_GOODS
FASHION_ACCESSORY
CLOTHING
SHOES
```

제품 데이터에는 Cloudinary에 업로드된 WebP 이미지의 URL과 `publicId`가 반영되어 있으며, 각 제품은 대표 이미지 1개를 가집니다.

### ProductTag

최종 기준 태그는 19개입니다.

```text
STYLE
- CASUAL
- FORMAL
- NEAT
- GLAMOROUS

SEASON
- SPRING
- SUMMER
- AUTUMN
- WINTER
- ALL_SEASON

OCCASION
- DAILY
- DATE
- TRAVEL
- GATHERING
- CEREMONY
- OUTDOOR
- OTHER

FEATURE
- COMPACT
- SPACIOUS
- MULTIWAY
```

DB에는 코드만 저장하고 사용자 화면용 한글명은 프론트엔드에서 매핑합니다.

### 카탈로그 Importer

애플리케이션 시작 시 Importer 실행 여부는 다음 환경변수로 제어합니다.

```env
APP_PRODUCT_IMPORT_ENABLED=false
```

기본값은 `false`입니다.

운영 DB에서 카탈로그를 언제 자동 적재할지와 `true` 사용 시점은 Gabia 배포 절차를 확정할 때 함께 결정합니다. 기본값이 `false`이므로 운영 서버를 단순 기동하는 것만으로는 60개 상품이 자동 Import되지 않습니다.

Validator와 Importer는 현재 최종 데이터에 대해 다음을 검증합니다.

- Validator: 전체 상품 수 60개
- Validator: WOMEN/MEN 각 30개
- Validator: 각 성별의 5개 소스 카테고리별 6개
- Validator: SKU 중복 없음
- Validator: `brand=MCM`, `status=ACTIVE`
- Validator: 상품당 이미지 1개, `isPrimary=true`, `sortOrder=0`
- Validator: STYLE / SEASON / OCCASION 필수 태그
- Importer: JSON의 태그가 DB 기준 `product_tags`에 존재하는지 확인
- Importer: SKU 기준 upsert 및 이미지·태그 매핑 교체
- Integration Test: 동일 JSON 재Import 시 상품 수가 증가하지 않는지 확인

최종 카탈로그 통합 테스트는 Testcontainers MySQL 8.4에서 실제 JSON Reader → Validator → Importer 전체 흐름을 검증합니다.

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

### 현재 JPA Schema 설정 주의

현재 `main`의 local/prod 설정에는 다음 값이 남아 있습니다.

```properties
spring.jpa.hibernate.ddl-auto=update
```

최종 운영 정책은 Flyway를 Schema 변경의 단일 기준으로 사용하고 JPA는 다음과 같이 Schema만 검증하는 것입니다.

```properties
spring.jpa.hibernate.ddl-auto=validate
```

따라서 Gabia 운영 배포 전 `main` 기준 설정을 최종 정책과 일치시키는 작업이 필요합니다.

---

## 테스트 전략

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
실제 MCM 카탈로그 Import 검증
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

PR 전 전체 검증:

```powershell
.\gradlew.bat clean check --no-daemon
```

PR #25 병합 전 위 명령을 기준으로 전체 테스트를 통과했습니다.

---

## 주요 확정 정책

### 추천 계산

- MCM 제품 추천 점수·순위는 **백엔드 Rule-Based**로 계산합니다.
- 구매 전 활용성 점수는 **백엔드 Rule-Based**로 계산합니다.
- OpenAI는 구매 전 활용성의 개인화 설명 생성에 사용하며 점수 계산 자체를 담당하지 않습니다.
- 장소 추천은 Kakao Local REST API 검색 결과를 기반으로 **백엔드 Rule-Based**로 순위를 계산합니다.

### 마이 아이템·제품 패스포트

- 사용자용 제품 상태 기능은 제거했습니다.
- 관리 기록 이력은 MVP에서 제거했습니다.
- 다음 관리 예정일은 `user_items.next_care_date`로 관리합니다.
- 제품 패스포트는 제품 정보, 구매 정보, 착용 이력을 조합해 제공합니다.
- 활용도와 오래 사용하지 않은 제품 여부는 착용 기록을 이용해 계산합니다.

### 이미지·AI Job

- MCM 카탈로그 상품 이미지는 현재 Cloudinary 연동이 완료되었습니다.
- 사용자 업로드 이미지 저장도 Cloudinary 사용을 계획합니다.
- ITEM 이미지는 현재 정책상 아이템당 최대 3장을 기준으로 합니다.
- 이미지 업로드 실패 시 마이 아이템 저장 자체는 성공할 수 있으며 같은 `myItemId`로 이미지 업로드를 재시도할 수 있습니다.
- AI Job 상태는 `PENDING → PROCESSING → SUCCEEDED / FAILED`입니다.
- `FAILED`는 polling API에서 정상적인 작업 결과 상태로 다루며 실제 요청/시스템 오류와 구분합니다.
- 프론트엔드 polling은 2초 간격, 최대 약 30초를 기준으로 합니다.

### 장소 추천

- 장소 원본 데이터: Kakao Local REST API
- 화면 지도: OpenFreeMap
- 사용자 위치에 따른 거리는 요청 시 계산하며 DB에 고정값으로 저장하지 않습니다.

### 스마트 착용 추천

사용자 화면에서는 **스마트 착용 추천**이라는 명칭을 사용합니다.

DB 및 내부 코드의 기존 `style_plans` 명칭은 현재 내부 저장 구조로 유지합니다.

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
| Backend Deployment | **Gabia** | 예정 |
| Product Image | Cloudinary | MCM 카탈로그 60개 이미지 URL 반영 완료 |
| User Image | Cloudinary | 연동 예정 |
| AI | OpenAI | 연동 예정 |
| Email | Resend | 연동 예정 |
| Place Search | Kakao Local REST API | 연동 예정 |
| Map Rendering | OpenFreeMap | 프론트엔드 사용 예정 |

> 인증 기능의 Spring Security/JWT 관련 구현은 현재 `feat/auth-api` 브랜치에서 진행 중이며 `main` 병합 전이므로 위 표의 현재 `main` 스택에는 포함하지 않았습니다.

---

## 현재 프로젝트 구조

현재 `main`에는 공통 기반과 MCM 제품 카탈로그 도메인이 구현되어 있습니다.

```text
.
├─ .github/
│  └─ workflows/
│     └─ ci.yml
├─ .env.example
├─ API_CONVENTIONS.md
├─ README.md
├─ build.gradle
└─ src/
   ├─ main/
   │  ├─ java/org/likelionhsu/hackathon/
   │  │  ├─ common/
   │  │  ├─ product/
   │  │  │  ├─ controller/
   │  │  │  ├─ dto/
   │  │  │  ├─ entity/
   │  │  │  ├─ importer/
   │  │  │  ├─ repository/
   │  │  │  └─ service/
   │  │  └─ HackathonBeApplication.java
   │  └─ resources/
   │     ├─ data/
   │     │  └─ mcm-products.json
   │     ├─ db/migration/
   │     │  ├─ V1__...
   │     │  ├─ ...
   │     │  └─ V13__...
   │     ├─ application.properties
   │     ├─ application-local.properties
   │     └─ application-prod.properties
   └─ test/
      ├─ java/org/likelionhsu/hackathon/
      │  ├─ common/
      │  └─ product/
      └─ resources/
         └─ application-test.properties
```

도메인 기능은 **package-by-domain** 구조를 사용합니다.

현재 별도 원격 브랜치 `feat/auth-api`에서 인증 도메인이 개발 중이며 `main`에는 아직 포함되지 않았습니다.

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
| 다중 Query | 동일 Parameter 이름 반복 |

---

## 배포 계획 — Gabia

백엔드 배포 대상은 기존 Railway 계획에서 **Gabia**로 변경했습니다.

이 README에서 확정적으로 반영하는 변경은 **백엔드 운영 대상을 Railway가 아닌 Gabia로 변경한다는 점**입니다.

```text
Client / Frontend
        ↓
Gabia Spring Boot Backend
        ↓
MySQL
```

Gabia의 실제 서버 상품, MySQL 배치 방식, 운영 도메인, HTTPS/Reverse Proxy 구성은 아직 확정하지 않았으므로 배포 단계에서 결정합니다.

프론트엔드 배포 위치와 `/api/**` 프록시·rewrite 구성은 프론트엔드 팀의 최종 배포 방식에 맞춰 별도로 확정합니다. 기존 Vercel 구성을 유지한다면 백엔드 목적지만 Gabia로 변경하는 방식을 검토할 수 있습니다.

> **문서 정합성 주의:** 현재 `API_CONVENTIONS.md`의 운영 예시에는 기존 Railway 기준 표현이 일부 남아 있습니다. Gabia 배포 구조가 최종 확정되면 해당 문서의 운영 환경 설명도 함께 갱신해야 합니다.

---

## 실행 환경

### 로컬 개발

```env
SPRING_PROFILES_ACTIVE=local
DB_USERNAME=hackathon
DB_PASSWORD=change-me
CORS_ALLOWED_ORIGIN=http://localhost:3000
APP_PRODUCT_IMPORT_ENABLED=false
```

### Gabia 운영 환경

현재 `main`의 `application-prod.properties`가 기대하는 환경변수 이름은 다음과 같습니다. 이는 Gabia가 자동으로 제공하는 변수라는 의미가 아니라, **배포 시 Gabia 서버 환경에 직접 주입할 애플리케이션 설정 계약**입니다.

```env
SPRING_PROFILES_ACTIVE=prod
PORT=8080

MYSQLHOST=change-me
MYSQLPORT=3306
MYSQLDATABASE=change-me
MYSQLUSER=change-me
MYSQLPASSWORD=change-me

CORS_ALLOWED_ORIGIN=https://frontend-domain.example
APP_PRODUCT_IMPORT_ENABLED=false
```

실제 비밀번호·Token·Secret은 Git에 저장하지 않습니다.

`MYSQLHOST` 등의 변수명은 현재 `application-prod.properties` 계약을 그대로 기록한 것입니다. Gabia 배포 구조를 확정하면서 필요하면 변수명을 더 일반적인 이름으로 정리할 수 있지만, 그 경우 코드와 `.env.example`을 함께 변경합니다.

Gabia 서버의 실제 환경변수 값과 Cloudinary, OpenAI, Resend, Kakao Local, 인증 관련 Secret은 각 기능 구현 및 배포 단계에서 안전하게 주입합니다.

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

PowerShell 예시:

```powershell
$env:DB_USERNAME="hackathon"
$env:DB_PASSWORD="자신의_DB_비밀번호"
$env:CORS_ALLOWED_ORIGIN="http://localhost:3000"
$env:APP_PRODUCT_IMPORT_ENABLED="false"
```

전체 환경변수 예시는 [.env.example](./.env.example)을 참고합니다.

### 테스트

빠른 테스트:

```powershell
.\gradlew.bat test
```

DB Integration Test:

```powershell
.\gradlew.bat integrationTest
```

PR 전 전체 검증:

```powershell
.\gradlew.bat clean check --no-daemon
```

### 서버 실행

```powershell
.\gradlew.bat bootRun
```

기본 주소:

```text
http://localhost:8080
```

---

## 현재 공개 API

현재 `main`에서 확인되는 서비스 도메인 API는 MCM 제품 조회 API입니다.

### MCM 제품 목록

```http
GET /api/products
```

예시:

```http
GET /api/products?category=BAG&color=BLACK&page=0&size=20&sort=price,asc
```

### MCM 제품 상세

```http
GET /api/products/{productId}
```

인증 관련 API는 현재 별도 브랜치에서 개발 중이며 `main`에 병합된 뒤 이 목록에 추가합니다.

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
git switch -c feat/<feature-name>
```

### PR 전 확인

- [ ] 최신 `main`에서 작업을 시작했습니다.
- [ ] 관련 테스트를 작성하거나 기존 테스트로 변경사항을 검증했습니다.
- [ ] `./gradlew clean check --no-daemon`가 성공합니다.
- [ ] `git diff --check`에 문제가 없습니다.
- [ ] 비밀번호·Token·실제 환경변수 파일이 포함되지 않았습니다.
- [ ] DB 변경 시 기존 Migration을 수정하지 않고 신규 Version을 추가했습니다.
- [ ] API 계약 변경 시 프론트엔드와 먼저 합의했습니다.

---

## 다음 개발 흐름

MCM 제품 카탈로그는 PR #25로 `main`에 병합 완료했습니다.

현재 이후 우선 흐름은 다음과 같습니다.

```text
일반 인증 API
(`feat/auth-api` 작업 중)
        ↓
제품 찜 / 제품 추천
        ↓
구매 전 활용 가능성
        ↓
마이 아이템
        ↓
이미지·AI 제품 분석
        ↓
제품 패스포트
        ↓
스마트 착용 추천 / 장소 추천 / 홈
```

배포 작업은 기능 개발과 병렬로 다음 순서로 진행할 수 있습니다.

```text
Gabia 서버 환경 확정
        ↓
Java 21 / 운영 환경 구성
        ↓
MySQL 연결
        ↓
SPRING_PROFILES_ACTIVE=prod
        ↓
Flyway Migration 검증
        ↓
Spring Boot 배포
        ↓
도메인·HTTPS·CORS 설정
        ↓
프론트엔드 원격 API 연동
```

세부 역할분배는 회의에 따라 바뀔 수 있으므로 README에는 개인별 고정 담당자보다 **기능 흐름과 현재 구현 상태**를 중심으로 기록합니다.

---

## 관련 문서

- [API 공통 규칙](./API_CONVENTIONS.md)
- [환경변수 예시](./.env.example)
- [백엔드 저장소](https://github.com/pro660/Hackathon_BE)
