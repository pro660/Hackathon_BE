# 입을래? Backend

2026 중앙해커톤 서비스 **입을래?**의 Spring Boot 백엔드 저장소입니다.

> **문서 기준일:** 2026-08-19
> **현재 구현 기준:** Backend `main` `3599d95` / PR #50 반영
> **API 공통 규칙:** [`API_CONVENTIONS.md`](./API_CONVENTIONS.md)
> **운영 목표 구조:** Frontend Vercel / Backend Gabia Cloud / Database Gabia MySQL
>
> README는 현재 구현 상태와 실행·테스트·연동 방법을 빠르게 파악하기 위한 요약 문서입니다.
> 세부 API 계약이 README와 다를 경우 **현재 `main`의 Controller·DTO·설정·Flyway와 `API_CONVENTIONS.md`**를 우선 확인합니다.

---

## 1. 프로젝트 소개

**입을래?**는 명품을 구매한 뒤 충분히 활용하지 못하는 문제를 줄이기 위해,
사용자의 **취향·보유 아이템·상황**을 기반으로 제품의 활용 가능성을 분석하고
구매부터 착용, 관리, 장소 추천까지 이어지는 활용 경험을 제공하는 서비스입니다.

백엔드는 현재 다음 흐름을 지원합니다.

1. 일반 회원가입·로그인 및 Kakao/Naver 소셜 로그인
2. 사용자 취향 프로필 저장
3. MCM 제품 카탈로그 조회·검색 조건 적용
4. 제품 찜 및 구매 후보 장바구니 관리
5. 취향·상황 기반 MCM 제품 추천
6. 마이 아이템 등록·조회·수정·삭제 및 이미지 관리
7. 이미지 기반 `ITEM_ANALYSIS`
8. 구매 전 활용 가능성 분석 및 AI 설명 생성
9. 제품 패스포트 조회
10. 스마트 착용 추천(`STYLE_PLAN`) 생성 및 스타일 플랜 저장
11. 스타일 플랜 기반 장소 검색·추천·저장
12. 맞춤 관리 가이드·캘린더·관리 알림
13. 홈 화면용 집계 조회

---

## 2. 현재 구현 상태

| 영역 | 상태 | 현재 구현 |
| --- | --- | --- |
| 공통 응답·예외 | ✅ 구현 | 공통 성공/오류 Wrapper, Validation·Business Exception 처리 |
| 시간 정책 | ✅ 구현 | 서버/JPA/Jackson UTC, `Instant` 중심 정확한 시각 처리 |
| 일반 인증 | ✅ 구현 | 이메일 인증, 회원가입, 로그인, 토큰 갱신, 로그아웃 |
| 소셜 로그인 | ✅ 구현 | Kakao / Naver OAuth |
| 재인증·계정 관리 | ✅ 구현 | 비밀번호·OAuth 재인증, 마이페이지, 회원 탈퇴 |
| 취향 프로필 | ✅ 구현 | 조회 및 전체 교체 저장, 낙관적 락 |
| MCM 제품 카탈로그 | ✅ 구현 | 목록·상세, 카테고리·색상·가격 필터, 정렬, 페이지네이션 |
| MCM 제품 데이터 | ✅ 구현 | MCM 60개 샘플 데이터 + Importer/Validator |
| 제품 찜 | ✅ 구현 | 찜 등록·해제·목록, 제품 응답 `favorited` |
| 구매 후보 장바구니 | ✅ 구현 | 담기·제거·목록 조회, 제품 상세 `inCart` |
| 제품 추천 | ✅ 구현 | 취향·상황 기반 Rule-Based 추천 생성·조회 |
| 마이 아이템 | ✅ 구현 | CRUD, 검색·필터·정렬, Soft Delete |
| ImageAsset | ✅ 구현 | JPEG/PNG 업로드, Cloudinary, 연결·교체·삭제·cleanup |
| `ITEM_ANALYSIS` | ✅ 구현 | 이미지 기반 브랜드/이름/카테고리/색상/소재 분석 |
| AI provenance | ✅ 구현 | 분석 Job·입력 이미지·마이 아이템 정합성 검증 |
| 구매 전 활용 가능성 | ✅ 구현 | Rule-Based 점수, 요인, 호환 아이템, 관리 난이도 |
| 구매 활용성 AI 설명 | ✅ 구현 | OpenAI Responses API, Structured Output, 캐시·fallback |
| 제품 패스포트 | ✅ 구현 | 마이 아이템 제품 정보·구매 정보 read model |
| 스마트 착용 추천 | ✅ 구현 | `STYLE_PLAN` AI 생성 + Rule-Based fallback |
| 스타일 플랜 저장 | ✅ 구현 | 생성·목록·상세·수정·삭제, generation type·version 관리 |
| 장소 검색 | ✅ 구현 | Kakao Local 기반 검색 |
| 장소 추천 | ✅ 구현 | StylePlan occasion + 거리/카테고리 Rule-Based 추천 |
| 장소 저장 | ✅ 구현 | 사용자 저장 장소 관리 |
| 맞춤 관리 | ✅ 구현 | 관리 가이드·보관법·월별 관리 캘린더 |
| 관리 알림 | ✅ 구현 | 아이템별 알림 설정, `CARE_REMINDER`, 알림 조회·읽음 처리 |
| 홈 집계 API | ✅ 구현 | 추천·스타일·패스포트·장소 데이터를 홈 화면용으로 집계 |
| 착용/사용 기록 API | ⏳ 미구현 | DB 초기 구조와 별개로 현재 별도 애플리케이션 API는 구현되지 않음 |
| 운영 배포 | 🚧 연동 단계 | Gabia 기반 운영 환경 구성 및 FE 연동 대상 |

