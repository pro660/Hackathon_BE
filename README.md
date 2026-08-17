# 입을래? Backend

2026 중앙해커톤 서비스 **입을래?**의 Spring Boot 백엔드 저장소입니다.

> **문서 기준일:** 2026-08-17
> **현재 구현 기준:** 마이 아이템 ImageAsset + `ITEM_ANALYSIS` OpenAI 이미지 분석 반영
> **API 공통 규칙:** [`API_CONVENTIONS.md`](./API_CONVENTIONS.md)
> **운영 배포 방향:** Gabia 준비 중
> **최종 로컬 검증:** `./gradlew clean check` 성공
>
> README는 현재 구현 상태와 실행·테스트 방법을 빠르게 파악하기 위한 요약 문서입니다.
> 세부 API 계약은 **실제 `main` 코드 → Flyway 스키마 → `API_CONVENTIONS.md` → README** 순으로 확인합니다.

---

## 1. 프로젝트 소개

**입을래?**는 명품을 단순히 구매하는 데서 끝내지 않고,
사용자가 보유한 아이템과 취향을 바탕으로 **구매 전 활용 가능성, 제품 추천, 보유 아이템 관리와 재활용 가치**를 높이는 서비스입니다.

현재 백엔드는 다음 흐름을 중심으로 구현되어 있습니다.

1. 일반 회원가입·로그인 및 Kakao/Naver 소셜 로그인
2. 사용자 취향 프로필 저장
3. MCM 제품 카탈로그 조회·필터·찜
4. 취향과 상황 기반 Rule-Based MCM 제품 추천
5. 마이 아이템 등록·조회·수정·삭제
6. 구매 전 활용 가능성 Rule-Based 분석
7. 공통 AI Job을 통한 비동기 AI 작업 관리
8. OpenAI Responses API를 통한 구매 활용성 AI 설명 생성
9. 마이 아이템 이미지 기반 `ITEM_ANALYSIS` 분석

---

## 2. 현재 구현 상태

| 영역 | 상태 | 현재 `main` 기준 |
| --- | --- | --- |
| 공통 응답·예외·시간 정책 | ✅ 구현 | 공통 응답 래퍼, 예외 처리, UTC/JPA 시간 정책 |
| 일반 인증 | ✅ 구현 | 이메일 인증, 회원가입, 로그인, 토큰 갱신, 로그아웃 |
| 소셜 로그인 | ✅ 구현 | Kakao / Naver OAuth |
| 계정 재인증 | ✅ 구현 | 비밀번호·소셜 재인증 기반 |
| 마이페이지 / 회원 탈퇴 | ✅ 구현 | 내 정보 조회·수정, 재인증 후 회원 탈퇴 |
| 취향 프로필 | ✅ 구현 | 조회 + 전체 교체 저장 |
| MCM 제품 카탈로그 | ✅ 구현 | 목록·상세·카테고리·색상·가격 필터·다중 정렬·페이지네이션 |
| MCM 샘플 데이터 | ✅ 구현 | `src/main/resources/data/mcm-products.json` 기반 import 구조 |
| 제품 찜 | ✅ 구현 | 찜 등록·해제·목록 조회 |
| 제품 추천 | ✅ 구현 | 취향·상황 기반 Rule-Based 추천 생성 및 조회 |
| 마이 아이템 | ✅ 구현 | 등록·목록·상세·수정·Soft Delete, 검색·필터·정렬 |
| 구매 전 활용 가능성 | ✅ 구현 | Rule-Based 점수·요인·호환 아이템·관리 난이도 분석 |
| 공통 AI Job | ✅ 구현 | 생성·조회·멱등성·상태·timeout·비동기 처리 기반 |
| 구매 활용성 OpenAI 설명 | ✅ 구현 | Responses API, Structured Output, 24시간 캐시, 제한적 retry |
| 마이 아이템 이미지 | ✅ 구현 | JPEG/PNG 업로드, Cloudinary 저장, 연결·교체·삭제, TEMP/삭제대기 자동 정리 |
| `ITEM_ANALYSIS` AI | ✅ 구현 | TEMPORARY ITEM 이미지 분석, Structured Output, 멱등 생성, 비동기 처리, 최대 1회 retry |
| 제품 패스포트 / 활용도 분석 | ⏳ 미구현 | 상위 기능 구현 필요 |
| 착용 기록 / 다시 활용 안내 | ⏳ 미구현 | 상위 기능 구현 필요 |
| 스마트 착용 추천 | ⏳ 미구현 | 구현 필요 |
| 스타일 플랜 | 🧱 DB 기반 | V8 스키마 기반은 있으나 현재 controller/service 미구현 |
| 장소 추천 | 🧱 DB 기반 | V8 스키마 기반은 있으나 현재 controller/service 미구현 |
| 백엔드 운영 배포 | 🚧 준비 중 | Gabia 기준 운영 배포 준비 |

