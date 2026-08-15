# 입을래? Backend

2026 중앙해커톤 서비스 **입을래?**의 Spring Boot 백엔드 저장소입니다.

> - 문서 기준일: **2026-08-16**
> - `main` 구현 기준: `a936e6f` — `[FEAT] 제품 추천 생성 및 조회 기능 구현 (#29)`
> - 최신 `main` Backend CI: **#34 성공**
> - API 공통 규칙 원본: [`API_CONVENTIONS.md`](./API_CONVENTIONS.md)
> - README는 **현재 구현 상태, 실행 방법, 테스트 전략, 배포 계획, 다음 개발 흐름**을 요약합니다.
> - 세부 API 계약은 `API_CONVENTIONS.md`, 실제 `main` 코드, 최신 팀 합의를 우선합니다.
> - 백엔드 운영 배포 대상은 현재 **Gabia(가비아)**를 기준으로 준비 중입니다.

---

## 프로젝트 소개

**입을래?**는 20~30대 명품 관심 사용자와 보유자가 제품을 구매하기 전부터 보유한 이후까지 제품을 더 자주, 다양하게 활용하고 관리할 수 있도록 돕는 모바일 웹 서비스입니다.

사용자의 취향을 저장하고 MCM 제품을 추천하는 것에서 시작해, 구매 전 활용 가능성 분석, 마이 아이템 등록과 관리, 제품 패스포트, 스마트 착용 추천, 장소 추천과 활용 지원까지 하나의 흐름으로 연결하는 것을 목표로 합니다.

### 현재 MVP 범위

| 영역 | 주요 내용 | 현재 상태 |
| --- | --- | --- |
| 일반 인증 | 이메일 인증, 로그인 ID 중복 확인, 회원가입, 로그인, 토큰 재발급, 로그아웃 | ✅ `main` 구현 완료 · PR #24 |
| 소셜 로그인 | Kakao·Naver 로그인 | 🔄 `feat/social-auth-api` 별도 브랜치 · `main` 미병합 |
| 취향 프로필 | 선호 색상·카테고리·스타일 조회 및 전체 교체 저장 | ✅ `main` 구현 완료 · PR #28 |
| MCM 제품 | 제품 목록·상세, 카테고리·색상·가격 필터, 페이지네이션·다중 정렬 | ✅ `main` 구현 완료 · PR #25 |
| MCM 제품 데이터 | MCM 샘플 제품 60개, Cloudinary 이미지 URL, 추천용 ProductTag | ✅ `main` 반영 완료 |
| 제품 찜 | 찜 등록·해제·목록, 제품 목록/상세의 `favorited` 응답 | ✅ `main` 구현 완료 · PR #27 |
| 제품 추천 | 취향 + 상황 조건 기반 Rule-Based MCM 제품 추천 생성·조회 | ✅ `main` 구현 완료 · PR #29 |
| 구매 전 활용성 | 취향·보유 아이템 등을 이용한 구매 전 활용 가능성 분석 | ⏳ 다음 주요 개발 대상 |
| 마이 아이템 | 등록·목록·검색·필터·상세·수정·삭제 | ⏳ DB 기반 완료 · API 미구현 |
| 이미지·AI 제품 분석 | 사용자 이미지 업로드, AI Job 생성·조회·실패·재시도 | ⏳ 정책/DB 기반 확정 · 기능 미구현 |
| 제품 패스포트 | 제품/구매 정보와 사용 이력을 조합해 조회 | ⏳ 기존 DB 기반 구현 예정 |
| 사용·활용 지원 | 착용 기록, 활용도 분석, 오래 사용하지 않은 제품 안내 | ⏳ 기능 미구현 |
| 스마트 착용 추천 | 보유 제품과 조건을 이용한 착용 추천 생성·저장 | ⏳ 정책/DB 기반 확정 · 기능 미구현 |
| 장소 추천 | Kakao Local 기반 장소 검색·추천·저장 | ⏳ 정책/DB 기반 확정 · 기능 미구현 |
| 사용자·마이페이지·홈 | 사용자 정보와 주요 기능 결과 조합 | ⏳ 기능 미구현 |
| 백엔드 배포 | Spring Boot 운영 서버 배포 | ⏳ Gabia 배포 준비 |

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
| Spring Security/JWT | ✅ 완료 | Stateless JWT Access Token + Refresh Token Cookie |
| GitHub Actions CI | ✅ 완료 | `main` 대상 PR/Push 시 `clean check` |
| H2 테스트 기반 | ✅ 완료 | 빠른 테스트용 In-memory DB |
| Flyway/Testcontainers 기반 | ✅ 완료 | MySQL 8.4 Integration Test |
| 운영 Migration | ✅ 완료 | **V1~V13** |
| local/prod JPA Schema 설정 | ✅ 완료 | `ddl-auto=validate` |
| MCM 제품 카탈로그 | ✅ 완료 | PR #25 |
| 일반 인증 API | ✅ 완료 | PR #24 |
| 제품 찜 API | ✅ 완료 | PR #27 |
| 취향 프로필 API | ✅ 완료 | PR #28 |
| 제품 추천 API | ✅ 완료 | PR #29 |
| 제품 추천 DB 검증 | ✅ 완료 | V7 JSON snapshot round-trip + Hibernate schema validation |
| 소셜 로그인 | 🔄 진행/대기 | 별도 브랜치 구현 존재 · 최신 `main` 재반영 필요 |
| 구매 전 활용성 | ⏳ 다음 단계 | Rule-Based 점수 + 개인화 설명 방향 |
| 외부 서비스 연동 | ⏳ 대기 | 사용자 이미지 Cloudinary, OpenAI, Resend, Kakao Local 등 |
| 개발용 백엔드 배포 | ⏳ 대기 | Gabia 서버·MySQL·HTTPS/CORS 구성 |
| FE 원격 연동 | ⏳ 대기 | 운영 API 주소 확정 후 진행 |