> DB 테이블이 존재하는 것과 애플리케이션 API가 구현된 것은 구분합니다.
> README의 구현 상태는 현재 `main`의 Controller/Service 기준으로 작성합니다.

---

## 3. 최근 주요 반영 내역

### PR #42 — 제품 패스포트 조회 및 구매 메타데이터

- `GET /api/my-items/{myItemId}/passport`
- 구매일·구매가격에 구매처·주문번호 메타데이터 추가
- UserItem 기반 Passport read model
- Flyway V17 반영

### PR #43 — 스마트 착용 추천

- 공통 AI Job의 `STYLE_PLAN` 실제 처리 연결
- 사용자의 보유 아이템과 MCM 제품을 조합한 착용 추천 생성
- OpenAI Responses API + Structured Output
- AI 사용 불가/실패 시 deterministic Rule-Based fallback
- `generationType: AI | RULE_BASED`

### PR #44 — 스타일 플랜 CRUD

- `POST /api/style-plans`
- `GET /api/style-plans`
- `GET /api/style-plans/{stylePlanId}`
- `PATCH /api/style-plans/{stylePlanId}`
- `DELETE /api/style-plans/{stylePlanId}`
- AI Job 결과 저장 시 조합 재검증
- `version` 기반 optimistic locking

### PR #45 — 맞춤 관리 가이드·캘린더 및 관리 알림

- 소재별 관리·보관 가이드
- 구매일 기반 월별 관리 캘린더
- 아이템별 관리 알림 설정
- `CARE_REMINDER` 서비스 내부 알림
- 알림 목록·읽음 상태 변경
- Flyway V18 반영

### PR #46 — 장소 검색·추천·저장

- Kakao Local 기반 장소 검색
- StylePlan 기반 장소 추천
- 카테고리 적합도 + 거리 적합도 Rule-Based 순위화
- 사용자 저장 장소 관리
- 추천 과정 자체에서는 OpenAI를 호출하지 않음

### PR #47 — MCM 제품 확인 장바구니

- `PUT /api/products/{productId}/cart`
- `DELETE /api/products/{productId}/cart`
- `GET /api/cart-items`
- 사용자·제품 단위 UNIQUE 관계와 멱등 담기
- 제품 상세 응답에 `inCart`
- Flyway V19 반영

### PR #48 — 홈 집계 조회 API