> `AiJobType`에는 향후 확장을 위한 타입이 존재하며, **현재 외부 AI Job 생성 API에서 실제 지원하는 타입은 `PURCHASE_UTILITY`, `ITEM_ANALYSIS`**입니다.

---

## 3. 최근 주요 반영 내역

### PR #31 — Kakao / Naver 소셜 로그인

- OAuth 시작 및 callback 처리
- 기존 사용자 로그인 / 신규 사용자 onboarding 분리
- Refresh Token 쿠키 연동
- 계정 삭제용 소셜 재인증 흐름

### PR #32 — 마이페이지 및 회원 탈퇴

- 현재 사용자 정보 조회·수정
- 비밀번호 / OAuth 재인증
- 재인증 토큰 기반 회원 탈퇴
- 사용자 소유 데이터 삭제 처리

### PR #33 — 마이 아이템 조회 및 관리

- 마이 아이템 CRUD
- 이름·브랜드 검색
- 카테고리·색상 필터
- 다중 정렬 및 페이지네이션
- Soft Delete
- 이미지/AI Job 연계 검증 기반

### PR #34 — 구매 전 활용 가능성 분석 기반

- 제품과 사용자 데이터를 이용한 Rule-Based 활용성 분석
- 취향·아이템 조합·계절·카테고리 요인 점수
- 호환 마이 아이템 snapshot 저장
- 분석 결과 상세 조회
- AI 설명 생성을 위한 Port / Job 기반 마련

### PR #35 — 구매 활용성 관리 난이도

- 소재 기반 관리 난이도 분석
- `EASY`, `MODERATE`, `HARD`, `UNKNOWN`
- Rule-Based fallback 설명에 관리 난이도 반영
- Value Score 자체와 관리 난이도는 분리

### PR #36 — 공통 AI Job + 구매 활용성 OpenAI

- 공통 `POST /api/ai-jobs`
- 공통 `GET /api/ai-jobs/{jobId}`
- `Idempotency-Key` + `request_hash`
- `PENDING / PROCESSING / SUCCEEDED / FAILED` 상태 관리
- DB clock 기준 2분 stale timeout
- Purchase Utility 비동기 processor 연결
- deterministic `input_hash`
- 동일 입력의 최근 24시간 AI summary cache
- OpenAI Responses API 연동
- Structured Output `{ "summary": "..." }`
- retryable 오류에 한해 최대 1회 재시도
- AI 실패 시 기존 Rule-Based 분석 결과 보존

---

## 4. 기술 스택

### Application

| 구분 | 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Build | Gradle Wrapper 9.5.1 |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA |
| Database | MySQL |
| Authentication | Spring Security + OAuth2 Resource Server + JWT |
| OAuth | Kakao / Naver |
| Validation | Jakarta Validation |
| DB Migration | Flyway |
| API Docs | springdoc-openapi 3.1.0 / Swagger UI |
| AI | OpenAI Responses API |
| Test | JUnit 5, Mockito, Testcontainers 2.0.5, H2 |
| CI | GitHub Actions |