### 현재 위치

```text
공통 기반 구축
    ↓
API 공통 규칙 / ERD / 정책 확정
    ↓
Flyway + Testcontainers 기반 구축
    ↓
V1~V13 운영 DB 스키마 구현·검증
    ↓
MCM 제품 카탈로그
    ↓
일반 인증
    ↓
제품 찜
    ↓
취향 프로필
    ↓
제품 추천
    ↓
현재
├─ 구매 전 활용 가능성 개발 준비
├─ 소셜 로그인 브랜치 최신 main 반영/통합 대기
└─ Gabia 배포 환경 구성 준비
```

---

## 현재 구현된 도메인

현재 `main`의 주요 package는 다음과 같습니다.

```text
auth/
common/
preference/
product/
recommendation/
wishlist/
```

### 1. 공통 백엔드 기반

- Java 21
- Spring Boot 4.1.0
- Gradle Wrapper 9.5.1
- Spring Web MVC
- Spring Data JPA
- Spring Security
- OAuth2 Resource Server JWT
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
        "field": "fieldName",
        "reason": "잘못된 입력값입니다."
      }
    ]
  }
}
```

### 3. 인증 방식

현재 `main`은 **JWT Stateless 인증**을 사용합니다.

```text
Access Token
- Authorization: Bearer <token>
- JWT
- 기본 TTL: 30분