- `GET /api/home`
- 홈 화면에서 필요한 저장 데이터를 한 번에 조회하는 Read Model
- 조회 시 새 AI Job/OpenAI/Kakao 검색을 숨은 부작용으로 실행하지 않음
- Flyway V20 조회 인덱스 반영

### PR #49 · #50 — API 문서 최신화

- Stateless JWT / Cookie / Trusted Origin 계약 현행화
- 공통 AI Job·Idempotency·polling 계약 현행화
- STYLE_PLAN 실제 허용값 반영
- 이미지 multipart MIME 계약 보강
- AI Job 타입별 결과 구조 보강
- `.env.example`에 OpenAI 환경변수 추가
- 과거 Railway 중심 문구 제거

---

## 4. 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Build | Gradle Wrapper 9.5.1 |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA + 일부 JDBC Repository |
| Database | MySQL |
| Authentication | Spring Security + OAuth2 Resource Server + JWT |
| OAuth | Kakao / Naver |
| Validation | Jakarta Validation |
| DB Migration | Flyway |
| API Docs | springdoc-openapi 3.1.0 / Swagger UI |
| AI | OpenAI Responses API |
| Image Storage | Cloudinary HTTP5 2.4.0 |
| Place Provider | Kakao Local API |
| Test | JUnit 5, Spring Test, H2, Testcontainers 2.0.5 |
| CI | GitHub Actions |

### 주요 설계 원칙

- 모든 백엔드 API는 `/api` prefix 사용
- 보호 API는 `Authorization: Bearer <accessToken>` 사용
- Refresh Token은 서버 관리 `HttpOnly` Cookie 사용
- 서버 Session에 로그인 상태를 저장하지 않는 Stateless JWT 구조
- JPA `ddl-auto=validate`
- DB 구조 변경은 기존 Migration 수정이 아니라 새 Flyway Migration 추가
- 서버/JPA/Jackson의 정확한 시각 기준은 UTC
- 공통 성공·오류 응답 구조 사용
- AI 기능마다 Job 시스템을 새로 만들지 않고 공통 `/api/ai-jobs` 재사용
- AI가 실패해도 가능한 기능은 Rule-Based fallback 유지
- 외부 Provider의 Secret/API Key는 저장소에 커밋하지 않음

---

## 5. 운영 목표 아키텍처

```text
Browser
  │
  │  /api/**
  ▼
Vercel Frontend
  │
  │  rewrite / proxy
  ▼
Gabia Cloud
Spring Boot Backend
  │
  ├── Gabia MySQL
  ├── OpenAI Responses API
  ├── Cloudinary
  ├── Kakao Local API
  └── Kakao / Naver OAuth
```

운영 브라우저는 프론트 Origin의 `/api/**`를 호출하고,
프론트 rewrite/proxy를 통해 백엔드에 전달하는 구조를 기본으로 합니다.

백엔드 코드는 특정 프론트 배포 URL을 하드코딩하지 않고
`CORS_ALLOWED_ORIGIN`, OAuth redirect 관련 환경변수 등으로 환경별 값을 주입합니다.

---

## 6. 주요 패키지 구조

```text
src/main/java/org/likelionhsu/hackathon/
├─ aijob/             # 공통 AI Job 생성·조회·상태·멱등성
├─ auth/              # 일반 인증, JWT, OAuth, 재인증
├─ careguide/         # 관리 가이드·보관법·캘린더·알림 설정
├─ cart/              # MCM 구매 후보 장바구니
├─ common/            # 공통 설정, 응답, 예외, enum, health
├─ home/              # 홈 화면 집계 Read Model
├─ imageasset/        # 이미지 업로드, Cloudinary, lifecycle/cleanup
├─ itemanalysis/      # ITEM_ANALYSIS 및 OpenAI 이미지 분석
├─ notification/      # 서비스 내부 알림
├─ place/             # Kakao Local 검색·장소 추천·저장
├─ preference/        # 사용자 취향 프로필
├─ product/           # MCM 제품 카탈로그 및 import
├─ purchaseutility/   # 구매 전 활용 가능성 + AI 설명
├─ recommendation/    # MCM 제품 추천
├─ styleplan/         # STYLE_PLAN AI + 스타일 플랜 저장
├─ user/              # 마이페이지·회원 탈퇴
├─ useritem/          # 마이 아이템·제품 패스포트·이미지 연계
└─ wishlist/          # 제품 찜
```