### 주요 설계 원칙

- API 공통 prefix: `/api`
- 인증된 API는 Bearer Access Token 사용
- Refresh Token은 서버 관리 쿠키 기반
- JPA `ddl-auto=validate`
- DB 변경은 Flyway migration으로만 관리
- 서버/JPA 시간 기준은 UTC
- API 공통 응답·오류 형식 통일
- AI 처리 실패가 핵심 Rule-Based 결과를 파괴하지 않도록 fallback 유지

---

## 5. 패키지 구조

현재 `src/main/java/org/likelionhsu/hackathon`의 주요 도메인은 다음과 같습니다.

```text
hackathon/
├─ aijob/             # 공통 AI Job 생성·조회·상태·멱등성
├─ auth/              # 일반 인증, JWT, OAuth, 재인증
├─ common/            # 공통 설정, 응답, 예외, enum, health
├─ preference/        # 사용자 취향 프로필
├─ product/           # MCM 제품 카탈로그 및 import
├─ purchaseutility/   # 구매 전 활용 가능성 + AI 설명
├─ recommendation/    # MCM Rule-Based 제품 추천
├─ user/              # 마이페이지, 회원 탈퇴
├─ useritem/          # 마이 아이템 CRUD
└─ wishlist/          # 제품 찜
```

DB에는 Style Plan / Place 등 이후 기능을 위한 스키마도 일부 존재하지만,
**현재 Java application package가 존재하는 기능과 실제 API 구현 여부를 구분해서 봐야 합니다.**

---

## 6. 주요 API

아래는 현재 `main`에 실제 Controller가 존재하는 주요 API 그룹입니다.

### Health

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/health` | 서버 상태 확인 |

### Auth

Base path: `/api/auth`

| Method | Endpoint | 설명 |
| --- | --- | --- |
| POST | `/email-verifications` | 이메일 인증번호 요청 |
| POST | `/email-verifications/confirm` | 이메일 인증 확인 |
| GET | `/login-ids/{loginId}/availability` | 로그인 ID 중복 확인 |
| POST | `/signup` | 일반 회원가입 |
| POST | `/login` | 일반 로그인 |
| POST | `/refresh` | Access Token 갱신 |
| POST | `/logout` | 로그아웃 |
| GET | `/oauth/{provider}` | Kakao/Naver OAuth 시작 |
| GET | `/oauth/{provider}/callback` | OAuth callback |
| POST | `/oauth/signup` | 소셜 신규 사용자 가입 완료 |
| POST | `/reauthentications` | 비밀번호 기반 재인증 |
| GET | `/oauth/{provider}/reauthentication` | 소셜 계정 재인증 시작 |

### User

Base path: `/api/users/me`

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/users/me` | 내 정보 조회 |
| PATCH | `/api/users/me` | 닉네임·성별 부분 수정 |
| DELETE | `/api/users/me` | 재인증 확인 후 회원 탈퇴 |

### Preferences

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/preferences` | 내 취향 프로필 조회 |
| PUT | `/api/preferences` | 내 취향 프로필 전체 교체 저장 |

### Products

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/products` | MCM 제품 목록 / 필터 / 정렬 / 페이지네이션 |
| GET | `/api/products/{productId}` | MCM 제품 상세 |

주요 목록 조건:

- `category`
- `color`
- `minPrice`
- `maxPrice`
- `page`
- `size`
- `sort=createdAt|name|price,asc|desc`

### Wishlist

| Method | Endpoint | 설명 |
| --- | --- | --- |
| PUT | `/api/products/{productId}/favorite` | 제품 찜 |
| DELETE | `/api/products/{productId}/favorite` | 제품 찜 해제 |
| GET | `/api/wishlists` | 찜 목록 조회 |

### Recommendations

