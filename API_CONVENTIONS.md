# 입을래? API 공통 규칙

> 프론트엔드와 백엔드가 분리된 저장소에서 동일한 기준으로 API를 설계하고 연동하기 위한 팀 공통 규칙이다.
>
> **현행 검증 기준:** 2026-08-19 / Backend `main` `a9a7b85` (PR #49 API 공통 규칙 최신 구현 기준 동기화 반영)
>
> 이 문서는 현재 구현과 앞으로의 API 설계를 함께 안내한다. 문서와 코드가 일시적으로 어긋나는 경우에는 **실제 `main`의 Controller·DTO·공통 응답/예외 코드가 현재 실행 계약**이며, 차이를 발견하면 문서 또는 코드 중 하나를 임의로 추측해서 사용하지 말고 팀 합의를 거쳐 동기화한다.

---

## 1. 핵심 합의 사항

| 항목 | 팀 규칙 |
| --- | --- |
| API 공통 경로 | 모든 백엔드 API는 `/api`를 공통 prefix로 사용한다. `/v1`, `/v2`, `/MVP` 같은 버전 경로는 현재 사용하지 않는다. |
| 운영 호출 구조 | 브라우저는 프론트 Origin의 `/api/**`를 호출하고 프론트 rewrite/proxy를 통해 백엔드로 전달하는 구조를 기본으로 한다. 현재 백엔드 운영 배포 방향은 Gabia이다. |
| 환경별 주소 | API 주소, 인증 키, 외부 Provider 키 등은 소스 코드에 직접 작성하지 않고 환경변수 또는 배포 설정으로 관리한다. |
| 인증 | 일반 인증 API는 Bearer Access Token을 사용하고, Refresh Token은 서버 관리 `HttpOnly` Cookie를 사용한다. 서버 Session은 사용하지 않는 Stateless JWT 구조이다. |
| 성공 응답 | 응답 본문이 있는 일반 성공은 `{ "success": true, "data": ... }` 형식을 사용한다. |
| 오류 응답 | 일반 요청 실패는 `{ "success": false, "error": { "code", "message" } }` 형식을 사용한다. |
| Validation 오류 | `error.fields` 배열에 필드별 오류를 담는다. |
| 204 응답 | `204 No Content`에는 JSON 응답 본문을 보내지 않는다. |
| Redirect 응답 | OAuth 시작/Callback처럼 Redirect가 목적이면 `302 Found + Location`을 사용하고 `ApiResponse` JSON을 사용하지 않는다. |
| 날짜·시간 | ISO 8601 형식을 사용한다. 정확한 시각은 UTC `Instant` 기준이다. |
| 기준 시간대 | 서버·DB·API의 정확한 시각은 UTC, 화면 표시는 프론트에서 `Asia/Seoul`로 변환한다. |
| Enum | API Enum 값은 영문 대문자 `SNAKE_CASE`를 사용한다. |
| 응답의 ID | 현재 주요 응답 DTO의 ID는 문자열로 반환한다. |
| 요청의 ID | 기존 구현에는 문자열 ID와 숫자 ID 요청 DTO가 모두 있으므로 각 Endpoint 계약을 따른다. 표현 변경은 Breaking Change로 취급한다. |
| 이름 표기 | Endpoint는 `kebab-case`, JSON/Query/Path 이름은 `lowerCamelCase`, DB 컬럼은 `snake_case`를 사용한다. |
| 원화 금액 | 원 단위 정수 JSON number로 전달한다. |
| 점수 | 표시용 문자열이 아니라 JSON number로 전달한다. 정수뿐 아니라 `BigDecimal` 기반 소수 점수도 허용한다. |
| 목록 조회 | 계속 증가할 수 있는 목록은 `page`, `size`, `sort` 기반 페이지네이션을 사용하고, 작고 제한된 목록은 배열로 반환할 수 있다. |
| 상태 변경 | 찜·장바구니·장소 저장처럼 최종 상태를 설정하는 API는 가능한 한 멱등적으로 설계한다. |
| PATCH 충돌 방지 | 낙관적 락을 사용하는 리소스는 조회 응답의 `version`을 PATCH 요청에 함께 보내고 충돌 시 `409`를 반환한다. |
| 비동기 AI | AI 기능별 Job 시스템을 새로 만들지 않고 공통 `/api/ai-jobs` 계약을 사용한다. |

---

## 2. API 기본 주소와 프론트 호출 방식

### 2.1 `/api` 위치

API 공통 prefix는 `/api`이다.

```text
로컬 백엔드: http://localhost:8080/api
운영 브라우저: https://프론트도메인/api
```

운영 브라우저 호출 구조는 다음을 기본으로 한다.

```text
Browser
→ Frontend Origin /api/**
→ Frontend rewrite/proxy
→ Backend
```

현재 백엔드 운영 배포 방향은 Gabia이지만, 프론트 코드가 특정 백엔드 업체의 주소에 직접 종속되지 않도록 `/api` 상대 경로 사용을 기본으로 한다.

운영 환경 예시:

```env
NEXT_PUBLIC_API_BASE_URL=/api
```

로컬에서 브라우저가 백엔드를 직접 호출하는 예시:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
```

환경변수에 이미 `/api`가 포함되어 있으므로 개별 요청에서 `/api`를 중복해서 붙이지 않는다.

```ts
// 권장
api.get("/products");

// 비권장: baseURL에 /api가 이미 있다면 중복될 수 있음
api.get("/api/products");
```

### 2.2 환경변수 파일 관리

- 실제 비밀 값은 `.env.local`, 배포 서비스 Secret/Environment 설정 등 Git에 올라가지 않는 위치에 저장한다.
- `.env.example`에는 변수 이름과 안전한 예시만 저장한다.
- `NEXT_PUBLIC_`처럼 브라우저에 노출되는 변수에는 API Key, Client Secret, JWT Secret, Cloudinary Secret 등을 넣지 않는다.
- 백엔드 외부 Provider Key는 서버 환경변수로만 관리한다.

### 2.3 Axios 공통 인스턴스

공통 Axios 인스턴스에서 모든 요청의 `Content-Type`을 `application/json`으로 강제하지 않는다.

현재 프로젝트에는 JSON 요청뿐 아니라 `multipart/form-data` 이미지 업로드가 존재하므로 브라우저/Axios가 요청 데이터에 맞는 `Content-Type`과 multipart boundary를 설정하도록 둔다.

```ts
import axios from "axios";

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
});
```

JSON 요청:

```ts
api.post("/auth/login", {
  loginId: "user1234",
  password: "example-password",
});
```

이미지 업로드:

```ts
const formData = new FormData();
formData.append("file", file);