Refresh Token
- HttpOnly Cookie
- 기본 TTL: 14일
- Cookie name: refresh_token
- Path: /api/auth
- SameSite: Lax
- 운영 환경 Secure=true
```

인증·Health·Swagger 일부 경로를 제외한 API는 기본적으로 인증이 필요합니다.

### 4. 날짜·시간 정책

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

## 일반 인증

PR #24를 통해 일반 인증 기능이 `main`에 반영되었습니다.

### 구현 API

| Method | Endpoint | 설명 | 인증 |
| --- | --- | --- | --- |
| POST | `/api/auth/email-verifications` | 이메일 인증 코드 요청 | 불필요 |
| POST | `/api/auth/email-verifications/confirm` | 이메일 인증 코드 확인 | 불필요 |
| GET | `/api/auth/login-ids/{loginId}/availability` | 로그인 ID 사용 가능 여부 | 불필요 |
| POST | `/api/auth/signup` | 회원가입 | 불필요 |
| POST | `/api/auth/login` | 로그인 | 불필요 |
| POST | `/api/auth/refresh` | Access Token 재발급 | Refresh Cookie |
| POST | `/api/auth/logout` | 로그아웃 | 필요 |

### 현재 인증 정책 요약

- Spring Security Session은 사용하지 않습니다.
- Access Token은 JWT로 검증합니다.
- Refresh Token은 HttpOnly Cookie를 사용합니다.
- 비밀번호는 BCrypt를 사용합니다.
- 인증이 필요한 API는 JWT subject를 현재 사용자 ID로 사용합니다.
- 로컬 개발 환경에서는 이메일 인증 코드를 로그로 확인할 수 있습니다.
- 실제 메일 발송 서비스 연동은 별도 후속 작업입니다.
- Kakao·Naver 소셜 로그인은 현재 별도 브랜치에 있으며 `main`에는 아직 포함되지 않았습니다.

---

## MCM 제품 카탈로그

PR #25를 통해 제품 카탈로그 기능이 `main`에 반영되었습니다.

### 제품 목록

```http
GET /api/products
Authorization: Bearer <accessToken>
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

허용 정렬 필드:

```text
createdAt
name
price
```

제품 목록 응답에는 현재 로그인 사용자의 `favorited` 상태가 함께 포함됩니다.

### 제품 상세

```http
GET /api/products/{productId}
Authorization: Bearer <accessToken>
```

존재하지 않거나 비활성화된 제품은 `PRODUCT_NOT_FOUND`로 처리합니다.

제품 상세 응답에도 현재 로그인 사용자의 `favorited` 상태가 포함됩니다.

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

제품 카테고리:

```text
BAG
LEATHER_GOODS
FASHION_ACCESSORY
CLOTHING
SHOES
```

대표 색상 그룹:

```text
BLACK
WHITE
GRAY
BROWN
BEIGE
RED
ORANGE
YELLOW
GREEN
BLUE
PURPLE
PINK
METALLIC
MULTI
OTHER
```

제품 데이터에는 Cloudinary에 업로드된 WebP 이미지 URL과 `publicId`가 반영되어 있습니다.

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

Validator와 Importer는 다음을 검증합니다.

- 전체 상품 60개
- WOMEN/MEN 각 30개
- SKU 중복 없음
- `brand=MCM`
- `status=ACTIVE`
- 상품당 대표 이미지 1개
- STYLE / SEASON / OCCASION 필수 태그
- DB 기준 ProductTag 존재 여부
- SKU 기준 upsert
- 이미지·태그 매핑 교체
- 동일 JSON 재Import 시 상품 수가 증가하지 않는 멱등성

---

## 제품 찜

PR #27을 통해 제품 찜 기능이 `main`에 반영되었습니다.

### 찜 등록

```http
PUT /api/products/{productId}/favorite
Authorization: Bearer <accessToken>
```

- 정상 처리: `204 No Content`
- 이미 찜한 제품을 다시 요청해도 동일하게 성공합니다.

### 찜 해제

```http
DELETE /api/products/{productId}/favorite
Authorization: Bearer <accessToken>
```

- 정상 처리: `204 No Content`
- 이미 해제된 제품을 다시 요청해도 동일하게 성공합니다.

### 찜 목록

```http
GET /api/wishlists?page=0&size=20&sort=createdAt,desc
Authorization: Bearer <accessToken>
```

- 기본 정렬: `createdAt,desc`
- 현재 허용 정렬 필드: `createdAt`
- 사용자별 찜 상태를 독립적으로 관리합니다.
- `ACTIVE` 제품만 유효한 찜 대상으로 다룹니다.