| Method | Endpoint | 설명 |
| --- | --- | --- |
| POST | `/api/recommendations` | Rule-Based MCM 제품 추천 생성 |
| GET | `/api/recommendations/{recommendationId}` | 추천 결과 상세 조회 |

현재 추천은 **OpenAI 추천이 아니라 서버 Rule-Based 추천**입니다.

### My Items

Base path: `/api/my-items`

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/my-items` | 목록·검색·필터·정렬 |
| POST | `/api/my-items` | 마이 아이템 등록 |
| GET | `/api/my-items/{myItemId}` | 상세 조회 |
| PATCH | `/api/my-items/{myItemId}` | 부분 수정 |
| DELETE | `/api/my-items/{myItemId}` | Soft Delete |

목록에서 현재 지원하는 주요 조건:

- 이름·브랜드 `keyword` 검색
- `category`
- `color`
- `page`, `size`
- `createdAt`, `name`, `purchaseDate`, `nextCareDate` 정렬

> 현재 마이 아이템 **정보 CRUD와 이미지 API가 모두 구현**되어 있습니다.

#### My Item Images

| Method | Endpoint | 설명 |
| --- | --- | --- |
| POST | `/api/image-assets` | JPEG/PNG 한 장을 multipart `file`로 업로드해 TEMPORARY ImageAsset 생성 |
| DELETE | `/api/image-assets/{imageAssetId}` | 연결 전 TEMPORARY 이미지 폐기 |
| PUT | `/api/my-items/{myItemId}/images/{imageAssetId}` | TEMPORARY 이미지를 연결하거나 기존 ACTIVE 이미지를 교체 |
| DELETE | `/api/my-items/{myItemId}/images/{imageAssetId}` | 연결된 ACTIVE 이미지 삭제 |

MVP 이미지 정책:

- 요청 한 번에 이미지 한 장
- JPEG / PNG만 허용
- 최대 10MB
- Cloudinary 저장
- UserItem 하나에는 최대 1개의 `ACTIVE` 이미지 유지
- UserItem은 이미지 없이도 생성 가능
- `TEMPORARY → ACTIVE → DELETE_PENDING → DELETED` lifecycle 사용
- 24시간이 지난 미연결 TEMPORARY 이미지는 cleanup 대상
- `PENDING / PROCESSING` AI Job이 사용하는 TEMPORARY 이미지는 삭제 및 TTL 정리에서 보호
- Cloudinary 삭제 실패 시 `DELETE_PENDING`을 유지하고 이후 cleanup에서 재시도

### AI Jobs

Base path: `/api/ai-jobs`

| Method | Endpoint | 설명 |
| --- | --- | --- |
| POST | `/api/ai-jobs` | AI 작업 멱등 생성 |
| GET | `/api/ai-jobs/{jobId}` | AI 작업 상태·결과 조회 |

현재 외부에서 생성 가능한 타입:

```text
PURCHASE_UTILITY
ITEM_ANALYSIS
```

AI Job 상태:

```text
PENDING
PROCESSING
SUCCEEDED
FAILED
```

### Purchase Utility

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/purchase-utility-analyses/{analysisId}` | 구매 활용성 분석 상세 조회 |

구매 활용성 분석 생성은 현재 공통 AI Job의 `PURCHASE_UTILITY` 작업 흐름과 연결됩니다.

---

## 7. 구매 활용성 분석

구매 활용성 기능은 **Rule-Based 핵심 분석 + 선택적 AI 설명** 구조입니다.

```text
제품 구매 활용성 요청
        │
        ▼
공통 AI Job 생성
        │
        ▼
Rule-Based 활용성 분석
        │
        ├─ 정보 부족
        │    └─ OpenAI 호출 없이 결과 종료
        │
        └─ 분석 가능
             │
             ▼
      deterministic input_hash
             │
             ▼
      최근 24시간 cache 조회
             │
        ┌────┴────┐
        │         │
    cache hit   cache miss
        │         │
        │         ▼
        │    OpenAI Responses API
        │         │
        └────┬────┘
             ▼
       분석 결과 최종화
```