---

## 7. 주요 API 그룹

세부 Request/Response, Validation, Enum 값은 Swagger와
[`API_CONVENTIONS.md`](./API_CONVENTIONS.md)를 기준으로 확인합니다.

| 영역 | 주요 Endpoint |
| --- | --- |
| Health | `GET /api/health` |
| Auth | `/api/auth/**` |
| User | `/api/users/me/**` |
| Preferences | `GET/PUT /api/preferences` |
| Products | `/api/products/**` |
| Wishlist | `GET /api/wishlists`, `PUT/DELETE /api/products/{productId}/favorite` |
| Cart | `GET /api/cart-items`, `PUT/DELETE /api/products/{productId}/cart` |
| Recommendations | `POST /api/recommendations`, `GET /api/recommendations/{recommendationId}` |
| My Items | `/api/my-items/**` |
| Product Passport | `GET /api/my-items/{myItemId}/passport` |
| Care | `/api/my-items/{myItemId}/care-guide`, `/storage-guide`, `/care-calendar`, `/care-reminder-setting` |
| ImageAsset | `/api/image-assets/**` |
| AI Jobs | `POST /api/ai-jobs`, `GET /api/ai-jobs/{jobId}` |
| Purchase Utility | `GET /api/purchase-utility-analyses/{analysisId}` |
| Style Plans | `/api/style-plans/**` |
| Place Search/Save | `/api/places/**` |
| StylePlan Place Recommendation | `POST /api/style-plans/{stylePlanId}/place-recommendations` |
| Notifications | `/api/notifications/**` |
| Home | `GET /api/home` |

### Swagger / OpenAPI

로컬 서버 실행 후:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## 8. 공통 AI Job

AI 기능은 공통 비동기 Job API를 사용합니다.

```http
POST /api/ai-jobs
Authorization: Bearer <accessToken>
Idempotency-Key: <required>
Content-Type: application/json
```

현재 지원 타입:

```text
PURCHASE_UTILITY
ITEM_ANALYSIS
STYLE_PLAN
```

상태:

```text
PENDING
PROCESSING
SUCCEEDED
FAILED
```

기본 흐름:

```text
POST /api/ai-jobs
        │
        ▼
      jobId
        │
        ▼
GET /api/ai-jobs/{jobId}
        │
        ├── PENDING / PROCESSING → polling
        │
        ├── SUCCEEDED → result 사용
        └── FAILED → error와 기능별 fallback 확인
```

### OpenAI 설정

현재 세 AI 기능은 동일한 환경변수를 사용합니다.

```env
OPENAI_API_KEY=change-me
OPENAI_MODEL=gpt-5.6-luna
```

`OPENAI_API_KEY`가 없는 환경에서는 OpenAI Adapter가 활성화되지 않으며,
기능별 정책에 따라 Rule-Based 결과 또는 실패 상태를 사용합니다.

실제 API Key는 `.env.example`, README, GitHub Issue/PR 등에 기록하지 않습니다.

---

## 9. 이미지 업로드 / ITEM_ANALYSIS

### 이미지 업로드

```http
POST /api/image-assets
Content-Type: multipart/form-data
```

현재 주요 계약:

- multipart part 이름: `file`
- JPEG / PNG
- `image/jpeg` 또는 `image/png`
- 실제 binary 형식과 Content-Type 일치 검증
- 최대 10MB
- 인증된 사용자만 사용
- Cloudinary 저장

ImageAsset lifecycle:

```text
TEMPORARY
   │
   ▼
ACTIVE
   │
   ▼
DELETE_PENDING
   │
   ▼
DELETED
```

현재 MVP에서는 UserItem 하나에 ACTIVE ITEM 이미지를 최대 1개 유지합니다.

### ITEM_ANALYSIS 흐름