api.post("/image-assets", formData);
```

`FormData` 요청에서 브라우저가 설정해야 할 multipart boundary를 프론트가 임의 문자열로 고정하지 않는다.

### 2.4 Bearer Access Token

일반 보호 API는 Access Token을 `Authorization` Header로 전달한다.

```http
Authorization: Bearer <accessToken>
```

예:

```ts
api.get("/products", {
  headers: {
    Authorization: `Bearer ${accessToken}`,
  },
});
```

Access Token을 어떤 프론트 상태 저장 방식으로 보관할지는 프론트 구현 정책으로 관리하되, API 계약상 전달 방식은 Bearer Header이다.

### 2.5 Refresh Cookie와 Cookie 요청

Refresh Token은 서버가 Cookie로 설정하고 읽는다.

현재 Refresh Cookie의 핵심 정책은 다음과 같다.

```text
HttpOnly: true
SameSite: Lax
Path: /api/auth
Secure: 환경별 설정, 운영 true
```

프론트 JavaScript가 Refresh Token 문자열을 직접 읽어서 Header에 복사하는 방식은 사용하지 않는다.

운영에서 브라우저 기준 동일 Origin `/api/**` 호출을 사용하면 기본 Cookie 동작을 활용할 수 있다.

로컬에서 프론트 `http://localhost:3000`과 백엔드 `http://localhost:8080`을 직접 연결하는 cross-origin Cookie 요청은 credential을 포함한다.

Fetch 예시:

```js
fetch("http://localhost:8080/api/auth/refresh", {
  method: "POST",
  credentials: "include",
});
```

Axios 예시:

```ts
api.post("/auth/refresh", undefined, {
  withCredentials: true,
});
```

### 2.6 CORS와 Trusted Origin

백엔드 CORS는 현재 `/api/**`에 대해 정확한 허용 Origin을 사용하고 `allowCredentials(true)`를 적용한다.

```text
GET
POST
PUT
PATCH
DELETE
OPTIONS
```

Wildcard Origin과 credential 요청을 조합하지 않는다.

현재 허용 Origin 설정은 다음 백엔드 설정을 사용한다.

```properties
app.cors.allowed-origin=${CORS_ALLOWED_ORIGIN:http://localhost:3000}
```

**CORS 허용 Origin과 인증 POST의 Trusted Origin 검증은 목적이 다르다.**

- CORS: 브라우저의 cross-origin 접근 허용 정책
- Trusted Origin: 인증/민감 Cookie 변경 POST를 서버에서 추가 검증하는 정책

현재 `TrustedOriginFilter`도 `app.cors.allowed-origin`의 값을 기준으로 요청의 `Origin` Header가 정확히 일치하는지 확인한다.

---

## 3. URL 및 요청 작성 규칙

### 3.1 기본 REST 규칙

- 리소스 이름은 가능한 한 복수 명사와 소문자 `kebab-case`를 사용한다.
- 일반 CRUD는 동사를 URL에 중복해서 쓰지 않는다.
- 검색·필터·정렬·페이지 정보는 Query Parameter로 전달한다.
- 부모에 종속된 리소스는 필요한 경우 하위 리소스로 표현한다.

```http
GET    /api/products
GET    /api/products/{productId}
GET    /api/my-items/{myItemId}
PATCH  /api/my-items/{myItemId}
DELETE /api/my-items/{myItemId}
```

비권장:

```http
POST /api/createProduct
GET  /api/getProducts
POST /api/toggleFavorite
```

### 3.2 인증 Endpoint 예외

`/api/auth/**`는 일반 CRUD와 달리 인증 흐름 자체가 동작의 의미이므로 필요한 동작형 Endpoint를 허용한다.

현재 주요 예시는 다음과 같다.

```http
POST /api/auth/email-verifications
POST /api/auth/email-verifications/confirm
POST /api/auth/signup
POST /api/auth/login
POST /api/auth/oauth/signup
POST /api/auth/reauthentications
POST /api/auth/refresh
POST /api/auth/logout

GET /api/auth/oauth/{provider}
GET /api/auth/oauth/{provider}/callback
GET /api/auth/oauth/{provider}/reauthentication
```

현재 다음 **POST** 요청은 인증 처리 또는 민감 Cookie 변경 전에 Trusted Origin 검증 대상이다.

```http
POST /api/auth/signup
POST /api/auth/login
POST /api/auth/oauth/signup
POST /api/auth/reauthentications
POST /api/auth/refresh
POST /api/auth/logout
```

`Origin`이 누락되거나 `null`이거나 허용 Origin과 정확히 일치하지 않으면:

```text
HTTP 403
ErrorCode: ORIGIN_NOT_ALLOWED
```

OAuth Provider Callback은 외부 공급자에서 돌아오는 요청이므로 위 POST Trusted Origin 검사 대상이 아니며 OAuth `state` 검증을 사용한다.

### 3.3 이름 표기 규칙

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| Endpoint 경로 | 소문자 `kebab-case` | `/my-items`, `/style-plans`, `/image-assets` |
| Path Variable | `lowerCamelCase` | `{productId}`, `{myItemId}`, `{stylePlanId}` |
| Query Parameter | `lowerCamelCase` | `minPrice`, `maxPrice`, `latitude`, `plannedAt` |
| JSON 필드 | `lowerCamelCase` | `productId`, `purchaseDate`, `primaryImageUrl` |
| Enum 값 | 영문 대문자 `SNAKE_CASE` | `PURCHASE_UTILITY`, `IN_PROGRESS`, `CARE_REMINDER` |
| DB 컬럼 | `snake_case` | `product_id`, `purchase_date`, `created_at` |
| HTTP Header | 표준/계약 이름 유지 | `Authorization`, `Idempotency-Key` |

### 3.4 ID 필드명과 자료형

필드명은 가능한 한 대상 리소스를 포함한다.

```json
{
  "productId": "123",
  "myItemId": "25",
  "stylePlanId": "51",
  "placeId": "82",
  "jobId": "99"
}
```

ID 목록은 복수형을 사용한다.

```json
{
  "myItemIds": ["25", "31"]
}
```

#### 현재 구현의 ID 자료형 계약

현재 주요 **응답 DTO**는 DB 숫자 ID를 문자열로 직렬화해 반환하는 방식을 사용한다.

예:

```json
{
  "productId": "123",
  "stylePlanId": "51",
  "jobId": "99"
}
```

다만 현재 **요청 DTO**에는 두 형태가 공존한다.

1. 문자열 ID를 받는 계약
    - 예: AI Job `context.productId`, `context.imageAssetId`
2. JSON number를 받는 기존 write DTO
    - 예: 일부 마이아이템/스타일 플랜 생성·수정의 `productId`, `myItemId`, `aiJobId`

따라서 프론트는 ID라는 이유만으로 임의 형변환하지 않고 **해당 Endpoint의 Swagger/요청 DTO 계약을 따른다.**

기존 Endpoint의 ID 표현을 문자열↔숫자로 변경하는 것은 Breaking Change이다.

새 API를 설계할 때는 JavaScript 안전 정수 문제를 피하고 응답과의 일관성을 높이기 위해 문자열 ID를 우선 검토한다.

Path Variable은 URL 문자열이지만 DB 숫자 ID를 사용하는 현재 API에서는 일반적으로 `1` 이상의 정수 형식을 검증한다.

### 3.5 요청 필수·선택 필드

- 필수 필드는 요청에서 반드시 전달한다.
- 선택 필드를 입력하지 않는 경우 필드 자체를 생략하는 것을 기본으로 한다.
- 문자열 `"null"`, `"undefined"`를 보내지 않는다.
- 의미 없는 `""`를 미입력 값 대신 사용하지 않는다.
- 배열의 빈 상태를 `[]`로 표현할지 필드를 생략할지는 각 Endpoint 계약을 따른다.
- 필드 누락과 명시적 `null`은 동일하다고 가정하지 않는다.

예:

```json
{
  "name": "토트백",
  "category": "BAG"
}
```

비권장:

```json
{
  "name": "토트백",
  "category": "BAG",
  "material": "",
  "purchaseDate": "undefined"
}
```

### 3.6 PATCH 부분 수정과 명시적 `null`

PATCH에서 기본 의미는 다음과 같다.

| 요청 상태 | 의미 |
| --- | --- |
| 필드가 없음 | 기존 값 유지 |
| 새 값 전달 | 값 변경 |
| 명시적 `null` | 기본적으로 금지. 해당 Endpoint가 삭제 의미를 명시한 필드에서만 사용 |

프론트는 JavaScript의 `undefined`로 빠진 필드와 JSON에 실제로 포함된 `null`을 구분해야 한다.

현재 실제 예외 중 하나는 스타일 플랜의 `plannedAt`이다.

```json
{
  "plannedAt": null,
  "version": 2
}
```

해당 PATCH 계약에서 `plannedAt: null`은 저장된 일정을 제거한다.

이처럼 `null`이 삭제 의미를 가지는지는 반드시 개별 API 계약에 명시한다.

### 3.7 낙관적 락과 `version`

동시 수정 충돌을 막기 위해 낙관적 락을 사용하는 리소스는 수정 요청에 최신 `version`을 포함한다.

현재 대표적인 적용 대상:

```text
마이아이템 PATCH
스타일 플랜 PATCH
```

예:

```json
{
  "memo": "관리 후 상태 양호",
  "version": 3
}
```

프론트 규칙:

1. 상세 조회에서 반환된 최신 `version`을 저장한다.
2. PATCH 요청에 해당 `version`을 포함한다.
3. 수정 성공 후 응답의 새 `version`으로 갱신한다.
4. 다른 요청이 먼저 수정해 버전이 달라지면 `409 Conflict`를 처리한다.

대표 오류:

```text
RESOURCE_VERSION_CONFLICT
PREFERENCE_UPDATE_CONFLICT
USER_PROFILE_UPDATE_CONFLICT
```

버전 충돌 응답을 자동으로 마지막 쓰기 우선으로 덮어쓰지 않는다.

### 3.8 검색·필터 Query Parameter

검색, 필터, 정렬, 페이지 정보는 Query Parameter로 전달한다.

제품 예:

```http
GET /api/products?category=BAG&color=BLACK&minPrice=500000&maxPrice=1500000
```

마이아이템 예:

```http
GET /api/my-items?keyword=토트백&category=BAG&page=0&size=20
```

현재 장소 검색 예:

```http
GET /api/places?query=성수카페&category=CAFE&latitude=37.5445&longitude=127.0557&radius=3000
```

현재 저장 장소 목록 예:

```http
GET /api/places/saved?page=0&size=20&sort=createdAt,desc
```

선택하지 않은 조건은 Query Parameter 자체를 생략한다.

비권장:

```http
GET /api/products?category=
GET /api/products?category=null
GET /api/products?category=undefined
```

기본 표현:

| 종류 | 표현 |
| --- | --- |
| 검색어 | 문자열 |
| Enum | 영문 대문자 `SNAKE_CASE` |
| Boolean | `true` / `false` |
| 원화 금액 | 0 이상의 정수 |
| 날짜 | `YYYY-MM-DD` |
| 좌표 | JSON/Query 숫자 소수값 |
| 페이지 | 0 이상의 정수 |
| 페이지 크기 | 1~100 |

### 3.9 다중 선택 Query Parameter

한 필터에서 여러 값을 전달해야 하는 경우 기본적으로 동일한 Query Parameter 이름을 반복한다.

```http
GET /api/example?style=CASUAL&style=NEAT
```

프론트 예:

```ts
const params = new URLSearchParams();

styles.forEach((style) => {
  params.append("style", style);
});
```

다음 방식을 API마다 섞어 쓰지 않는다.

```text
?style=CASUAL,NEAT
?style[]=CASUAL&style[]=NEAT
?styles=CASUAL|NEAT
```

정렬도 여러 조건을 지원하는 API라면 `sort`를 반복할 수 있다.

```http
GET /api/products?sort=price,asc&sort=createdAt,desc
```

### 3.10 Boolean 표현

Boolean은 JSON의 실제 Boolean을 사용한다.

```json
{
  "favorited": true,
  "inCart": false,
  "saved": true,
  "hasNext": false
}
```

비권장:

```json
{
  "favorited": "true",
  "inCart": 0,
  "saved": "Y"
}
```

필드명은 실제 도메인 계약을 따른다. 현재 프로젝트에는 `favorited`, `inCart`, `saved`, `enabled`, `hasNext`, `hasPrevious`처럼 서로 다른 의미의 Boolean 필드가 있다.

### 3.11 숫자·점수·횟수

숫자 데이터에 화면용 단위를 붙인 문자열을 반환하지 않는다.

```json
{
  "utilityScore": 87.5,
  "compatibleItemCount": 3,
  "purchasePrice": 1250000
}
```

비권장:

```json
{
  "utilityScore": "87.5%",
  "compatibleItemCount": "3개",
  "purchasePrice": "1,250,000원"
}
```

점수는 API별 계산 정책에 따라 정수 또는 소수 JSON number가 될 수 있다. 현재 구매 활용성 분석과 제품 추천에는 `BigDecimal` 기반 점수가 존재하므로 공통 규칙에서 점수를 정수로 제한하지 않는다.

퍼센트 기호, `원`, `개`, `회` 같은 표시 단위는 프론트에서 붙인다.

### 3.12 정상적인 결과 없음과 오류 구분

요청 처리는 성공했지만 결과가 없는 경우는 일반적으로 오류가 아니다.

예:

```text
검색 결과 없음
추천 제품 없음
저장 장소 없음
장바구니 비어 있음
호환 마이아이템 없음
분석에 필요한 정보 부족
```

작은 목록:

```json
{
  "success": true,
  "data": []
}
```

페이지 목록:

```json
{
  "success": true,
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

기능상 결과 상태를 별도로 구분해야 하면 정상 응답 데이터 안에 Enum 상태를 둘 수 있다.

```json
{
  "success": true,
  "data": {
    "resultStatus": "INSUFFICIENT_DATA",
    "items": []
  }
}
```

반대로 특정 ID 리소스 자체가 없으면 `404 Not Found`이다.

### 3.13 찜·장바구니·장소 저장 상태 API

현재 상태를 단순 반전하는 `toggle`보다 원하는 최종 상태를 명시하는 API를 사용한다.

현재 실제 예:

```http
PUT    /api/products/{productId}/favorite
DELETE /api/products/{productId}/favorite

PUT    /api/products/{productId}/cart
DELETE /api/products/{productId}/cart

PUT    /api/places/{placeId}/saved
DELETE /api/places/{placeId}/saved
```

같은 상태 설정 요청을 반복해도 최종 상태가 달라지지 않도록 멱등성을 우선한다.

예:

```text
PUT favorite를 두 번 호출
→ 최종 상태는 계속 찜 상태

DELETE cart를 이미 없는 상태에서 다시 호출
→ 최종 상태는 계속 장바구니에 없음
```

응답 데이터가 필요하지 않은 해제 API는 `204 No Content`를 사용할 수 있다.

### 3.14 하위 리소스 URL

부모 리소스에 종속되는 기능은 부모 ID 아래에 표현할 수 있다.

현재 실제 예:

```http
PUT    /api/my-items/{myItemId}/images/{imageAssetId}
DELETE /api/my-items/{myItemId}/images/{imageAssetId}

GET /api/my-items/{myItemId}/passport
GET /api/my-items/{myItemId}/care-guide
GET /api/my-items/{myItemId}/storage-guide
GET /api/my-items/{myItemId}/care-calendar
GET /api/my-items/{myItemId}/care-reminder-setting
PUT /api/my-items/{myItemId}/care-reminder-setting
```

향후 사용 기록처럼 부모 아이템에 종속된 컬렉션을 구현한다면 다음 패턴을 사용할 수 있다.

```http
GET  /api/my-items/{myItemId}/usage-records
POST /api/my-items/{myItemId}/usage-records
```

단, 아직 구현되지 않은 Endpoint를 현재 구현된 API처럼 문서에 표현하지 않는다.

중첩은 필요한 수준까지만 사용한다.

비권장:

```text
/api/users/{userId}/my-items/{myItemId}/usage-records/{usageRecordId}/details
```

현재 로그인 사용자 소유 리소스는 URL에 `userId`를 중복 노출하기보다 인증 주체에서 사용자 ID를 가져오는 방식을 우선한다.

### 3.15 Path·Query Validation

Request Body뿐 아니라 Path Variable과 Query Parameter도 검증한다.

대표 오류:

| 대상 | 잘못된 예 |
| --- | --- |
| 페이지 | `page=-1` |
| 페이지 크기 | `size=0`, `size=101` |
| ID | `productId=0` 또는 숫자 ID 자리에 비숫자 문자열 |
| 금액 | `minPrice=-100` |
| 금액 범위 | `minPrice > maxPrice` |
| Enum | 지원하지 않는 Enum 문자열 |
| Boolean | Boolean 파라미터에 잘못된 값 |
| 날짜/시간 | 파싱할 수 없는 ISO 값 |
| 정렬 | 허용되지 않은 필드·방향 |

잘못된 요청값은 `500`이 아니라 `400 Bad Request`로 처리한다.

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "입력값을 확인해 주세요.",
    "fields": [
      {
        "field": "size",
        "reason": "100 이하여야 합니다."
      }
    ]
  }
}
```

형식은 유효하지만 리소스가 실제로 없으면 Validation 오류가 아니라 `404`이다.

### 3.16 시연용·샘플 데이터

모든 Product/Place 응답에 `isSample`을 강제하지 않는다.

현재 Product API는 `isSample` 필드를 공통 계약으로 반환하지 않으며, 현재 Place 검색은 Kakao Local의 실제 검색 결과를 사용한다.

따라서 프론트는 다음 값으로 샘플 여부를 임의 추측하지 않는다.

```text
ID 범위
제품명
이미지 URL
가격 범위
```

향후 실제 데이터와 시연용 데이터를 API에서 구분해야 하는 요구가 생기면 해당 리소스 계약에 `isSample` Boolean 또는 `dataSource` Enum을 명시적으로 추가한다.

그때까지 `isSample`은 공통 필수 필드가 아니다.

### 3.17 화면 집계 Read Model

여러 도메인의 저장 데이터를 한 화면에 보여주기 위해 화면 전용 집계 조회 Endpoint를 사용할 수 있다.

현재 예:

```http
GET /api/home
```

`/api/home`은 현재 사용자의 저장 데이터를 읽어 집계하는 Read Model이며, 이 조회 자체가 다음 작업을 새로 실행하지 않는다.

```text
Recommendation 생성
AI Job 생성
OpenAI 호출
Kakao Local 검색
장소 추천 실행
```

일반적인 조회 Endpoint가 숨은 부작용으로 외부 API 호출이나 비동기 Job 생성을 시작하지 않도록 한다. 생성이 필요한 동작은 별도의 명시적 POST 계약을 사용한다.

---

## 4. 이미지 업로드·ImageAsset 규칙

### 4.1 업로드

```http
POST /api/image-assets
Content-Type: multipart/form-data
```

현재 계약:

- multipart part 이름: `file`
- 요청 한 번에 이미지 한 장
- JPEG / PNG 허용
- multipart `file` part의 Content-Type은 `image/jpeg` 또는 `image/png`이며 실제 binary 형식과 일치해야 한다.
- WebP, `application/octet-stream` 등은 현재 허용하지 않는다.
- 최대 10MB
- 실제 이미지 binary/dimensions 검증
- 인증된 사용자만 사용
- 성공: `201 Created`
- FE는 소유자 ID, `myItemId`, `aiJobId`, 내부 상태, Cloudinary 관리 URL을 임의로 조작해서 보내지 않는다.

### 4.2 ImageAsset lifecycle

```text
TEMPORARY
→ ACTIVE
→ DELETE_PENDING
→ DELETED
```

현재 MVP에서는 UserItem 하나에 `ACTIVE` ITEM 이미지를 최대 1개 유지한다.

### 4.3 마이아이템 연결·교체

```http
PUT /api/my-items/{myItemId}/images/{imageAssetId}
```

- 첫 연결: `TEMPORARY → ACTIVE`
- 기존 ACTIVE가 있으면 기존 이미지를 `DELETE_PENDING`으로 바꾸고 새 이미지를 ACTIVE로 연결한다.
- 같은 이미지를 같은 마이아이템에 다시 연결하는 요청은 멱등 no-op으로 처리할 수 있다.
- 동시 연결/교체 충돌은 DB row lock과 상태 검증으로 처리한다.
- 다른 사용자 소유 ImageAsset은 존재 여부를 숨기기 위해 `IMAGE_ASSET_NOT_FOUND`로 처리한다.

### 4.4 삭제

연결 전 임시 이미지:

```http
DELETE /api/image-assets/{imageAssetId}
```

연결 이미지:

```http
DELETE /api/my-items/{myItemId}/images/{imageAssetId}
```

성공 시:

```text
204 No Content
```

외부 저장소 삭제 전 DB 상태를 `DELETE_PENDING`으로 전환하며 외부 저장소 삭제 실패 시 background cleanup이 재시도할 수 있다.

PENDING/PROCESSING AI Job이 사용하는 TEMPORARY 이미지는 직접 삭제하지 않는다.

```text
409 IMAGE_ASSET_IN_USE
```

### 4.5 ITEM_ANALYSIS와 최초 이미지 provenance

`ITEM_ANALYSIS` 결과를 기반으로 생성한 마이아이템은 분석에 사용한 이미지와 최초 연결 이미지의 입력 해시 정합성을 검증한다.

분석 이미지와 연결하려는 이미지가 일치하지 않으면:

```text
409 IMAGE_ASSET_ANALYSIS_MISMATCH
```

현재 `IMAGE_SORT_ORDER_CONFLICT` 정책은 사용하지 않는다. 과거 최대 3장/sortOrder 정책이 아니라 **UserItem당 최대 1개의 ACTIVE 이미지**가 현행 정책이다.

### 4.6 대표 이미지 오류 코드

```text
400 IMAGE_FILE_INVALID
404 IMAGE_ASSET_NOT_FOUND
409 IMAGE_ASSET_STATE_CONFLICT
409 IMAGE_ASSET_IN_USE
409 IMAGE_ASSET_ANALYSIS_MISMATCH
413 IMAGE_FILE_TOO_LARGE
415 IMAGE_FORMAT_UNSUPPORTED
502 IMAGE_STORAGE_ERROR
```

---

## 5. 성공 응답 형식

### 5.1 본문이 있는 일반 성공

```json
{
  "success": true,
  "data": {
    "productId": "123",
    "name": "MCM 가방"
  }
}
```

성공 응답에는 사용하지 않는 `error: null`을 넣지 않는다.

### 5.2 작은 목록

```json
{
  "success": true,
  "data": []
}
```

배열 결과가 없으면 `null`이 아니라 `[]`을 사용한다.

### 5.3 생성 성공

일반적인 동기 리소스 생성은 `201 Created`를 사용한다.

```http
HTTP/1.1 201 Created
```

```json
{
  "success": true,
  "data": {
    "myItemId": "25"
  }
}
```

현재 예:

```text
회원가입
소셜 회원가입
ImageAsset 생성
마이아이템 생성
스타일 플랜 저장
```

### 5.4 비동기 접수 성공

실제 처리가 비동기로 이어지는 요청은 `202 Accepted`를 사용할 수 있다.

현재 예:

```text
이메일 인증번호 발송
신규 AI Job 생성
```

AI Job은 멱등 재요청으로 기존 완료 Job을 반환할 때 `200 OK`가 될 수 있다.

### 5.5 응답 본문 없음

```http
204 No Content
```

`204`에는 다음 JSON을 보내지 않는다.

```json
{
  "success": true,
  "data": null
}
```

### 5.6 Redirect

OAuth 시작/Callback처럼 브라우저 Redirect가 목적이면:

```http
302 Found
Location: https://...
```

을 사용한다.

이 경우 `ApiResponse` JSON Wrapper를 요구하지 않는다.

### 5.7 HTTP 상태 코드 표

| 상황 | 상태 코드 |
| --- | ---: |
| 조회·수정 성공 | `200 OK` |
| 정상 빈 목록·정상 결과 없음 | `200 OK` |
| 동기 리소스 생성 | `201 Created` |
| 비동기 작업 접수 | `202 Accepted` |
| 본문 없는 성공 | `204 No Content` |
| OAuth 등 Redirect | `302 Found` |
| 잘못된 요청·Validation 실패 | `400 Bad Request` |
| 인증 실패·토큰 문제 | `401 Unauthorized` |
| 권한·Trusted Origin 거부 | `403 Forbidden` |
| 리소스 없음 | `404 Not Found` |
| 상태·중복·버전 충돌 | `409 Conflict` |
| 이미지 크기 초과 | `413 Content Too Large` |
| 지원하지 않는 미디어 형식 | `415 Unsupported Media Type` |
| 요청/사용 한도 초과 | `429 Too Many Requests` |
| 서버 내부 오류 | `500 Internal Server Error` |
| 외부 Provider/Storage 오류 | `502 Bad Gateway` |
| 외부 서비스 일시 사용 불가 | `503 Service Unavailable` |
| 외부 Provider timeout | `504 Gateway Timeout` |

---

## 6. 페이지네이션 규칙

### 6.1 요청

계속 증가할 수 있는 목록은 기본적으로:

```http
?page=0&size=20&sort=createdAt,desc
```

형식을 사용한다.

| Parameter | 규칙 | 기본 |
| --- | --- | ---: |
| `page` | 0부터 시작 | `0` |
| `size` | 1~100 | `20` |
| `sort` | `필드,asc|desc` | API별 기본 정렬 |

허용되는 sort 필드는 API마다 whitelist로 정의한다.

예:

```http
GET /api/products?sort=price,asc&sort=createdAt,desc
```

지원하지 않는 필드 또는 방향은 `400 VALIDATION_ERROR`로 처리한다.

### 6.2 공통 페이지 응답

현재 공통 `PageResponse<T>` 형식:

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "productId": "123",
        "name": "MCM 가방"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 41,
    "totalPages": 3,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

빈 결과:

```json
{
  "success": true,
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

화면에서 1부터 시작하는 페이지 번호가 필요하면 프론트가 `page + 1`로 표시한다.

### 6.3 현재 대표 페이지네이션 목록

현재 구현에서 대표적인 페이지 목록:

```text
MCM 제품 목록
찜 목록
마이아이템 목록
스타일 플랜 목록
저장 장소 목록
알림 목록
장바구니 목록
```

기능상 결과 수가 작고 제한적인 추천 preview 등에는 페이지네이션을 강제하지 않는다.

---

## 7. 일반 오류 응답

```json
{
  "success": false,
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "제품을 찾을 수 없습니다."
  }
}
```

규칙:

- 오류 응답에는 최상위 `success`, `error`를 포함한다.
- 오류 응답에 사용하지 않는 `data: null`을 넣지 않는다.
- `code`는 프론트 분기용 고정 코드이다.
- `message`는 사용자 안내 또는 안전한 개발 확인용 설명이다.
- 프론트는 `message` 문자열이 아니라 `code`를 기준으로 분기한다.
- SQL, stack trace, 파일 경로, 외부 Provider 원문 오류 등 내부 정보를 응답에 노출하지 않는다.

예:

```ts
if (error.code === "PRODUCT_NOT_FOUND") {
  // 제품 없음 UI
}
```

### 7.1 오류 코드 표기

오류 코드는 대문자 `SNAKE_CASE`를 사용한다.

대표 예:

```text
VALIDATION_ERROR
REQUEST_BODY_INVALID
PRODUCT_NOT_FOUND
MY_ITEM_NOT_FOUND
STYLE_PLAN_NOT_FOUND
PLACE_NOT_FOUND
NOTIFICATION_NOT_FOUND
AI_JOB_NOT_FOUND
RESOURCE_VERSION_CONFLICT
ORIGIN_NOT_ALLOWED
INTERNAL_SERVER_ERROR
```

### 7.2 현재 중요 오류 코드

| HTTP | ErrorCode | 의미 |
| ---: | --- | --- |
| 401 | `INVALID_CREDENTIALS` | 로그인 자격 증명 오류 |
| 401 | `ACCESS_TOKEN_INVALID` | Access Token 유효하지 않음 |
| 401 | `ACCESS_TOKEN_EXPIRED` | Access Token 만료 |
| 401 | `REFRESH_TOKEN_INVALID` | Refresh Token 유효하지 않음 |
| 403 | `ACCOUNT_NOT_ACTIVE` | 활성 계정이 아님 |
| 403 | `ORIGIN_NOT_ALLOWED` | Trusted Origin 검증 실패 |
| 409 | `RESOURCE_VERSION_CONFLICT` | 낙관적 락 버전 충돌 |
| 409 | `PREFERENCE_REQUIRED` | 추천 전 취향 정보 필요 |
| 409 | `IDEMPOTENCY_KEY_CONFLICT` | 같은 Idempotency-Key에 다른 요청 사용 |
| 409 | `AI_JOB_ALREADY_RUNNING` | 다른 AI Job이 이미 실행 중 |
| 429 | `AI_DAILY_LIMIT_EXCEEDED` | 최근 24시간 AI Job 생성 한도 초과 |
| 409 | `IMAGE_ASSET_STATE_CONFLICT` | ImageAsset 현재 상태와 요청 충돌 |
| 409 | `IMAGE_ASSET_IN_USE` | 실행 중 AI Job이 이미지 사용 중 |
| 409 | `IMAGE_ASSET_ANALYSIS_MISMATCH` | ITEM_ANALYSIS 입력 이미지와 최초 연결 이미지 불일치 |
| 413 | `IMAGE_FILE_TOO_LARGE` | 이미지 크기 초과 |
| 415 | `IMAGE_FORMAT_UNSUPPORTED` | 지원하지 않는 이미지 형식 |
| 502 | `IMAGE_STORAGE_ERROR` | 이미지 저장소 연동 실패 |
| 502 | `PLACE_PROVIDER_UNAVAILABLE` | 장소 Provider 호출 실패/사용 불가 |
| 504 | `PLACE_PROVIDER_TIMEOUT` | 장소 Provider timeout |
| 503 | `EMAIL_PROVIDER_UNAVAILABLE` | 이메일 Provider 일시 사용 불가 |

오류 코드의 최종 Source of Truth는 백엔드 `ErrorCode.java`이다.

### 7.3 잘못된 JSON

```json
{
  "success": false,
  "error": {
    "code": "REQUEST_BODY_INVALID",
    "message": "요청 본문 형식을 확인해 주세요."
  }
}
```

요청 본문 자체가 없거나 JSON 문법/역직렬화가 깨진 경우 필드별 Validation이 불가능할 수 있으므로 `fields` 없이 반환할 수 있다.

### 7.4 처리되지 않은 서버 오류

```json
{
  "success": false,
  "error": {
    "code": "INTERNAL_SERVER_ERROR",
    "message": "서버 오류가 발생했습니다."
  }
}
```

---

## 8. Validation 오류

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "입력값을 확인해 주세요.",
    "fields": [
      {
        "field": "email",
        "reason": "올바른 이메일 형식이 아닙니다."
      },
      {
        "field": "size",
        "reason": "100 이하여야 합니다."
      }
    ]
  }
}
```

규칙:

- `fields`는 Validation 오류에만 포함한다.
- `field`는 실제 JSON 필드, Query Parameter, Path Variable, Header 계약 이름과 최대한 일치시킨다.
- 한 필드에 여러 오류가 있더라도 사용자에게 필요한 우선 오류 하나를 반환할 수 있다.
- 여러 필드가 잘못되었다면 가능한 범위에서 한 번에 반환한다.
- 일반 Business Error에는 `fields: []`를 넣지 않는다.

Header Validation도 공통 Validation 규칙에 포함된다.

현재 대표 예:

```text
Idempotency-Key 누락/blank/길이 초과
```

---

## 9. 날짜·시간과 기준 시간대

### 9.1 형식

| 의미 | API 형식 | Java 타입 |
| --- | --- | --- |
| 날짜만 의미 | `YYYY-MM-DD` | `LocalDate` |
| 시간만 의미 | `HH:mm:ss` | `LocalTime` |
| 정확한 시각 | UTC ISO 8601 (`...Z`) | `Instant` |

예:

```text
purchaseDate: 2026-08-18
createdAt: 2026-08-18T12:30:00Z
plannedAt: 2026-08-20T09:00:00Z
```

### 9.2 현재 실제 예

`LocalDate` 계열:

```text
purchaseDate
nextCareDate
```

`Instant` 계열:

```text
createdAt
updatedAt
plannedAt
analyzedAt
addedAt
completedAt
```

현재 스타일 플랜의 일정 시각은 `plannedAt: Instant`이다. 과거의 `스타일 플랜 날짜 LocalDate + 시간 LocalTime` 예시를 현행 계약으로 사용하지 않는다.

### 9.3 시간대

- 서버/Jackson/Hibernate 정확한 시각 기준: UTC
- DB 연결 timezone: UTC
- API의 `Instant`: UTC ISO 8601
- 프론트 표시: 필요 시 `Asia/Seoul`
- 날짜 자체만 의미하는 `LocalDate`: 시간대 변환하지 않음

`2026년 8월 18일`, `08/18/2026` 같은 화면용 문자열을 백엔드가 만들어 보내지 않는다.

### 9.4 Entity 생성·수정 시각

공통 원칙:

```text
createdAt → 최초 생성 시각
updatedAt → 마지막 수정 시각
```

정확한 시각은 `Instant`를 사용한다.

신규 정확한 시각 도메인 값에 `LocalDateTime`, `java.util.Date`, `Calendar`, `Timestamp` 사용을 피한다.

---

## 10. Enum 표현

Enum은 영문 대문자 `SNAKE_CASE`로 전달한다.

현재 예:

```json
{
  "type": "STYLE_PLAN",
  "status": "PROCESSING",
  "occasion": "DATE",
  "weatherCondition": "RAINY"
}
```

사용자 표시용 한글 문구는 프론트에서 변환한다.

프론트가 백엔드에 정의되지 않은 Enum 문자열을 임의 생성하지 않는다.

Enum 값 추가는 일반적으로 호환 가능한 확장일 수 있지만, 기존 Enum 값 삭제/이름 변경은 Breaking Change이다.

---

## 11. 응답의 `null`, 빈 배열, 빈 문자열

| 상황 | 표현 |
| --- | --- |
| 선택 단일 값이 아직 없음 | `null` |
| 목록 결과 없음 | `[]` |
| 빈 문자열 자체가 의미 있음 | `""` 가능 |
| 공통 성공 응답의 사용하지 않는 `error` | 필드 생략 |
| 공통 오류 응답의 사용하지 않는 `data` | 필드 생략 |

예:

```json
{
  "profileImageUrl": null,
  "items": []
}
```

응답 계약에 포함된 선택 필드는 가능한 한 응답마다 존재하도록 하여 프론트 타입을 안정적으로 유지한다.

단, 공통 Wrapper에서 성공 시 `error`, 오류 시 `data`처럼 서로 배타적인 최상위 필드는 생략한다.

AI Job의 `result`, `fallback`, `error`, `completedAt`은 상태에 따라 `null`일 수 있다.

---

## 12. ID와 금액

### 12.1 ID

현재 응답 ID의 기본 예:

```json
{
  "productId": "123",
  "myItemId": "25",
  "jobId": "41"
}
```

응답 ID는 문자열을 우선한다.

다만 기존 요청 DTO에 숫자 ID가 존재하므로 프론트는 요청/응답을 같은 타입이라고 가정하지 않고 Endpoint 계약을 확인한다.

타입을 바꾸려면 별도 Breaking API 변경으로 공유한다.

### 12.2 금액

현재 MVP 통화 기준은 KRW이다.

```json
{
  "price": 1250000,
  "purchasePrice": 990000
}
```

- 원 단위 정수
- 쉼표 없음
- `원` 문자열 없음
- 현재 모든 금액에 `currency` 필드를 의무적으로 붙이지 않음

프론트 표시 예:

```ts
`${price.toLocaleString("ko-KR")}원`;
```

---

## 13. 인증·토큰·OAuth 공통 계약

### 13.1 구조

현재 인증 구조:

```text
Spring Security
+ OAuth2 Resource Server
+ JWT Access Token
+ Refresh Token Cookie
+ Stateless Session Policy
```

서버 Session에 로그인 상태를 저장하지 않는다.

### 13.2 일반 보호 API

```http
Authorization: Bearer <accessToken>
```

Access Token이 없거나 유효하지 않으면 `401`이다.

현재 보안 계층의 대표 오류:

```text
ACCESS_TOKEN_INVALID
ACCESS_TOKEN_EXPIRED
ACCOUNT_NOT_ACTIVE
RESOURCE_ACCESS_DENIED
```

### 13.3 회원가입·로그인

일반 회원가입:

```http
POST /api/auth/signup
→ 201 Created
→ Access Token 응답 데이터
→ Refresh Token Set-Cookie
```

로그인:

```http
POST /api/auth/login
→ 200 OK
→ Access Token 응답 데이터
→ Refresh Token Set-Cookie
```

### 13.4 Refresh

```http
POST /api/auth/refresh
```

Refresh Token은 Cookie에서 읽는다.

성공 시:

```text
200 OK
새 Access Token 데이터 반환
Refresh Cookie 재설정 가능
```

### 13.5 Logout

```http
POST /api/auth/logout
Authorization: Bearer <accessToken>
Cookie: refresh_token=...
```

현재 logout은 인증된 사용자 JWT와 Refresh Cookie를 사용한다.

성공 시:

```text
204 No Content
Refresh/Reauthentication Cookie clear
```

### 13.6 OAuth Redirect

OAuth 시작과 Callback은 JSON API가 아니라 Redirect 계약이다.

```http
GET /api/auth/oauth/{provider}
→ 302 Found

GET /api/auth/oauth/{provider}/callback
→ 302 Found
```

신규 소셜 사용자의 onboarding이 필요한 경우 Callback 결과에 따라 프론트 onboarding URL로 Redirect하고 서버 관리 onboarding Cookie를 사용할 수 있다.

### 13.7 재인증

비밀번호 재인증:

```http
POST /api/auth/reauthentications
Authorization: Bearer <accessToken>
```

성공 시 본문 없이 `204`와 재인증 Cookie를 설정한다.

소셜 계정 재인증은 OAuth Redirect 흐름을 사용한다.

---

## 14. 공통 AI Job 계약

AI 기능마다 별도 Job 시스템을 만들지 않고 공통 AI Job API를 사용한다.

### 14.1 현재 지원 타입

```text
PURCHASE_UTILITY
ITEM_ANALYSIS
STYLE_PLAN
```

### 14.2 생성

```http
POST /api/ai-jobs
Authorization: Bearer <accessToken>
Idempotency-Key: <required>
Content-Type: application/json
```

`Idempotency-Key`는 현재 최대 255자이다.

기본 요청:

```json
{
  "type": "PURCHASE_UTILITY",
  "context": {
    "productId": "123"
  }
}
```

ITEM_ANALYSIS:

```json
{
  "type": "ITEM_ANALYSIS",
  "context": {
    "imageAssetId": "31"
  }
}
```

STYLE_PLAN context는 현재 다음 필드를 사용한다.

```json
{
  "type": "STYLE_PLAN",
  "context": {
    "occasion": "DATE",
    "styleTags": ["CASUAL", "NEAT"],
    "weatherCondition": "CLOUDY",
    "prioritizeOwnedItems": true,
    "language": "ko"
  }
}
```

현재 STYLE_PLAN context 허용값과 조건:

```text
occasion (필수):
DAILY | DATE | TRAVEL | GATHERING | CEREMONY | OUTDOOR | OTHER

styleTags (필수):
CASUAL | FORMAL | NEAT | GLAMOROUS
- 1~4개
- 중복 불가

weatherCondition (선택):
SUNNY | CLOUDY | RAINY | SNOWY | HOT | COLD | WINDY | INDOOR | OTHER

prioritizeOwnedItems (필수):
true | false

language (필수):
ko
```

`weatherCondition`은 생략할 수 있지만 전달하는 경우 위 허용값 중 하나여야 한다.

현재 `language`는 `ko`만 허용한다.

타입별 context 필드를 섞어 보내지 않는다.

### 14.3 멱등성

같은 사용자가 같은 `Idempotency-Key`와 동일한 요청을 다시 보내면 기존 Job을 재사용한다.

같은 Key에 다른 요청 본문을 사용하면:

```text
409 IDEMPOTENCY_KEY_CONFLICT
```

신규 Job:

```text
202 Accepted
```

기존 완료 Job의 멱등 재조회:

```text
200 OK
```

### 14.4 생성 정책

현재 서버 정책:

- 사용자당 `PENDING`/`PROCESSING` AI Job은 동시에 1개 허용
- 최근 24시간 기준 AI Job 생성 한도 적용
- 현재 기본/공개 계약상 최대 10개
- 오래 멈춘 실행 중 Job은 stale timeout 처리 가능

대표 오류:

```text
409 AI_JOB_ALREADY_RUNNING
429 AI_DAILY_LIMIT_EXCEEDED
```

현재 stale PENDING/PROCESSING Job은 서버에서 timeout 실패 상태로 정리될 수 있으며 오류 코드는 `AI_JOB_TIMEOUT`이다.

### 14.5 생성 응답

```json
{
  "success": true,
  "data": {
    "jobId": "41",
    "type": "ITEM_ANALYSIS",
    "status": "PENDING",
    "createdAt": "2026-08-18T12:30:00Z"
  }
}
```

### 14.6 조회와 Polling

```http
GET /api/ai-jobs/{jobId}
Authorization: Bearer <accessToken>
```

상태:

```text
PENDING
PROCESSING
SUCCEEDED
FAILED
```

프론트 기본 흐름:

```text
POST /api/ai-jobs
→ jobId 확보
→ GET /api/ai-jobs/{jobId} polling
→ PENDING/PROCESSING이면 대기
→ SUCCEEDED 또는 FAILED이면 polling 종료
```

정확한 polling 간격은 프론트 구현에서 서버 부하를 고려해 정한다. 짧은 간격의 무한 polling을 하지 않는다.

### 14.7 조회 응답의 `result`, `fallback`, `error`

현재 AI Job 조회 데이터 구조:

```json
{
  "success": true,
  "data": {
    "jobId": "41",
    "type": "STYLE_PLAN",
    "status": "SUCCEEDED",
    "result": {},
    "fallback": null,
    "error": null,
    "createdAt": "2026-08-18T12:30:00Z",
    "completedAt": "2026-08-18T12:30:06Z"
  }
}
```

`result`의 JSON 구조는 `type`에 따라 다르며 공통 `{}` 구조로 고정되지 않는다.

`ITEM_ANALYSIS` 성공 `result` 예:

```json
{
  "brandName": "MCM",
  "name": "토트백",
  "category": "BAG",
  "primaryColor": "BLACK",
  "material": "LEATHER"
}
```

`PURCHASE_UTILITY` 분석 완료 `result` 예:

```json
{
  "status": "READY",
  "analysisId": "12"
}
```

`PURCHASE_UTILITY`는 분석에 필요한 정보가 부족하더라도 Job 처리 자체가 정상 완료된 경우 `SUCCEEDED` 상태에서 다음과 같은 `result`를 반환할 수 있다.

```json
{
  "status": "INSUFFICIENT_DATA",
  "analysisId": null,
  "message": "..."
}
```

`PURCHASE_UTILITY`가 `READY`이면 반환된 `analysisId`로 상세 분석을 조회한다.

```http
GET /api/purchase-utility-analyses/{analysisId}
Authorization: Bearer <accessToken>
```

`STYLE_PLAN` 성공 `result` 예:

```json
{
  "previewId": "job:41",
  "title": "...",
  "description": "...",
  "ownedItems": [],
  "recommendedProducts": [],
  "generationType": "AI"
}
```

각 타입의 세부 필드 계약은 실제 Controller·DTO·Swagger를 따른다.

중요한 구분:

**AI 처리 자체가 FAILED인 것과 HTTP 요청 실패는 다르다.**

Job 조회 요청이 정상적으로 처리되어 FAILED Job을 읽은 경우:

```json
{
  "success": true,
  "data": {
    "jobId": "41",
    "status": "FAILED",
    "result": null,
    "fallback": {},
    "error": {
      "code": "AI_STYLE_PLAN_FAILED",
      "message": "스마트 착용 추천 생성에 실패했습니다."
    }
  }
}
```

이때 최상위 `success`는 HTTP 조회 성공을 뜻한다.

반대로 존재하지 않는 Job을 조회한 경우는 HTTP 오류이다.

```json
{
  "success": false,
  "error": {
    "code": "AI_JOB_NOT_FOUND",
    "message": "AI 작업을 찾을 수 없습니다."
  }
}
```

### 14.8 AI fallback

AI 생성이 실패해도 규칙 기반 결과를 유지할 수 있는 기능에서는 `fallback`을 반환할 수 있다.

프론트는 `status == FAILED`라는 이유만으로 `fallback`을 무시하지 않고 기능별 계약을 확인한다.

생성 출처를 별도 필드로 제공하는 기능은 현재 다음 값을 사용한다.

```text
AI         → AI Provider 결과를 사용한 생성 결과
RULE_BASED → 서버 규칙 기반 생성 또는 대체 결과
```

현재 대표 필드는 다음과 같다.

```text
STYLE_PLAN                  → generationType
구매 활용성 분석 상세       → explanationGenerationType
```

모든 AI Job 타입이 동일한 생성 출처 필드를 반환한다고 가정하지 않는다.

---

## 15. 외부 Provider 연동 규칙

### 15.1 외부 호출은 서버에서 수행

다음 Provider Secret은 프론트로 노출하지 않는다.

```text
OpenAI API Key
Kakao Local REST API Key
Cloudinary API Secret
OAuth Client Secret
이메일 Provider Secret
```

### 15.2 장소 검색

현재 장소 검색은 Kakao Local 기반이다.

```http
GET /api/places
```

현재 주요 Query:

```text
query
category
latitude
longitude
radius
```

서버는 외부 검색 결과를 내부 Place 데이터와 연결/캐시할 수 있다. 프론트가 Kakao Provider 내부 응답 구조에 직접 의존하지 않는다.

외부 Provider 오류는 내부 500과 구분한다.

```text
502 PLACE_PROVIDER_UNAVAILABLE
504 PLACE_PROVIDER_TIMEOUT
```

### 15.3 이미지 저장소

Cloudinary 저장/삭제 상세 구현을 프론트 API 계약으로 노출하지 않는다.

프론트는 서버가 반환한 이미지 URL을 사용하되 Cloudinary `public_id`, 삭제 credential 등 내부 관리 정보로 삭제를 직접 시도하지 않는다.

---

## 16. API 변경의 분류

### 16.1 호환 가능한 변경 후보

- 새로운 Endpoint 추가
- 기존 동작을 바꾸지 않는 설명 보완
- 기존 클라이언트가 무시할 수 있는 선택 응답 필드 추가
- 선택 Query Parameter 추가

단, 새 필드가 프론트 TypeScript의 exhaustive type 처리에 영향을 줄 수 있으므로 실제 영향은 확인한다.

### 16.2 Breaking Change

다음은 기본적으로 Breaking Change이다.

```text
Endpoint 변경
HTTP Method 변경
요청/응답 필드 삭제
필드명 변경
자료형 변경
ID 문자열↔숫자 변경
필수 요청 필드 추가
Enum 기존 값 삭제/이름 변경
null 가능 여부 변경
PATCH null 의미 변경
페이지 시작 번호 변경
페이지 응답 구조 변경
기존 상태 코드 의미 변경
오류 코드 의미 변경
인증 방식 변경
Cookie path/SameSite 정책 변경으로 FE 요청 방식이 달라지는 경우
Idempotency-Key 요구 여부 변경
```

Breaking Change는 프론트와 백엔드가 적용 순서를 합의한다.

---

## 17. 프론트·백엔드 저장소 간 API 변경 공유

프론트 요구만으로 백엔드 API 계약을 프론트 저장소에서 먼저 확정하지 않는다.

권장 흐름:

1. 변경 필요성을 Issue 또는 팀 채널에서 공유한다.
2. 현재 요청/응답과 문제점을 적는다.
3. 변경 후 원하는 계약 예시를 적는다.
4. FE/BE가 호환성과 적용 순서를 확인한다.
5. Backend 코드·Swagger·테스트를 수정한다.
6. Backend PR을 생성한다.
7. 확정된 Backend PR/merge 기준으로 FE를 연동한다.
8. `API_CONVENTIONS.md`에 영향을 주는 공통 정책이면 같은 PR 또는 후속 문서 PR에서 동기화한다.

API 계약의 최종 확인 순서:

```text
현재 main Controller / Request·Response DTO
→ 공통 response / ErrorCode / Security 설정
→ 관련 테스트
→ API_CONVENTIONS.md
→ README 요약
```

문서와 코드가 다르면 오래된 문서를 그대로 믿지 않고 차이를 해결한다.

---

## 18. API 변경 PR 템플릿

````md
## 변경 유형

- [ ] 새로운 API 추가
- [ ] 호환 가능한 변경
- [ ] Breaking Change
- [ ] 문서만 변경

## 대상 API

- Method: `GET`
- Path: `/api/products`

## 변경 이유

변경이 필요한 이유를 작성합니다.

## 변경 전

```json
{}
```

## 변경 후

```json
{}
```

## 요청 규칙

- Path:
- Query:
- Header:
- Body:
- 인증:

## 응답

- 성공 상태 코드:
- 성공 데이터:
- 빈 결과:

## 오류

- `400`:
- `401`:
- `404`:
- `409`:
- 기타 Provider/Rate Limit 오류:

## 프론트 영향

- 타입 변경 여부:
- 화면 분기 변경 여부:
- 인증/Cookie 영향:
- 적용 순서:

## 관련 작업

- Backend Issue:
- Backend PR:
- Frontend Issue/PR:

## 확인 체크리스트

- [ ] Swagger가 실제 코드와 일치합니다.
- [ ] 성공/오류 공통 응답 규칙을 지킵니다.
- [ ] Validation 오류 필드명을 확인했습니다.
- [ ] ID 자료형을 확인했습니다.
- [ ] null/빈 배열 계약을 확인했습니다.
- [ ] 페이지네이션/정렬 계약을 확인했습니다.
- [ ] 인증/Origin/Cookie 영향을 확인했습니다.
- [ ] 필요한 테스트가 추가되었습니다.
- [ ] `./gradlew clean check`가 통과했습니다.
- [ ] FE 담당자가 Breaking 영향 여부를 확인했습니다.
````

PR 제목 예:

```text
[API] 제품 목록 응답 보완
[API] AI Job 오류 계약 문서화
[API][Breaking] 요청 ID 자료형 변경
[DOCS] API_CONVENTIONS 현행 코드 기준 동기화
```

---

## 19. 프론트 대응 PR 체크리스트

```md
## 관련 Backend 변경

- Backend PR:
- 대상 API:

## 프론트 변경

- 요청 DTO 변경:
- 응답 타입 변경:
- 오류 분기 변경:
- 인증/Cookie 변경:

## 연동 확인

- [ ] 정상 성공
- [ ] 빈 결과
- [ ] Validation 오류
- [ ] 401 인증 오류
- [ ] 403 권한/Origin 오류
- [ ] 404 리소스 없음
- [ ] 409 충돌
- [ ] 429 한도 초과(해당 기능)
- [ ] 5xx Provider 오류(해당 기능)
- [ ] 모바일/새로고침 후 인증 흐름
- [ ] multipart 이미지 업로드(해당 기능)
- [ ] AI Job polling 종료 처리(해당 기능)
```

---

## 20. API 구현 완료 조건

공통 API 작업은 다음을 확인한다.

- [ ] 실제 Controller/DTO가 확정되었다.
- [ ] Swagger 설명이 실제 계약과 일치한다.
- [ ] 인증 필요 여부가 명확하다.
- [ ] Bearer/Cookie/Trusted Origin 요구사항을 확인했다.
- [ ] 성공 응답은 공통 Wrapper 또는 명시된 예외(204/302)를 따른다.
- [ ] 오류 응답은 공통 ErrorCode 계약을 따른다.
- [ ] Validation 오류는 `error.fields` 규칙을 따른다.
- [ ] ID 필드명과 실제 자료형을 확인했다.
- [ ] PATCH의 생략/null/version 의미를 확인했다.
- [ ] 날짜·시간은 `LocalDate`/`Instant` 의미에 맞게 사용했다.
- [ ] Enum은 대문자 `SNAKE_CASE`이다.
- [ ] 빈 목록은 `[]` 또는 `items: []`이다.
- [ ] 페이지네이션이 필요한 목록은 공통 `PageResponse`를 사용한다.
- [ ] 허용 sort 필드를 검증한다.
- [ ] 멱등성이 필요한 상태 API를 검증한다.
- [ ] AI 기능은 기존 공통 AI Job 시스템을 재사용한다.
- [ ] 외부 Provider 실패를 내부 500과 구분한다.
- [ ] 단위 테스트/통합 테스트가 필요한 범위에서 추가되었다.
- [ ] `./gradlew clean check`가 통과한다.
- [ ] Breaking Change라면 FE와 적용 순서를 합의했다.
- [ ] 공통 정책 변화가 있으면 `API_CONVENTIONS.md`를 함께 갱신한다.

---

## 21. 현재 문서에서 폐기된 과거 규칙

다음은 과거 설계에서 사용되었거나 문서에 남아 있었지만 현재 `main` 기준으로 사용하지 않는 규칙이다.

```text
Railway Backend를 고정 전제로 한 운영 설명
모든 Axios 요청에 Content-Type: application/json 강제
서버 Session 기반 로그인 상태 유지
ITEM 이미지 최대 3장 + sortOrder 충돌 정책
IMAGE_SORT_ORDER_CONFLICT
모든 시연 데이터에 isSample 필수
모든 점수를 0~100 정수로 제한
스타일 플랜을 LocalDate + LocalTime으로 표현한다는 예시
region/placeType 기반의 과거 장소 검색 예시
usage-records를 현재 구현 완료 API로 표현하는 설명
```

과거 문서나 채팅의 예시보다 현재 `main`의 실제 계약을 우선한다.