---

## 취향 프로필

PR #28을 통해 취향 프로필 기능이 `main`에 반영되었습니다.

### 조회

```http
GET /api/preferences
Authorization: Bearer <accessToken>
```

- 취향 프로필이 있으면 저장된 값을 반환합니다.
- 아직 취향 프로필이 없으면 정상 응답에서 `data: null`을 반환합니다.

### 저장

```http
PUT /api/preferences
Authorization: Bearer <accessToken>
Content-Type: application/json
```

예시:

```json
{
  "preferredColors": ["BLACK", "BEIGE"],
  "preferredCategories": ["BAG", "LEATHER_GOODS"],
  "preferredStyleTags": ["CASUAL", "NEAT"]
}
```

입력 규칙:

```text
preferredColors
- 필수
- 1~3개

preferredCategories
- 필수
- 1~3개

preferredStyleTags
- 필수
- 1~2개
- CASUAL / FORMAL / NEAT / GLAMOROUS
```

현재 저장 방식은 **전체 교체 방식**입니다.

입력 Enum은 API 정책에 따라 정확한 대문자 값을 사용합니다.

---

## MCM 제품 추천

PR #29를 통해 제품 추천 기능이 `main`에 반영되었습니다.

제품 추천은 OpenAI가 점수를 계산하는 방식이 아니라 **백엔드 Rule-Based 계산**을 사용합니다.

### 추천 생성

```http
POST /api/recommendations
Authorization: Bearer <accessToken>
Content-Type: application/json
```

예시:

```json
{
  "occasion": "DATE",
  "season": "AUTUMN",
  "preferredFeatures": ["COMPACT", "MULTIWAY"],
  "category": "BAG",
  "limit": 3
}
```

Request:

| 필드 | 필수 | 규칙 |
| --- | --- | --- |
| `occasion` | 필수 | `DAILY`, `DATE`, `TRAVEL`, `GATHERING`, `CEREMONY`, `OUTDOOR`, `OTHER` |
| `season` | 필수 | `SPRING`, `SUMMER`, `AUTUMN`, `WINTER` |
| `preferredFeatures` | 필수 | `COMPACT`, `SPACIOUS`, `MULTIWAY` 중 1~3개 |
| `category` | 선택 | 현재 `ItemCategory` 값 |
| `limit` | 선택 | 1~3, 기본 3 |

`ALL_SEASON`은 상품 태그에는 존재하지만 추천 요청의 `season` 입력값으로는 사용하지 않습니다.

STYLE 조건은 요청에서 받지 않고 현재 사용자의 `PreferenceProfile.preferredStyleTags`를 사용합니다.

취향 프로필이 없으면:

```text
409 PREFERENCE_REQUIRED
```

를 반환합니다.

### 추천 점수

최대 100점:

```text
STYLE      30
OCCASION   25
SEASON     25
FEATURE    20
```

세부 규칙:

- STYLE: 사용자의 선호 STYLE 중 하나라도 제품 STYLE과 일치하면 30점
- OCCASION: 요청 OCCASION과 제품 태그가 일치하면 25점
- SEASON: 요청 SEASON과 일치하거나 제품이 `ALL_SEASON`이면 25점
- FEATURE: 요청한 기능 중 일치한 기능의 비율만큼 20점을 배분
- 최종 점수 0점 상품은 제외
- 정렬: `score DESC → productId ASC`
- 최종 점수는 소수 둘째 자리까지 `HALF_UP`

후보 제품:

```text
brand = MCM
status = ACTIVE
category = 요청값이 있을 경우 해당 카테고리
```

### 추천 저장 정책

추천 결과는 기존 V7 테이블을 사용합니다.

```text
recommendations
recommendation_products
```

생성 시점의 다음 정보를 JSON snapshot으로 저장합니다.

```text
추천 조건
취향 STYLE
score policy version
제품 표시 정보
ProductTag
score breakdown
추천 이유
```