### Rule-Based 분석 요소

- 사용자 취향 적합도
- 보유 마이 아이템과의 조합 가능성
- 계절 적합성
- 카테고리 조합성
- 호환 가능한 보유 아이템
- 소재 기반 관리 난이도

### AI 설명 정책

- Rule-Based 계산 사실만 AI 입력으로 사용
- Structured Output:
  ```json
  {
    "summary": "..."
  }
  ```
- 동일 semantic input의 최근 24시간 성공 결과 재사용
- timeout / 통신 / 일부 provider 오류 / 잘못된 구조화 응답만 최대 1회 재시도
- 최종 AI 실패 시 Rule-Based 분석은 그대로 유지

---

## 8. 공통 AI Job

AI Job은 특정 AI 기능 하나에 종속된 테이블/API가 아니라,
앞으로 여러 AI 기능에서 재사용하기 위한 **공통 비동기 작업 관리 기반**입니다.

공통으로 담당하는 것:

- Job ID 발급
- 사용자별 소유권
- `PENDING / PROCESSING / SUCCEEDED / FAILED`
- `Idempotency-Key`
- `request_hash`
- model / prompt version 기록
- retry count
- 오류 상태
- stale timeout
- 결과 조회

새로운 AI 기능을 붙일 때는 공통 AI Job 전체를 다시 만드는 대신 다음을 확장합니다.

```text
공통 AI Job
├─ PURCHASE_UTILITY  ← 현재 실제 연결 완료
├─ ITEM_ANALYSIS     ← 현재 실제 연결 완료
└─ STYLE_PLAN        ← 향후 구현 필요
```

기능별로 별도 구현이 필요한 부분은 입력 validation, processor, prompt/schema, AI adapter, 결과 저장 방식입니다.

---

## 9. OpenAI 설정

구매 활용성 AI 설명과 `ITEM_ANALYSIS` 이미지 분석은 OpenAI Responses API를 사용합니다.

### 환경변수

| 변수 | 필수 여부 | 설명 |
| --- | --- | --- |
| `OPENAI_API_KEY` | AI 사용 시 필수 | 값이 있을 때 OpenAI adapter 활성화 |
| `OPENAI_MODEL` | 선택 | 미지정 시 현재 코드 기본값 `gpt-5.6-luna` |

`OPENAI_API_KEY`가 없으면 애플리케이션 전체가 실패하는 것이 아니라
**OpenAI adapter만 생성되지 않으며**, 구매 활용성의 Rule-Based 분석은 유지되고 `ITEM_ANALYSIS` 작업은 AI 분석을 수행할 수 없습니다.

> 현재 `.env.example`에는 OpenAI 변수 예시가 아직 추가되어 있지 않습니다.
> README 업데이트 이후 별도의 환경설정 정리 작업에서 `.env.example`도 맞추는 것이 좋습니다.

---

## 10. DB / Flyway

현재 `main`의 최신 Flyway migration은 **V15**입니다.

| Version | 역할 |
| --- | --- |
| V1 | 사용자·일반 인증 기반 |
| V2 | AI Job 테이블 |
| V3 | 제품 카탈로그 |
| V4 | 취향 프로필·찜 |
| V5 | 마이 아이템·이미지 |
| V6 | 착용/관리 기록 초기 스키마 |
| V7 | 제품 추천 |
| V8 | 스타일 플랜·장소 DB 기반 |
| V9 | Product Tag 구조 정리 |
| V10 | Product Tag 기준 데이터 |
| V11 | User Item 관리 스키마 단순화 |
| V12 | Purchase Utility 중복 점수 컬럼 정리 |
| V13 | 계정 재인증 토큰 |
| V14 | Purchase Utility 관리 난이도 |
| V15 | 공통 AI Job request identity |