```text
이미지 업로드
  ↓
ImageAsset(TEMPORARY)
  ↓
POST /api/ai-jobs
type = ITEM_ANALYSIS
  ↓
OpenAI 이미지 분석
  ↓
brandName / name / category / primaryColor / material
  ↓
사용자 확인
  ↓
UserItem 등록
  ↓
최초 이미지 연결 및 provenance 검증
```

구매일·구매가격·구매처·메모처럼 이미지에서 신뢰성 있게 판단할 수 없는 값은
사용자 입력 정보로 관리합니다.

---

## 10. 스마트 착용 추천 / STYLE_PLAN

`STYLE_PLAN`은 보유 아이템과 MCM 제품을 조합해 착용 추천 preview를 생성합니다.

현재 context 핵심 값:

```text
occasion:
DAILY | DATE | TRAVEL | GATHERING | CEREMONY | OUTDOOR | OTHER

styleTags:
CASUAL | FORMAL | NEAT | GLAMOROUS

weatherCondition:
SUNNY | CLOUDY | RAINY | SNOWY | HOT | COLD | WINDY | INDOOR | OTHER

language:
ko
```

AI 생성 성공 시 `generationType=AI`,
AI를 사용할 수 없거나 생성에 실패해 규칙 기반 결과를 사용하는 경우
`generationType=RULE_BASED`로 구분할 수 있습니다.

AI Job 결과는 자동 저장되지 않습니다.
사용자가 preview를 확정한 뒤 `/api/style-plans`로 저장합니다.

---

## 11. 장소 검색·추천

장소 기능은 Kakao Local API를 사용합니다.

주요 특징:

- 장소 검색
- 장소 결과 DB materialization
- 사용자 저장 장소
- StylePlan occasion 기반 추천
- 카테고리 적합도와 거리 적합도를 이용한 Rule-Based ranking
- 추천 자체에는 OpenAI를 사용하지 않음

```http
POST /api/style-plans/{stylePlanId}/place-recommendations
```

현재 추천 결과는 최대 3개이며, 해당 StylePlan의 기존 추천 장소 연결을 교체합니다.

---

## 12. 맞춤 관리와 알림

관리 기능은 `UserItem.material`, `purchaseDate`와
서버의 관리 정책 리소스를 기반으로 계산합니다.

주요 기능:

- 관리 가이드
- 보관법
- 월별 관리 캘린더
- 아이템별 관리 알림 ON/OFF
- 예정일 도래 시 `CARE_REMINDER`
- 서비스 내부 알림 목록 및 읽음 상태 관리

Scheduler는 환경 설정으로 활성화하며 기본 설정값과 운영 설정이 다를 수 있습니다.

---

## 13. Database / Flyway

현재 DB Migration은 **V1 ~ V20**까지 존재합니다.

최근 Migration:

| Version | 내용 |
| --- | --- |
| V16 | UserItem `brandName` nullable 허용 |
| V17 | UserItem 구매 메타데이터 추가 |
| V18 | 관리 알림 설정·Notification 테이블 |
| V19 | Cart Items 테이블 |
| V20 | Home 조회 인덱스 |

원칙:

```text
이미 적용된 V1~V20 수정 금지
        ↓
추가 DB 변경 필요
        ↓
V21+ 신규 Migration 작성
```

로컬/운영 모두 JPA schema 자동 변경이 아니라:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

를 사용합니다.

---

## 14. MCM 제품 샘플 데이터

제품 데이터:

```text
src/main/resources/data/mcm-products.json
```

현재 MCM 샘플 제품 60개를 기준으로 Importer/Validator가 구성되어 있습니다.

Importer는 기본 비활성화입니다.

```env
APP_PRODUCT_IMPORT_ENABLED=false
```

제품 적재가 필요한 환경에서만 명시적으로 활성화합니다.

```env
APP_PRODUCT_IMPORT_ENABLED=true
```

운영 DB에서 활성화 여부를 변경하기 전에 현재 적재 상태와 배포 절차를 확인합니다.

---

## 15. 로컬 실행

### 요구 환경

- Java 21
- MySQL
- Docker Desktop — `integrationTest`, `clean check`의 Testcontainers 실행 시 필요
- Git