따라서 과거 추천 조회 시 제품명·가격·태그 등이 이후 변경되더라도 **생성 당시 추천 결과를 재계산하지 않습니다.**

단, `favorited`는 snapshot에 고정하지 않고 현재 로그인 사용자의 Wishlist 상태를 조회해 응답합니다.

추천 결과가 0개여도 오류가 아니라 Recommendation parent를 저장하고:

```json
{
  "products": []
}
```

형태의 정상 결과를 반환합니다.

### 추천 상세 조회

```http
GET /api/recommendations/{recommendationId}
Authorization: Bearer <accessToken>
```

- 본인이 생성한 추천 결과만 조회할 수 있습니다.
- 존재하지 않는 ID와 다른 사용자의 추천 ID는 동일하게 `RECOMMENDATION_NOT_FOUND`로 처리합니다.
- 저장된 snapshot 기준으로 조회하며 추천을 다시 계산하지 않습니다.

---

## 데이터베이스

DB Schema 변경은 **Flyway Migration**으로 관리합니다.

### 현재 Migration

| Version | 내용 |
| --- | --- |
| V1 | 사용자·인증 관련 테이블 |
| V2 | AI Job |
| V3 | MCM 제품 카탈로그 |
| V4 | 취향 프로필·찜 |
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
- DB 변경은 Migration과 Integration Test를 함께 검토합니다.

### JPA Schema 정책

local/prod 모두:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

를 사용합니다.

즉 Schema 변경의 단일 기준은 Flyway이며, Hibernate는 Entity와 실제 DB Schema의 정합성을 검증합니다.

---

## 테스트 전략

### 빠른 테스트

```text
H2 In-memory
일반 단위/슬라이스/애플리케이션 테스트
integration Tag 제외
```

실행:

```powershell
.\gradlew.bat test
```

### DB Integration Test

```text
Testcontainers MySQL 8.4
Flyway ON
운영 Migration V1~V13 적용
FK / CHECK / UNIQUE / JSON / Schema 정합성 검증
실제 MCM 카탈로그 Import 검증
Recommendation JSON snapshot round-trip 검증
```

실행:

```powershell
.\gradlew.bat integrationTest
```

### 전체 검증

`check`는 `integrationTest`에 의존합니다.

```powershell
.\gradlew.bat clean check --no-daemon
```

**`integrationTest`와 `check`를 실행하려면 Docker가 실행 중이어야 합니다.**

최근 제품 추천 PR #29는 다음 검증을 통과했습니다.

```text
gradlew test
gradlew integrationTest
gradlew check
git diff --check
PR Backend CI #33
main merge 후 Backend CI #34
```

---

## 주요 확정 정책

### 추천 계산

- MCM 제품 추천 점수·순위는 **백엔드 Rule-Based**로 계산합니다.
- 구매 전 활용성 점수도 **백엔드 Rule-Based**를 기준으로 설계합니다.
- OpenAI는 필요한 경우 개인화 설명 생성에 사용하며 핵심 점수 계산 자체를 담당하지 않습니다.
- 장소 추천은 Kakao Local 검색 결과를 기반으로 백엔드에서 후처리·순위 계산하는 방향입니다.

### 마이 아이템·제품 패스포트

- 사용자용 제품 상태 기능은 제거했습니다.
- 관리 기록 이력은 MVP에서 제거했습니다.
- 다음 관리 예정일은 `user_items.next_care_date`로 관리합니다.
- 제품 패스포트는 제품 정보, 구매 정보, 착용 이력을 조합해 제공합니다.
- 활용도와 오래 사용하지 않은 제품 여부는 착용 기록을 이용해 계산합니다.

### 이미지·AI Job