### Migration 규칙

- 이미 공유·커밋된 기존 migration 파일은 수정하지 않습니다.
- 스키마 변경은 항상 다음 버전 migration을 추가합니다.
- Hibernate는 `ddl-auto=validate`로 실제 스키마와 Entity 불일치를 검증합니다.
- MySQL 기반 migration은 Testcontainers integration test로 검증합니다.

---

## 11. 로컬 실행

### 요구사항

- Java 21
- MySQL
- Docker Desktop
    - 전체 integration test / Testcontainers 실행 시 필요
- Git

Gradle은 별도 설치 대신 repository의 wrapper를 사용합니다.

### 1) MySQL 준비

기본 local 설정:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hackathon_db?serverTimezone=UTC&characterEncoding=UTF-8
spring.datasource.username=${DB_USERNAME:hackathon}
spring.datasource.password=${DB_PASSWORD}
```

따라서 로컬 MySQL에 `hackathon_db`를 준비하고 환경변수를 설정합니다.

PowerShell 예시:

```powershell
$env:DB_USERNAME="hackathon"
$env:DB_PASSWORD="your-password"
```

### 2) 인증 관련 환경변수

개발 환경에서는 일부 기본값이 있지만 팀 단위 테스트에서는 명시적인 값을 권장합니다.

```powershell
$env:JWT_SECRET="replace-with-at-least-32-random-characters"
$env:CORS_ALLOWED_ORIGIN="http://localhost:3000"
```

Kakao / Naver OAuth를 사용할 경우:

```powershell
$env:KAKAO_CLIENT_ID="..."
$env:KAKAO_CLIENT_SECRET="..."
$env:NAVER_CLIENT_ID="..."
$env:NAVER_CLIENT_SECRET="..."
```

OpenAI 구매 활용성 설명을 실제 호출하려면:

```powershell
$env:OPENAI_API_KEY="..."
$env:OPENAI_MODEL="gpt-5.6-luna"
```

마이 아이템 이미지를 실제 Cloudinary에 업로드하려면:

```powershell
$env:CLOUDINARY_CLOUD_NAME="..."
$env:CLOUDINARY_API_KEY="..."
$env:CLOUDINARY_API_SECRET="..."
```

이미지 자동 정리는 local에서 기본 비활성화되고 production profile에서는 기본 활성화됩니다.
필요하면 `IMAGE_ASSET_CLEANUP_ENABLED`로 명시적으로 제어할 수 있습니다.

### 3) 선택: MCM 제품 데이터 import

```powershell
$env:APP_PRODUCT_IMPORT_ENABLED="true"
```

제품 import가 필요하지 않은 실행에서는 기본값 `false`를 사용할 수 있습니다.

### 4) 서버 실행

Windows:

```powershell
.\gradlew.bat bootRun
```

macOS / Linux:

```bash
./gradlew bootRun
```

기본 local profile이 적용되며 서버는 기본적으로 `8080` 포트를 사용합니다.

### 5) Health 확인

```http
GET http://localhost:8080/api/health
```

예상 응답의 핵심 값:

```json
{
  "data": {
    "status": "ok",
    "message": "Hackathon backend is running"
  }
}
```

---

## 12. Swagger / OpenAPI

애플리케이션 실행 후 springdoc 기본 경로를 사용합니다.

```text
Swagger UI
http://localhost:8080/swagger-ui/index.html