### 1) 저장소 준비

```bash
git clone https://github.com/pro660/Hackathon_BE.git
cd Hackathon_BE
```

### 2) 로컬 MySQL 준비

기본 로컬 설정은 다음 값을 사용합니다.

```text
host: localhost
port: 3306
database: hackathon_db
username: hackathon
```

팀원 환경에 따라 환경변수로 변경할 수 있습니다.

### 3) 환경변수 설정

저장소의 `.env.example`을 변수 목록 참고용으로 사용합니다.

최소 로컬 예:

```env
SPRING_PROFILES_ACTIVE=local
DB_USERNAME=hackathon
DB_PASSWORD=<local-db-password>
CORS_ALLOWED_ORIGIN=http://localhost:3000
JWT_SECRET=<local-development-secret>
```

외부 기능을 사용할 경우 필요한 값:

```env
OPENAI_API_KEY=<secret>
OPENAI_MODEL=gpt-5.6-luna

CLOUDINARY_CLOUD_NAME=<value>
CLOUDINARY_API_KEY=<value>
CLOUDINARY_API_SECRET=<secret>

KAKAO_LOCAL_REST_API_KEY=<secret>
```

Kakao/Naver 소셜 로그인을 사용할 경우 해당 OAuth 환경변수도 설정합니다.

> 실제 Secret을 Git에 커밋하지 마세요.

### 4) 실행

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

macOS / Linux:

```bash
./gradlew bootRun
```

기본 포트:

```text
http://localhost:8080
```

---

## 16. 테스트

### 일반 테스트

Windows:

```powershell
.\gradlew.bat test
```

macOS / Linux:

```bash
./gradlew test
```

`test`는 `@Tag("integration")` 테스트를 제외합니다.

### MySQL Testcontainers 통합 테스트

Docker Desktop이 실행 중이어야 합니다.

Windows:

```powershell
.\gradlew.bat integrationTest
```

macOS / Linux:

```bash
./gradlew integrationTest
```

### 전체 검증

```powershell
.\gradlew.bat clean check
```

또는:

```bash
./gradlew clean check
```

현재 Gradle 설정에서 `check`는 `integrationTest`에도 의존하므로 Docker가 필요합니다.

---

## 17. GitHub Actions CI

CI는 다음 시점에 실행됩니다.

- `main` 대상 Pull Request
- `main` push

환경:

```text
Ubuntu
Java 21 Temurin
Gradle
timeout 30 minutes
```

실행 명령:

```bash
./gradlew clean check --no-daemon
```

따라서 PR 전에 로컬에서도 가능한 경우 `clean check`로 전체 회귀를 확인합니다.

---

## 18. 환경변수

정확한 변수 목록은 [`.env.example`](./.env.example)을 참고합니다.

### Local / 공통

```text
SPRING_PROFILES_ACTIVE
DB_USERNAME
DB_PASSWORD
CORS_ALLOWED_ORIGIN
JWT_ISSUER
JWT_SECRET
REFRESH_COOKIE_SECURE
OAUTH_COOKIE_SECURE
REAUTH_COOKIE_SECURE
AUTH_LOG_VERIFICATION_CODE
FRONTEND_OAUTH_SUCCESS_URL
FRONTEND_OAUTH_ONBOARDING_URL
FRONTEND_REAUTHENTICATION_SUCCESS_URL
NAVER_CLIENT_ID
NAVER_CLIENT_SECRET
NAVER_REDIRECT_URI
KAKAO_CLIENT_ID
KAKAO_CLIENT_SECRET
KAKAO_REDIRECT_URI
```

### Production DB

```text
PORT
MYSQLHOST
MYSQLPORT
MYSQLDATABASE
MYSQLUSER
MYSQLPASSWORD
```

운영에서는 반드시:

```env
SPRING_PROFILES_ACTIVE=prod
```

를 활성화합니다.

### Product / Image / Place / AI

```text
APP_PRODUCT_IMPORT_ENABLED

CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET
IMAGE_ASSET_CLEANUP_ENABLED

KAKAO_LOCAL_REST_API_KEY

OPENAI_API_KEY
OPENAI_MODEL
```