- MCM 카탈로그 상품 이미지는 Cloudinary URL 반영이 완료되었습니다.
- 사용자 업로드 이미지 저장은 Cloudinary 연동 예정입니다.
- ITEM 이미지는 현재 정책상 아이템당 최대 3장을 기준으로 합니다.
- 이미지 업로드 실패 시 마이 아이템 저장 자체는 성공할 수 있으며 같은 `myItemId`로 재시도할 수 있습니다.
- AI Job 상태는 `PENDING → PROCESSING → SUCCEEDED / FAILED`입니다.
- `FAILED`는 polling API에서 정상적인 작업 결과 상태로 다루며 실제 요청/시스템 오류와 구분합니다.
- 프론트엔드 polling은 2초 간격, 최대 약 30초를 기준으로 합니다.

### 장소 추천

- 장소 원본 데이터: Kakao Local REST API
- 화면 지도: OpenFreeMap
- 사용자 위치에 따른 거리는 요청 시 계산하며 DB에 고정하지 않습니다.

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
| Security | Spring Security | Stateless |
| Authentication | OAuth2 Resource Server JWT | Access Token |
| Refresh | HttpOnly Cookie | Refresh Token |
| Persistence | Spring Data JPA, Hibernate | MySQL |
| Migration | Flyway | V1~V13 |
| Validation | Jakarta Validation | 요청값 검증 |
| Fast Test DB | H2 | In-memory |
| DB Integration Test | Testcontainers MySQL | MySQL 8.4 |
| API Docs | springdoc-openapi | 3.1.0 |
| CI | GitHub Actions | PR/main `clean check` |
| Backend Deployment | Gabia | 예정 |
| Product Image | Cloudinary | MCM 카탈로그 URL 반영 완료 |
| User Image | Cloudinary | 연동 예정 |
| AI | OpenAI | 연동 예정 |
| Email | 개발용 Logging Sender | 실제 발송 서비스 연동 예정 |
| Social Login | Kakao / Naver | 별도 브랜치 구현 · main 미병합 |
| Place Search | Kakao Local REST API | 연동 예정 |
| Map Rendering | OpenFreeMap | 프론트엔드 사용 예정 |

---

## 현재 프로젝트 구조

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
   │  │  ├─ auth/
   │  │  │  ├─ config/
   │  │  │  ├─ controller/
   │  │  │  ├─ domain/
   │  │  │  ├─ dto/
   │  │  │  ├─ repository/
   │  │  │  ├─ security/
   │  │  │  ├─ service/
   │  │  │  └─ support/
   │  │  ├─ common/
   │  │  ├─ preference/
   │  │  ├─ product/
   │  │  ├─ recommendation/
   │  │  ├─ wishlist/
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
      │  ├─ auth/
      │  ├─ common/
      │  ├─ preference/
      │  ├─ product/
      │  ├─ recommendation/
      │  └─ wishlist/
      └─ resources/
         └─ application-test.properties