OpenAPI JSON
http://localhost:8080/v3/api-docs
```

OpenAPI에는 Bearer JWT 보안 스키마 `bearerAuth`가 정의되어 있습니다.

---

## 13. 테스트

테스트는 **빠른 단위 테스트와 MySQL Testcontainers integration test를 분리**합니다.

### 일반 테스트

```powershell
.\gradlew.bat test
```

`@Tag("integration")` 테스트는 제외합니다.

### Integration Test

Docker가 실행 중이어야 합니다.

```powershell
.\gradlew.bat integrationTest
```

### 전체 최종 검증

```powershell
.\gradlew.bat clean check
```

`check`는 `integrationTest`까지 포함합니다.

2026-08-17 `7d2d03f` 반영 전후 동일 코드 기준 로컬 최종 검증:

```text
BUILD SUCCESSFUL
```

### GitHub Actions

`.github/workflows/ci.yml`의 `Backend CI`는 다음 시점에 실행됩니다.

- `main` 대상 Pull Request
- `main` push

CI 명령:

```bash
./gradlew clean check --no-daemon
```

CI timeout은 10분입니다.

---

## 14. 인증 구조 요약

현재 인증 구조는 **서버 세션 방식이 아니라 JWT/OAuth2 기반**입니다.

### 일반 로그인

```text
회원가입/로그인
    │
    ├─ Access Token → API 요청의 Bearer JWT
    │
    └─ Refresh Token → 서버 관리 Cookie
```

### 소셜 로그인

```text
GET /api/auth/oauth/{provider}
        │
        ▼
Kakao / Naver
        │
        ▼
OAuth callback
        │
        ├─ 기존 사용자 → 로그인 완료
        └─ 신규 사용자 → onboarding → 소셜 가입 완료
```

### 회원 탈퇴

민감 작업인 회원 탈퇴는 최근 재인증을 요구합니다.

- 일반 계정: 비밀번호 재인증
- 소셜 계정: OAuth 재인증
- 유효한 재인증 확인 후 사용자 및 소유 데이터 삭제

---

## 15. API 공통 규칙

API 공통 규칙의 상세 원본은 [`API_CONVENTIONS.md`](./API_CONVENTIONS.md)를 사용합니다.

주요 원칙:

- JSON body: `camelCase`
- DB: `snake_case`
- 시간: UTC ISO-8601
- 목록 API: 0-base page
- 공통 성공 응답: `ApiResponse`
- 공통 오류 응답: `ErrorResponse`
- 인증 API: Bearer JWT
- 사용자 소유 리소스는 로그인 사용자 기준으로 조회·검증
- 세부 상태 코드·오류 코드는 실제 Controller / `ErrorCode` / API 규칙 문서를 우선

---

## 16. 배포 상태

### 현재 방향

- 백엔드 운영 배포 대상: **Gabia**
- 현재 상태: **운영 배포 준비 중**
- 운영 DB: MySQL 기준

### 현재 설정상 주의점

현재 repository의 `application-prod.properties`와 `.env.example`에는 다음과 같은 변수명이 남아 있습니다.

```text
MYSQLHOST
MYSQLPORT
MYSQLDATABASE
MYSQLUSER
MYSQLPASSWORD
```

또 `.env.example`에는 과거 Railway 기준 설명 문구가 남아 있습니다.

따라서 **Gabia 실제 배포 전에 production 환경변수와 문서 표현을 한 번 정리해야 합니다.**
README에서는 현재 팀의 실제 배포 방향인 Gabia를 기준으로 관리합니다.

---

## 17. 아직 구현되지 않은 주요 기능

현재 DB 테이블이 존재하는 것과 API가 실제 구현된 것은 구분해야 합니다.

### `ITEM_ANALYSIS` 후속 연계

마이 아이템 이미지 분석 백엔드는 구현되었습니다.

- JPEG/PNG 업로드 및 실제 이미지 검증
- Cloudinary 저장
- ImageAsset lifecycle 및 마이 아이템 연결·교체·삭제
- 24시간 TEMPORARY cleanup
- DELETE_PENDING 재시도 및 재시작 복구
- 회원 탈퇴 시 이미지 저장소 정리 큐 유지
- 공통 AI Job 기반 `ITEM_ANALYSIS` 요청 validation
- Item Analysis 비동기 processor
- OpenAI 이미지 분석 adapter + Structured Output
- retryable 오류 최대 1회 재시도

남은 것은 프론트에서 AI 분석 결과를 사용자에게 보여주고 확인·수정한 뒤 기존 마이 아이템 등록 API와 연결하는 화면 흐름입니다.

### 제품 패스포트 / 활용도

아직 상위 서비스/API 구현이 필요합니다.

예정 범위 예:

- 사용/착용 이력
- 제품 활용도 집계
- 다시 활용할 제품 안내
- 제품 단위 활동/관리 정보 표시

### 스마트 착용 / 스타일 플랜

V8 DB 기반은 존재하지만 현재 application layer 구현은 완료되지 않았습니다.

### 장소 추천

V8 장소 관련 DB 기반은 있으나 현재 Place Controller / Service는 없습니다.

프로젝트 방향상 장소 추천 구현 시 후보 검색과 백엔드 Rule-Based ranking, 프론트 지도 표시를 분리하는 방향을 사용합니다.

---

## 18. 다음 개발 흐름

현재 `main` 기준으로 기반 기능은 상당 부분 구현되었습니다.

다음 작업 후보는 다음과 같습니다.

1. AI 분석 결과 사용자 확인·수정 후 마이 아이템 등록 연계
2. 제품 패스포트 / 착용 기록 / 활용도 분석
3. 스마트 착용 추천 및 스타일 플랜
4. 장소 추천
5. Gabia 운영 배포 및 프론트엔드 실서버 연동
6. `.env.example` / production 설정 최신화

실제 작업 순서는 해커톤 일정과 FE 연동 우선순위에 따라 조정합니다.

---

## 19. Git 협업 규칙

### 기본 흐름

```text
main 최신화
  ↓