실제 운영 비밀값은 Gabia의 배포 환경 또는 별도 Secret 관리 영역에서 주입하며
저장소 파일에 직접 기록하지 않습니다.

---

## 19. 운영 배포 시 확인 사항

현재 프로젝트의 운영 목표는 다음 구성입니다.

```text
Frontend  → Vercel
Backend   → Gabia Cloud
Database  → Gabia MySQL
```

백엔드 운영 환경의 주요 확인 항목:

- Java 21
- `SPRING_PROFILES_ACTIVE=prod`
- Gabia MySQL 접속 정보
- `JWT_SECRET`
- 실제 Vercel Origin을 사용한 `CORS_ALLOWED_ORIGIN`
- OAuth Client ID / Secret / Redirect URI
- OpenAI 환경변수
- Cloudinary 환경변수
- Kakao Local REST API Key
- 외부 Provider로의 HTTPS outbound 통신
- Flyway V1~V20 적용
- JPA `ddl-auto=validate`

운영 배포 주소나 실제 Secret 값은 README에 기록하지 않습니다.

---

## 20. 현재 프로젝트 단계

현재 `main`에는 MVP 백엔드의 주요 도메인 기능이 대부분 구현되어 있습니다.

### 구현 완료 중심 영역

```text
인증
취향
MCM 제품
찜 / 장바구니
제품 추천
마이 아이템
이미지 / ITEM_ANALYSIS
구매 활용 가능성
제품 패스포트
스마트 착용 추천
스타일 플랜
장소 검색·추천·저장
관리 가이드·알림
홈 집계
```

### 이후 통합 단계에서 중점 확인할 영역

```text
Frontend ↔ Backend 실제 API 연동
운영 환경변수 / OAuth Redirect / CORS 검증
Gabia MySQL + Flyway 운영 검증
OpenAI / Cloudinary / Kakao Local 운영 연동
전체 사용자 시나리오 E2E
시연 데이터와 오류/fallback 시나리오 점검
```

착용/사용 기록 및 이를 기반으로 한 별도 활용도 통계 API는
현재 구현 완료 영역으로 표시하지 않습니다.

---

## 21. API 계약 우선순위

API 연동 중 문서와 코드가 일시적으로 다를 경우 다음 순서로 확인합니다.

1. 현재 `main`의 Controller / DTO / Service / 설정
2. 현재 Flyway schema
3. [`API_CONVENTIONS.md`](./API_CONVENTIONS.md)
4. Swagger / OpenAPI
5. README

README는 프로젝트 전체를 빠르게 이해하기 위한 문서이며,
필드 단위 계약은 `API_CONVENTIONS.md`와 실제 구현을 기준으로 판단합니다.

---

## 22. 문서 및 개발 변경 원칙

- 실제 Secret/API Key/Password를 Git에 커밋하지 않습니다.
- 기존 적용 Flyway Migration을 수정하지 않습니다.
- API 계약 변경 시 프론트엔드 영향 여부를 확인합니다.
- AI 기능은 기존 공통 AI Job을 재사용합니다.
- 문서 전용 변경과 기능 코드 변경은 가능한 한 PR 범위를 분리합니다.
- PR 전 `git diff --check`를 확인합니다.
- 기능 변경은 관련 테스트와 필요 시 `clean check`까지 검증합니다.

---

## 23. 관련 문서

- [`API_CONVENTIONS.md`](./API_CONVENTIONS.md) — API 공통 계약 및 프론트 연동 규칙
- [`.env.example`](./.env.example) — 필요한 환경변수 이름과 안전한 예시
- [`build.gradle`](./build.gradle) — 의존성 및 test/integrationTest/check 구성
- [`.github/workflows/ci.yml`](./.github/workflows/ci.yml) — Backend CI
- [`src/main/resources/db/migration`](./src/main/resources/db/migration) — Flyway Migration
- [`src/main/resources/data/mcm-products.json`](./src/main/resources/data/mcm-products.json) — MCM 제품 데이터