```

도메인 기능은 **package-by-domain** 구조를 사용합니다.

---

## API 공통 규칙

API 계약의 기본 기준은 [`API_CONVENTIONS.md`](./API_CONVENTIONS.md)입니다.

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
| ID | API에서 문자열 표현을 기본으로 함 |
| 금액 | KRW 원 단위 정수 |
| 페이지 번호 | `page=0`부터 시작 |
| 페이지 크기 | `1~100` |
| 다중 Query | 동일 Parameter 이름 반복 |

---

## 현재 구현 API 요약

### 인증 없이 접근 가능한 API

| Method | Endpoint |
| --- | --- |
| GET | `/api/health` |
| POST | `/api/auth/email-verifications` |
| POST | `/api/auth/email-verifications/confirm` |
| GET | `/api/auth/login-ids/{loginId}/availability` |
| POST | `/api/auth/signup` |
| POST | `/api/auth/login` |
| POST | `/api/auth/refresh` |
| GET | `/swagger-ui/**` |
| GET | `/v3/api-docs/**` |

### 인증이 필요한 주요 API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| POST | `/api/auth/logout` | 로그아웃 |
| GET | `/api/products` | MCM 제품 목록 |
| GET | `/api/products/{productId}` | MCM 제품 상세 |
| PUT | `/api/products/{productId}/favorite` | 찜 등록 |
| DELETE | `/api/products/{productId}/favorite` | 찜 해제 |
| GET | `/api/wishlists` | 찜 목록 |
| GET | `/api/preferences` | 취향 프로필 조회 |
| PUT | `/api/preferences` | 취향 프로필 저장 |
| POST | `/api/recommendations` | MCM 제품 추천 생성 |
| GET | `/api/recommendations/{recommendationId}` | 추천 상세 조회 |

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

## 배포 계획 — Gabia

현재 백엔드 운영 배포 대상은 **Gabia**를 기준으로 준비 중입니다.

```text
Client / Frontend
        ↓
Gabia Spring Boot Backend
        ↓
MySQL
```

아직 최종 확정이 필요한 항목:

```text
Gabia 서버 상품
운영 MySQL 배치 방식
운영 도메인
HTTPS / Reverse Proxy
CORS / Trusted Origin
Frontend → Backend API 연결 방식
```

현재 `application-prod.properties`는 다음 형식의 MySQL 환경변수를 사용합니다.

```env
MYSQLHOST
MYSQLPORT
MYSQLDATABASE
MYSQLUSER
MYSQLPASSWORD
```

이 이름들은 현재 애플리케이션 설정 계약이며, Gabia가 자동 제공하는 변수라는 뜻은 아닙니다.

### 문서 정합성 주의

현재 `.env.example`의 Production 주석에는 과거 Railway 기준 표현이 남아 있습니다.

README 기준 배포 계획은 Gabia이므로, 실제 배포 환경을 확정할 때 `.env.example`의 해당 주석과 관련 운영 문서도 함께 정리해야 합니다.

---

## 실행 환경

### 로컬 개발

```env
SPRING_PROFILES_ACTIVE=local

DB_USERNAME=hackathon
DB_PASSWORD=change-me

CORS_ALLOWED_ORIGIN=http://localhost:3000

JWT_ISSUER=hackathon-be
JWT_SECRET=replace-with-at-least-32-random-characters

REFRESH_COOKIE_SECURE=false
AUTH_LOG_VERIFICATION_CODE=true

APP_PRODUCT_IMPORT_ENABLED=false
```

### 운영 환경 예시

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
JWT_SECRET=replace-with-secure-production-secret

APP_PRODUCT_IMPORT_ENABLED=false
```

실제 비밀번호·Token·Secret은 Git에 저장하지 않습니다.

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
$env:JWT_SECRET="replace-with-at-least-32-random-characters"
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

최신 제품 추천 PR #29의 PR CI와 `main` merge 후 Backend CI #34까지 성공했습니다.

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
→ CI / 검토
→ Squash and merge
→ main 최신화
→ 작업 브랜치 삭제
```

작업 시작 예시:

```bash
git switch main
git fetch origin
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
- [ ] PR CI 성공 후 병합합니다.

---

## 다음 개발 흐름

현재 `main`까지 완료된 주요 기능:

```text
공통 기반
→ Flyway/Testcontainers
→ V1~V13
→ MCM 제품 카탈로그
→ 일반 인증
→ 제품 찜
→ 취향 프로필
→ MCM 제품 추천
```

다음 우선 흐름:

```text
구매 전 활용 가능성
        ↓
마이 아이템
        ↓
사용자 이미지 업로드 + AI 제품 분석
        ↓
제품 패스포트
        ↓
착용 기록 / 활용도 분석
        ↓
스마트 착용 추천
        ↓
장소 추천
        ↓
마이페이지 / 홈 조합
```

소셜 로그인과 배포 작업은 위 기능 개발과 병렬로 진행할 수 있습니다.

### 배포 준비 흐름

```text
Gabia 서버 환경 확정
        ↓
Java 21 / 운영 환경 구성
        ↓
MySQL 연결
        ↓
운영 Secret 주입
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

---

## 관련 문서

- [API 공통 규칙](./API_CONVENTIONS.md)
- [환경변수 예시](./.env.example)
- [백엔드 저장소](https://github.com/pro660/Hackathon_BE)