기능 브랜치 생성
  ↓
기능 구현 + 관련 테스트
  ↓
push
  ↓
Pull Request
  ↓
검증
  ↓
Squash and merge
```

브랜치 예:

```text
feat/item-analysis
fix/ai-job-timeout
docs/readme-current-status
```

커밋 / PR prefix:

```text
[FEAT]
[FIX]
[DOCS]
[CHORE]
[TEST]
[REFACTOR]
```

### 작업 전

```powershell
git switch main
git pull --ff-only origin main
git switch -c feat/<feature-name>
```

### PR 전 최종 확인

```powershell
git diff --check
.\gradlew.bat clean check
```

작업 중 생성한 개인 백업 파일, 임시 patch, 로컬 문서는 `git add .`로 한꺼번에 추가하지 않고 실제 반영할 파일만 명시적으로 stage합니다.

---

## 20. 현재 기준 요약

```text
서비스           입을래?
Backend          Java 21 / Spring Boot 4.1.0
DB               MySQL + Flyway V1~V15
Auth             JWT + Refresh Cookie + Kakao/Naver OAuth
Catalog          MCM 제품 조회/필터/정렬
Preference       취향 프로필 조회/저장
Wishlist         제품 찜
Recommendation   Rule-Based 제품 추천
My Item          CRUD + 검색/필터/정렬
Purchase Utility Rule-Based 분석 + 관리 난이도
AI Job           공통 비동기 작업 기반
OpenAI           Purchase Utility 설명 + ITEM_ANALYSIS 이미지 분석 연결 완료
Testing          Unit + Testcontainers Integration
CI               GitHub Actions clean check
Deployment       Gabia 준비 중
main             7d2d03f / PR #36
```

---

## 문서 유지 원칙

README의 상태 정보는 구현 진행에 따라 쉽게 오래될 수 있습니다.

따라서 기능 상태를 판단할 때는 다음 순서를 권장합니다.

1. 현재 GitHub `main`
2. Flyway migration / DB constraint
3. 실제 Controller / Service / Repository
4. `API_CONVENTIONS.md`
5. README
6. 과거 설계·인수인계 문서

README에서 **“구현 완료”**라고 표시하려면 실제 `main`에 코드와 필요한 테스트가 반영된 상태여야 합니다.
