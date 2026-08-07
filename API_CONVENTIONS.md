# 입을래? API 공통 규칙

> 프론트엔드와 백엔드가 분리된 저장소에서 동일한 기준으로 API를 설계하고 연동하기 위한 팀 공통 규칙이다.

## 1. 핵심 합의 사항

| 항목            | 팀 규칙                                                                    |
| --------------- | -------------------------------------------------------------------------- |
| API 공통 경로   | 도메인 말단에 `/api`를 사용하며 `v1,v2,MVP`와 같은 경로는 사용하지 않는다. |
| 환경별 주소     | 소스 코드에 직접 작성하지 않고 환경변수로 관리한다.                        |
| 요청 경로       | 환경변수에 `/api`를 포함하고, 개별 요청에는 리소스 경로만 작성한다.        |
| 성공 응답       | `{ "success": true, "data": ... }`                                         |
| 오류 응답       | `{ "success": false, "error": { "code", "message" } }`                     |
| Validation 오류 | `error.fields` 배열에 필드별 오류를 담는다.                                |
| 필드 포함 규칙  | 성공 응답에는 `error`를, 오류 응답에는 `data`를 포함하지 않는다.           |
| 날짜·시간       | ISO 8601 형식을 사용한다.                                                  |
| 기준 시간대     | 서버·DB·API는 UTC, 화면 표시는 `Asia/Seoul`을 사용한다.                    |
| Enum            | 영문 대문자 `SNAKE_CASE`를 사용한다.                                       |
| 응답 값 없음    | 단일 값은 `null`, 목록은 `[]`을 반환한다.                                  |
| ID              | API에서는 문자열로 전달한다.                                               |
| ID 필드명        | `productId`, `myItemId`처럼 대상 리소스 이름을 포함한다.                    |
| 이름 표기        | Endpoint는 `kebab-case`, API 필드는 `lowerCamelCase`, DB 컬럼은 `snake_case`를 사용한다. |
| 원화 금액       | 원 단위 정수로 전달한다.                                                   |
| 목록 조회       | 계속 증가할 수 있는 목록은 `page`, `size`, `sort` 기반 페이지네이션을 사용하고, 작은 고정 목록은 배열로 반환한다. |

---

## 2. API 기본 주소와 `/api` 위치

### 2.1 기본 주소

API의 공통 경로는 `/api`로 통일한다. 별도의 버전 경로인 `/v1`은 붙이지 않는다.

```text
로컬 개발: http://localhost:8080/api
운영 환경: https://서비스도메인/api
```

프론트엔드와 백엔드가 운영 환경에서 같은 도메인을 사용한다면 운영 환경의 값은 다음처럼 상대 경로로 둘 수 있다.

```env
NEXT_PUBLIC_API_BASE_URL=/api
```

브라우저가 백엔드 서버를 직접 호출하는 로컬 개발 환경의 예시는 다음과 같다.

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
```

`NEXT_PUBLIC_`이 붙은 환경변수는 브라우저에 공개될 수 있다. API 기본 주소는 공개되어도 되는 값이지만, API 키나 비밀번호 같은 비밀 값은 절대 넣지 않는다.

### 2.2 환경변수 파일 관리

- 실제 값은 `.env.local` 등 배포 환경에 맞는 파일이나 배포 서비스 설정에 저장한다.
- 실제 환경변수 파일은 Git에 올리지 않는다.
- 변수 이름과 예시만 담은 `.env.example`은 저장소에 올린다.

`.env.example` 예시:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
```

### 2.3 Axios 공통 인스턴스

```ts
import axios from "axios";

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});
```

환경변수에 이미 `/api`가 포함되어 있으므로 개별 요청에는 `/api`를 다시 붙이지 않는다.

```ts
// 권장: GET http://localhost:8080/api/products
api.get("/products");

// 금지: /api가 중복될 수 있음
api.get("/api/products");
```

---

## 3. URL 및 요청 작성 규칙

- 리소스 이름은 복수 명사와 소문자 케밥 표기법을 사용한다.
- 동작을 URL에 넣기보다 HTTP Method로 표현한다.
- 경로의 ID 이름은 어떤 리소스의 ID인지 알 수 있게 작성한다.
- 검색·필터·정렬·페이지 정보는 Query Parameter로 전달한다.

```http
GET    /api/products
GET    /api/products/{productId}
POST   /api/products
PATCH  /api/products/{productId}
DELETE /api/products/{productId}

GET /api/products?page=0&size=20&sort=createdAt,desc
```

다음과 같이 동사를 URL에 중복해서 쓰는 방식은 피한다.

```http
POST /api/createProduct
GET  /api/getProducts
```

### 3.1 이름 표기 규칙

API와 백엔드 내부에서 사용하는 이름은 다음 기준으로 통일한다.

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| Endpoint 경로 | 소문자 `kebab-case`, 가능한 한 복수 명사 | `/products`, `/my-items`, `/style-plans` |
| Path Variable | `lowerCamelCase` | `{productId}`, `{myItemId}` |
| Query Parameter | `lowerCamelCase` | `minPrice`, `maxPrice`, `careNeeded` |
| JSON 요청·응답 필드 | `lowerCamelCase` | `productId`, `purchaseDate`, `imageUrls` |
| Enum 값 | 영문 대문자 `SNAKE_CASE` | `STREET_CASUAL`, `IN_PROGRESS` |
| DB 컬럼 | `snake_case` | `product_id`, `purchase_date`, `created_at` |

예:

```http
GET /api/my-items/{myItemId}?careNeeded=true
```

```json
{
  "myItemId": "25",
  "purchaseDate": "2026-08-07",
  "purchasePrice": 1250000
}
```

Endpoint에서 여러 단어가 필요한 경우 다음처럼 작성한다.

```text
권장
/my-items
/style-plans
/usage-records
/care-records

비권장
/myItems
/style_plans
/usageRecords
```

### 3.2 ID 필드명 규칙

API 요청과 응답에서는 가능한 한 `id`처럼 대상이 불분명한 이름보다 리소스 이름을 포함한 ID 필드명을 사용한다.

```json
{
  "productId": "123",
  "myItemId": "25",
  "stylePlanId": "51",
  "placeId": "82"
}
```

ID 목록은 복수형으로 작성한다.

```json
{
  "myItemIds": ["25", "31"]
}
```

Path Variable도 같은 기준을 사용한다.

```http
GET /api/products/{productId}
GET /api/my-items/{myItemId}
GET /api/style-plans/{stylePlanId}
GET /api/places/{placeId}
```

ID의 자료형은 이 문서의 ID 자료형 규칙에 따라 문자열로 전달한다.

### 3.3 요청 필드의 필수·선택 규칙

요청 DTO의 필드는 기능명세서에 따라 필수값과 선택값을 구분한다.

- 필수값은 요청에서 반드시 전달한다.
- 선택값을 입력하지 않은 경우에는 요청 JSON에서 해당 필드를 생략하는 것을 기본으로 한다.
- 의미 없는 빈 문자열 `""`, 문자열 `"null"`, 문자열 `"undefined"`를 대신 전달하지 않는다.
- 선택값에도 `null` 전달을 기본적으로 허용하지 않는다. 값을 명시적으로 삭제해야 하는 기능이 필요한 경우에만 해당 API 계약에서 별도로 정의한다.
- 배열 선택값에 항목이 없다면 API 계약에 따라 필드를 생략하거나 빈 배열 `[]`을 사용한다. 두 방식을 같은 필드에서 혼용하지 않는다.

예를 들어 마이아이템 직접 등록에서 제품명과 제품 종류만 필수이고 구매일, 구매 가격, 소재, 색상 등이 선택값이라면 다음처럼 요청한다.

```json
{
  "name": "토트백",
  "category": "BAG"
}
```

입력하지 않은 선택값을 다음처럼 임의의 빈 값으로 채우지 않는다.

```json
{
  "name": "토트백",
  "category": "BAG",
  "material": "",
  "color": "",
  "purchaseDate": ""
}
```

필수값 누락이나 형식 오류는 `400 Bad Request`와 `VALIDATION_ERROR`로 처리한다.

### 3.4 `PATCH` 부분 수정 규칙

`PATCH`는 리소스 전체를 다시 보내는 것이 아니라 변경할 필드만 전달한다.

기본 규칙은 다음과 같다.

| PATCH 요청 상태 | 의미 |
| --- | --- |
| 필드 자체가 없음 | 기존 값을 유지한다. |
| 새로운 값 전달 | 해당 값으로 변경한다. |
| 명시적으로 `null` 전달 | 기본적으로 허용하지 않는다. 값 삭제가 필요한 필드는 해당 API에서 별도로 정의한다. |

예를 들어 기존 마이아이템 정보가 다음과 같다고 가정한다.

```json
{
  "myItemId": "25",
  "material": "LEATHER",
  "purchasePrice": 1200000
}
```

구매 가격만 수정하려면 변경할 값만 전달한다.

```json
{
  "purchasePrice": 1300000
}
```

결과:

```json
{
  "myItemId": "25",
  "material": "LEATHER",
  "purchasePrice": 1300000
}
```

요청에 포함되지 않은 `material`은 기존 값을 유지한다.

`PATCH` 요청에서도 `null`은 기본적으로 허용하지 않는다.

필수값에는 `null`을 허용하지 않으며, 선택값을 명시적으로 삭제해야 하는 기능이 필요한 경우에는 해당 API에서 삭제 방법과 요청 형식을 별도로 정의한다.

### 3.5 검색·필터 Query Parameter 규칙

검색, 필터, 정렬, 페이지 정보는 Query Parameter로 전달한다.

Query Parameter 이름은 `lowerCamelCase`를 사용한다.

예:

```http
GET /api/products?category=BAG&color=BLACK&minPrice=500000&maxPrice=1500000
```

```http
GET /api/my-items?keyword=토트백&category=BAG&page=0&size=20
```

```http
GET /api/places?region=SEOUL&placeType=CAFE
```

선택하지 않은 검색·필터 조건은 Query Parameter 자체를 보내지 않는 것을 기본으로 한다.

권장:

```http
GET /api/products?category=BAG
```

다음처럼 의미 없는 값을 전달하지 않는다.

```http
GET /api/products?category=
GET /api/products?category=null
GET /api/products?category=undefined
```

검색어가 비어 있다면 `keyword=`를 전송하기보다 `keyword` 자체를 생략한다.

Query Parameter의 기본 규칙은 다음과 같다.

| 종류 | 규칙 | 예시 |
| --- | --- | --- |
| 검색어 | 문자열 | `keyword=토트백` |
| Enum 필터 | 영문 대문자 `SNAKE_CASE` | `category=BAG` |
| Boolean | `true`, `false` | `careNeeded=true` |
| 금액 | 원 단위 정수 | `minPrice=500000` |
| 날짜 | `YYYY-MM-DD` | `usedDate=2026-08-07` |
| 페이지 | 0부터 시작하는 정수 | `page=0` |
| 페이지 크기 | `1` 이상 `100` 이하의 정수 | `size=20` |

필터를 사용하지 않은 경우와 필터 결과가 없는 경우를 구분한다.

필터 조건에 맞는 결과가 없더라도 요청 자체가 정상이라면 `404 Not Found`가 아니라 `200 OK`와 빈 목록을 반환한다.

예:

```json
{
  "success": true,
  "data": []
}
```

페이지네이션 API라면 기존 페이지 응답 형식을 유지하면서 `items`를 빈 배열로 반환한다.

### 3.6 다중 선택 Query Parameter 규칙

하나의 필터에서 여러 값을 선택할 수 있는 경우 동일한 Query Parameter 이름을 반복해서 전달한다.

예를 들어 여러 스타일을 선택하는 경우:

```http
GET /api/products?style=CASUAL&style=MINIMAL
```

여러 카테고리를 선택하는 경우:

```http
GET /api/products?category=BAG&category=SHOES
```

백엔드는 이를 목록으로 처리한다.

예:

```java
List<String> style
```

또는 실제 도메인 Enum이 확정된 경우:

```java
List<Style> style
```

프론트에서는 `URLSearchParams.append()` 등을 사용해 같은 키를 반복해서 추가할 수 있다.

예:

```ts
const params = new URLSearchParams();

styles.forEach((style) => {
  params.append("style", style);
});
```

다음과 같은 여러 표현 방식을 API마다 혼용하지 않는다.

```text
비권장

?style=CASUAL,MINIMAL
?style[]=CASUAL&style[]=MINIMAL
?styles=CASUAL|MINIMAL
```

다중 선택값이 하나도 없다면 빈 값이나 빈 문자열을 전달하지 않고 해당 Query Parameter 자체를 생략한다.

```text
권장

GET /api/products

비권장

GET /api/products?style=
```

정렬 조건의 경우에도 기존 페이지네이션 규칙과 동일하게 같은 `sort` Parameter를 반복해서 전달한다.

```http
GET /api/products?sort=status,asc&sort=createdAt,desc
```

### 3.7 Boolean 표현 규칙

참·거짓을 의미하는 값은 JSON과 Query Parameter 모두 Boolean을 사용한다.

JSON에서는 실제 Boolean 값인 `true`, `false`를 사용한다.

```json
{
  "isFavorite": true,
  "careNeeded": false
}
```

다음과 같이 숫자나 문자열로 표현하지 않는다.

```json
{
  "isFavorite": 1,
  "careNeeded": "Y"
}
```

다음과 같은 문자열 Boolean도 사용하지 않는다.

```json
{
  "isFavorite": "true"
}
```

Query Parameter에서도 `true`, `false`를 사용한다.

```http
GET /api/my-items?careNeeded=true
```

Boolean 필드명은 의미가 명확하게 드러나도록 작성한다.

예:

```text
isFavorite
isSaved
careNeeded
hasNext
hasPrevious
preferenceCompleted
```

단, 실제 필드명은 각 기능 API를 설계할 때 도메인 의미에 맞게 최종 확정한다.

Boolean 값 자체가 아직 정해지지 않은 상태를 표현해야 하는 특별한 경우가 아니라면 Boolean 필드에 `null`을 사용하지 않는다.

### 3.8 숫자·점수·횟수 표현 규칙

API에서 숫자 데이터를 표시용 문자열로 가공하지 않고 숫자 자료형으로 전달한다.

기본 규칙은 다음과 같다.

| 의미 | 표현 | 예시 |
| --- | --- | --- |
| 횟수 | 0 이상의 정수 | `3` |
| 개수 | 0 이상의 정수 | `5` |
| 적합도·백분율 점수 | `0` 이상 `100` 이하의 정수 | `87` |
| 원화 금액 | 원 단위 정수 | `1250000` |
| 페이지 번호 | 0 이상의 정수 | `0` |
| 페이지 크기 | `1` 이상 `100` 이하의 정수 | `20` |

예:

```json
{
  "matchScore": 87,
  "monthlyUsageCount": 3,
  "totalUsageCount": 12,
  "purchasePrice": 1250000
}
```

프론트에서 표시할 때 필요한 `%`, `회`, `원` 등의 단위는 API 값에 포함하지 않는다.

비권장:

```json
{
  "matchScore": "87%",
  "monthlyUsageCount": "3회",
  "purchasePrice": "1,250,000원"
}
```

권장:

```json
{
  "matchScore": 87,
  "monthlyUsageCount": 3,
  "purchasePrice": 1250000
}
```

화면 표시용 문자열은 프론트에서 변환한다.

```ts
`${matchScore}%`;
`${monthlyUsageCount}회`;
`${purchasePrice.toLocaleString("ko-KR")}원`;
```

활용도처럼 단순한 숫자보다 단계 자체가 의미를 가지는 값은 Enum을 사용할 수 있다.

예:

```json
{
  "utilizationLevel": "LOW"
}
```

```text
LOW
MEDIUM
HIGH
```

단, 실제 활용도 계산 방식과 Enum 값은 해당 기능의 도메인 정책을 확정할 때 정한다.

소수점이 필요한 값이 새로 생기는 경우에는 해당 API에서 허용 범위와 소수 자릿수를 별도로 정의한다.

### 3.9 정상적인 결과 없음·데이터 부족 상태 규칙

검색, 추천, 분석 결과가 없거나 계산에 필요한 데이터가 부족한 상황은 요청 자체가 정상적으로 처리되었다면 오류로 처리하지 않는다.

다음과 같은 상황은 정상적인 사용자 흐름에 포함될 수 있다.

```text
추천 제품이 없음
검색·필터 조건에 맞는 항목이 없음
추천 장소가 없음
함께 활용할 마이아이템이 없음
사용 기록이 부족함
활용도 분석에 필요한 데이터가 부족함
스마트 착용 추천 대상이 없음
```

목록 조회 결과가 없는 경우에는 `200 OK`와 빈 배열을 반환한다.

```json
{
  "success": true,
  "data": []
}
```

페이지네이션 목록이면 기존 페이지 응답 구조를 유지하고 `items`를 빈 배열로 반환한다.

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

단순한 빈 목록보다 결과 상태 자체를 프론트에서 구분해야 하는 기능은 정상 응답 데이터 안에 상태를 포함할 수 있다.

예:

```json
{
  "success": true,
  "data": {
    "resultStatus": "INSUFFICIENT_DATA",
    "items": []
  }
}
```

상태값은 영문 대문자 `SNAKE_CASE` Enum으로 표현한다.

예:

```text
AVAILABLE
NO_MATCH
INSUFFICIENT_DATA
```

단, 실제 상태값은 각 추천·분석 기능을 설계할 때 필요한 범위만 정의한다.

다음 상황은 정상적인 결과 없음과 구분한다.

- 특정 ID로 조회한 리소스 자체가 존재하지 않음 → `404 Not Found`
- 요청 값 또는 형식이 잘못됨 → `400 Bad Request`
- 인증되지 않은 사용자 → `401 Unauthorized`
- 처리되지 않은 서버 오류 → `500 Internal Server Error`

즉, "결과가 없음"과 "요청 처리 실패"를 동일한 오류로 취급하지 않는다.

### 3.10 찜·저장 등 상태 변경 API 규칙

찜, 저장처럼 하나의 상태를 설정하거나 해제하는 기능은 `toggle` 동작보다 원하는 최종 상태가 명확하게 드러나는 API를 사용한다.

다음과 같이 현재 상태를 반대로 전환하는 Endpoint는 사용하지 않는다.

```http
POST /api/products/{productId}/toggle-favorite
POST /api/places/{placeId}/toggle-save
```

대신 상태 설정과 해제를 구분한다.

예:

```http
PUT    /api/products/{productId}/favorite
DELETE /api/products/{productId}/favorite

PUT    /api/places/{placeId}/saved
DELETE /api/places/{placeId}/saved
```

구체적인 Endpoint 이름은 각 도메인 API를 설계할 때 최종 확정하되, 같은 요청을 반복해도 최종 상태가 달라지지 않도록 설계하는 것을 기본으로 한다.

예를 들어 이미 찜한 제품에 찜 설정 요청을 다시 보내더라도 최종 상태는 계속 찜 상태여야 한다.

```text
PUT /api/products/{productId}/favorite
→ 최종 상태: 찜

같은 요청 다시 실행
→ 최종 상태: 여전히 찜
```

해제 요청도 같은 기준을 따른다.

```text
DELETE /api/products/{productId}/favorite
→ 최종 상태: 찜 아님
```

응답에 현재 상태를 반환할 필요가 있는 경우 Boolean 규칙에 따라 표현한다.

```json
{
  "success": true,
  "data": {
    "isFavorite": true
  }
}
```

반환할 데이터가 없는 경우에는 `204 No Content`를 사용할 수 있으며, 이 경우 응답 본문을 보내지 않는 기존 규칙을 따른다.

제품 찜과 장소 저장처럼 의미가 다른 상태의 실제 필드명과 Endpoint는 각 도메인 API에서 별도로 정의한다.

### 3.11 하위 리소스 URL 규칙

특정 리소스에 종속되어 생성·조회되는 데이터는 부모 리소스 아래의 하위 리소스로 표현할 수 있다.

하위 리소스를 조회·수정·삭제할 때는 해당 하위 리소스가 URL에 지정된 부모 리소스에 실제로 속하는지도 함께 확인한다.

현재 프로젝트에서 대표적인 예는 마이아이템의 사용 기록과 관리 기록이다.

```text
마이아이템
├─ 사용 기록
└─ 관리 기록
```

이 경우 다음과 같이 부모 리소스의 ID를 경로에 포함한다.

```http
GET  /api/my-items/{myItemId}/usage-records
POST /api/my-items/{myItemId}/usage-records

GET  /api/my-items/{myItemId}/care-records
POST /api/my-items/{myItemId}/care-records
```

특정 기록 하나를 조회·수정·삭제해야 하는 경우에는 해당 하위 리소스의 ID를 추가한다.

예:

```http
GET    /api/my-items/{myItemId}/usage-records/{usageRecordId}
PATCH  /api/my-items/{myItemId}/usage-records/{usageRecordId}
DELETE /api/my-items/{myItemId}/usage-records/{usageRecordId}
```

```http
GET    /api/my-items/{myItemId}/care-records/{careRecordId}
PATCH  /api/my-items/{myItemId}/care-records/{careRecordId}
DELETE /api/my-items/{myItemId}/care-records/{careRecordId}
```

다음처럼 동작을 URL에 직접 표현하는 방식은 사용하지 않는다.

```http
POST /api/addUsageRecord
POST /api/createCareRecord
POST /api/my-items/{myItemId}/add-usage
```

부모와의 관계가 명확하지 않거나 여러 부모 리소스에서 독립적으로 조회해야 하는 데이터는 반드시 하위 리소스로 만들 필요가 없다.

하위 리소스 구조를 지나치게 깊게 만들지 않는다.

```text
권장 예시

/api/my-items/{myItemId}/usage-records/{usageRecordId}
```

다음처럼 불필요하게 여러 단계로 중첩하는 구조는 피한다.

```text
/api/users/{userId}/my-items/{myItemId}/usage-records/{usageRecordId}/details
```

실제 Endpoint 구조는 각 도메인 API를 설계할 때 데이터 관계와 조회 방식을 기준으로 최종 확정한다.

### 3.12 Path Variable·Query Parameter Validation 규칙

Request Body뿐만 아니라 Path Variable과 Query Parameter도 각 API에서 허용하는 형식과 범위를 검증한다.

잘못된 요청값은 서버 내부 오류로 처리하지 않고 `400 Bad Request`로 반환한다.

대표적인 검증 대상은 다음과 같다.

| 대상 | 잘못된 요청 예시 |
| --- | --- |
| 페이지 번호 | `page=-1` |
| 페이지 크기 | `size=0`, `size=101` |
| 금액 | `minPrice=-100` |
| 금액 범위 | `minPrice`가 `maxPrice`보다 큼 |
| Enum | 지원하지 않는 `category`, `style`, `placeType` |
| Boolean | `careNeeded=abc` |
| 날짜 | `2026-99-99`처럼 잘못된 날짜 |
| 정렬 | 지원하지 않는 정렬 필드 또는 방향 |
| Path Variable | 허용하지 않는 형식의 ID |

예:

```http
GET /api/products?page=-1&size=200
```

```http
GET /api/products?minPrice=1500000&maxPrice=500000
```

```http
GET /api/products?category=UNKNOWN_CATEGORY
```

위와 같은 요청은 정상 조회 결과가 없는 상황과 다르므로 빈 목록을 반환하지 않고 `400 Bad Request`로 처리한다.

요청 필드를 특정할 수 있는 Validation 오류는 기존 `VALIDATION_ERROR` 형식을 사용한다.

예:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "입력값을 확인해 주세요.",
    "fields": [
      {
        "field": "page",
        "reason": "page는 0 이상이어야 합니다."
      },
      {
        "field": "size",
        "reason": "size는 1 이상 100 이하여야 합니다."
      }
    ]
  }
}
```

Query Parameter도 `field` 이름을 실제 API Parameter 이름과 동일하게 작성한다.

예:

```text
page
size
minPrice
maxPrice
category
usedDate
careNeeded
```

여러 필드의 조합 자체가 잘못된 경우에도 가능한 한 관련 필드를 식별할 수 있도록 처리한다.

예를 들어 다음 조건은 허용하지 않는다.

```text
minPrice > maxPrice
```

날짜·Enum·Boolean 등 요청 문자열을 해당 자료형으로 변환할 수 없는 경우에도 `500 Internal Server Error`가 아니라 클라이언트 요청 오류인 `400 Bad Request`로 처리한다.

지원하지 않는 정렬 필드를 전달한 경우에도 `400 Bad Request`를 반환한다.

```http
GET /api/products?sort=unknownField,desc
```

Path Variable의 형식이 올바르지만 해당 ID의 실제 리소스가 존재하지 않는 경우에는 Validation 오류가 아니라 `404 Not Found`로 처리한다.

즉 다음 두 상황을 구분한다.

```text
요청값의 형식·범위가 잘못됨
→ 400 Bad Request

올바른 형식의 ID이지만 해당 리소스가 존재하지 않음
→ 404 Not Found
```

### 3.13 시연용·샘플 데이터 표시 규칙

해커톤 시연을 위해 사용하는 샘플 제품, 장소 등 실제 상용 데이터가 아닌 정보는 프론트가 이를 구분할 수 있도록 응답에서 샘플 여부를 명확하게 전달한다.

대표적으로 다음과 같은 데이터가 대상이 될 수 있다.

```text
추천 제품
제품의 가격·소재 등 제품 정보
추천 장소
장소의 설명·대표 이미지 등 시연용 정보
```

실제 데이터와 샘플 데이터의 구분이 필요한 리소스에서는 `isSample` Boolean 필드를 사용하여 샘플 여부를 명확하게 전달한다.

예:

```json
{
  "productId": "123",
  "name": "MCM 토트백",
  "price": 1250000,
  "material": "LEATHER",
  "isSample": true
}
```

장소 데이터도 같은 기준을 사용할 수 있다.

```json
{
  "placeId": "82",
  "name": "성수 카페",
  "placeType": "CAFE",
  "isSample": true
}
```

`isSample`은 Boolean 표현 규칙에 따라 실제 Boolean 값인 `true`, `false`를 사용한다.

```json
{
  "isSample": true
}
```

다음처럼 문자열이나 숫자로 표현하지 않는다.

```json
{
  "isSample": "Y"
}
```

```json
{
  "isSample": 1
}
```

샘플 데이터 여부를 프론트에서 추측하도록 만들지 않는다.

예를 들어 특정 ID 범위, 제품명, 이미지 URL 등을 기준으로 프론트가 샘플 여부를 판단하지 않는다.

```text
비권장

productId가 1000 미만이면 샘플로 판단
제품명에 "샘플"이 포함되어 있으면 샘플로 판단
특정 이미지 URL이면 샘플로 판단
```

샘플 데이터를 사용하는 화면에서는 프론트가 `isSample` 값을 기준으로 필요한 안내 문구를 표시할 수 있다.

단, 모든 API 응답에 `isSample`을 의무적으로 포함하지 않는다. 실제 데이터와 샘플 데이터의 구분이 필요한 리소스에만 적용한다.

사용자가 직접 등록한 마이아이템이나 사용·관리 기록처럼 사용자 입력으로 생성된 데이터는 일반적으로 샘플 데이터로 취급하지 않는다.

향후 실제 외부 데이터와 여러 종류의 시연 데이터를 세부적으로 구분해야 하는 요구가 생기는 경우에는 `dataSource`와 같은 별도의 Enum 필드를 도입할 수 있다. 해당 구분이 필요해질 때 API 계약에서 별도로 정의한다.

---

## 4. 성공 응답 형식

### 4.1 성공·오류 필드 포함 규칙

- 응답 본문이 있는 성공 응답에는 `success`와 `data`를 포함한다.
- 응답 본문이 있는 성공 응답에는 `error`를 포함하지 않는다.
- 오류 응답에는 `success`와 `error`를 포함한다.
- 오류 응답에는 `data`를 포함하지 않는다.
- `fields`는 Validation 오류가 발생한 경우에만 `error` 내부에 포함한다.
- 사용하지 않는 최상위 필드를 `null`로 보내지 않고 응답에서 생략한다.

성공 응답:

```json
{
  "success": true,
  "data": {}
}
```

다음처럼 `error: null`을 함께 보내지 않는다.

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

### 4.2 단일 객체

```json
{
  "success": true,
  "data": {
    "productId": "123",
    "name": "MCM 가방",
    "price": 1250000
  }
}
```

### 4.3 페이지네이션을 사용하지 않는 작은 목록

페이지네이션을 사용하지 않는 작은 목록은 `data`에 배열을 직접 담는다.

```json
{
  "success": true,
  "data": [
    {
      "code": "CASUAL",
      "label": "캐주얼"
    }
  ]
}
```

### 4.4 작은 목록의 조회 결과가 없는 경우

조회 결과가 없더라도 오류로 처리하지 않고 빈 배열을 반환한다.

```json
{
  "success": true,
  "data": []
}
```

- `null` 대신 `[]`을 반환한다.
- 단순히 검색·추천 결과가 없다는 이유로 `404 Not Found`를 반환하지 않는다.
- 특정 ID로 조회한 하나의 리소스가 존재하지 않는 경우에는 `404 Not Found`를 사용한다.

### 4.5 생성 성공

새로운 리소스를 생성한 경우 `201 Created`를 사용한다.

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

### 4.6 응답 본문이 없는 성공

삭제나 찜 해제처럼 반환할 데이터가 없다면 `204 No Content`를 사용하고 응답 본문을 보내지 않는다. `204` 응답에는 `success`, `data`를 포함한 JSON 본문도 함께 보내지 않는다.

### 4.7 HTTP 상태 코드

| 상황                             |                   상태 코드 |
| -------------------------------- | --------------------------: |
| 조회·수정 성공                   |                    `200 OK` |
| 정상적인 빈 목록·추천 결과       |                    `200 OK` |
| 생성 성공                        |               `201 Created` |
| 성공했지만 반환할 본문 없음      |            `204 No Content` |
| 잘못된 요청·Validation 실패      |           `400 Bad Request` |
| 인증 필요                        |          `401 Unauthorized` |
| 권한 없음                        |             `403 Forbidden` |
| 특정 ID의 리소스 없음            |             `404 Not Found` |
| 중복 또는 현재 상태와 충돌       |              `409 Conflict` |
| 서버 내부 오류                   | `500 Internal Server Error` |

---

## 5. 페이지네이션 규칙

### 5.1 요청 형식

목록이 계속 늘어날 수 있는 API는 기본적으로 페이지네이션을 적용한다.

```http
GET /api/products?page=0&size=20&sort=createdAt,desc
```

| 파라미터 | 규칙                                   |           기본값 |
| -------- | -------------------------------------- | ---------------: |
| `page`   | `0`부터 시작한다. 첫 페이지는 `0`이다. |              `0` |
| `size`   | `1` 이상 `100` 이하의 한 페이지 항목 수이다. | `20` |
| `sort`   | `필드명,정렬방향` 형식이며 기본 정렬은 API별로 정의한다. | API별 정의 |

- 기본 정렬 필드와 방향은 각 목록 API의 성격에 맞게 별도로 정의한다.
- 정렬 방향은 `asc` 또는 `desc`만 사용한다.
- 여러 정렬 조건이 필요하면 `sort`를 반복해서 보낼 수 있다.
- 지원하지 않는 정렬 필드는 `400 Bad Request`로 처리한다.

```http
GET /api/products?page=0&size=20&sort=status,asc&sort=createdAt,desc
```

### 5.2 페이지 응답 형식

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

조회 결과가 없을 때도 `items`는 `null`이 아니라 빈 배열을 반환한다.

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

프론트 화면에서 사용자에게 보이는 페이지 번호는 필요하면 `page + 1`로 표시한다. API 요청과 응답의 `page` 값은 항상 0부터 시작한다.

### 5.3 페이지네이션 적용·비적용 기준

모든 배열 응답에 페이지네이션을 적용하지 않는다.

데이터가 사용자 활동에 따라 계속 누적되거나 개수가 커질 가능성이 있는 목록에는 페이지네이션을 적용하는 것을 기본으로 한다.

대표적인 적용 후보는 다음과 같다.

```text
마이아이템 목록
사용 기록 목록
관리 기록 목록
찜한 제품 목록
저장한 스타일 플랜 목록
저장한 장소 목록
```

실제 페이지네이션 적용 여부는 각 기능 API를 설계할 때 데이터 증가 가능성과 화면 사용 방식을 기준으로 최종 결정한다.

반대로 결과 개수가 작고 기능상 일정한 범위 안에서만 반환되는 목록에는 페이지네이션을 강제하지 않는다.

대표적인 비적용 후보는 다음과 같다.

```text
소수의 추천 결과
스타일 조합 후보
선택 옵션 목록
추천 이유 목록
Enum 기반 옵션 목록
```

페이지네이션을 사용하지 않는 작은 목록은 기존 성공 응답 규칙에 따라 `data`에 배열을 직접 반환한다.

예:

```json
{
  "success": true,
  "data": [
    {
      "code": "CASUAL",
      "label": "캐주얼"
    },
    {
      "code": "MINIMAL",
      "label": "미니멀"
    }
  ]
}
```

결과가 없으면 `null`이 아니라 빈 배열을 반환한다.

```json
{
  "success": true,
  "data": []
}
```

단순히 구현을 통일하기 위한 목적으로 작은 고정 목록에 불필요한 페이지네이션 구조를 적용하지 않는다.

반대로 데이터가 지속적으로 증가할 수 있는 목록을 한 번에 모두 반환하지 않는다.

---

## 6. 일반 오류 응답 형식

```json
{
  "success": false,
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "제품을 찾을 수 없습니다."
  }
}
```

- 오류 응답에는 `success`와 `error`를 포함하고 `data`는 포함하지 않는다.
- 사용하지 않는 `data`를 `null`로 보내지 않고 응답에서 생략한다.
- `code`는 프론트엔드가 오류 종류를 구분할 때 사용하는 고정된 영문 코드이다.
- `message`는 사용자 안내 또는 개발 중 확인을 위한 안전한 설명이다.
- 프론트엔드는 `message` 문자열을 비교하지 않고 `code`를 기준으로 분기한다.
- 서버의 예외 메시지, SQL, 파일 경로, 스택 트레이스 등 내부 정보는 응답에 노출하지 않는다.

```ts
if (error.code === "PRODUCT_NOT_FOUND") {
  // 제품 없음 화면 표시
}
```

오류 코드는 대문자 `SNAKE_CASE`를 사용하며, 가능한 한 `대상_원인` 형태로 작성한다.

```text
PRODUCT_NOT_FOUND
MY_ITEM_NOT_FOUND
VERIFICATION_CODE_INVALID
VERIFICATION_CODE_EXPIRED
FILE_SIZE_EXCEEDED
INTERNAL_SERVER_ERROR
```

요청 본문 자체가 없거나 JSON 문법이 잘못된 경우에는 `fields` 없이 일반 오류 형식을 사용한다.

```json
{
  "success": false,
  "error": {
    "code": "REQUEST_BODY_INVALID",
    "message": "요청 본문 형식을 확인해 주세요."
  }
}
```

처리되지 않은 서버 내부 오류도 내부 예외 내용을 노출하지 않고 공통 형식으로 반환한다.

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

## 7. Validation 오류 형식

입력값 검증이 실패하면 `400 Bad Request`와 함께 잘못된 필드를 배열로 반환한다.

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
        "field": "verificationCode",
        "reason": "인증번호는 6자리여야 합니다."
      }
    ]
  }
}
```

- `fields`는 Validation 오류가 발생한 경우에만 `error` 내부에 포함한다.
- `field`는 실제 요청에서 사용하는 필드명, Query Parameter명 또는 Path Variable명과 일치시킨다.
- 이메일 인증 요청이면 `email`, 휴대폰 인증 요청이면 `phoneNumber`처럼 실제 요청 필드명을 사용한다.
- 한 필드에 오류가 여러 개여도 우선순위가 가장 높은 오류 하나만 반환한다.
- 여러 필드가 잘못되었다면 가능한 한 한 번에 모두 반환한다.
- Validation 오류가 아닌 일반 오류에는 `fields`를 빈 배열로 넣지 않고 필드 자체를 생략한다.
- 요청 본문 자체가 없거나 JSON 문법이 잘못된 경우에는 `fields` 없이 일반 오류 형식을 사용한다.

---

## 8. 날짜·시간과 기준 시간대

### 8.1 형식

ISO 8601 형식으로 통일한다.

| 데이터 종류        | 형식          | 예시                   |
| ------------------ | ------------- | ---------------------- |
| 특정 시각          | UTC 날짜·시간 | `2026-08-05T07:30:00Z` |
| 날짜만 의미하는 값 | `YYYY-MM-DD`  | `2026-08-05`           |
| 시간만 의미하는 값 | `HH:mm:ss`    | `16:30:00`             |

`2026/08/05`, `08-05-2026`, `2026년 8월 5일`처럼 화면 표시용으로 가공한 값을 API에서 보내지 않는다.

### 8.2 시간대

- 서버와 DB 저장: UTC
- API 요청·응답: UTC
- 프론트 화면 표시: `Asia/Seoul`
- 생일, 행사일 등 날짜 자체만 의미하는 값: 시간대 변환 없이 `YYYY-MM-DD`

예를 들어 API가 `2026-08-05T07:30:00Z`를 반환하면 프론트는 한국 시간 `2026-08-05 16:30`으로 표시한다.

### 8.3 백엔드 Java 타입

| 의미 | Java 타입 | API 형식 |
| --- | --- | --- |
| 날짜만 의미하는 값 | `LocalDate` | `YYYY-MM-DD` |
| 시간만 의미하는 값 | `LocalTime` | `HH:mm:ss` |
| 정확한 시각 | `Instant` | UTC ISO 8601 |

예:

- 스타일 플랜 날짜 → `LocalDate`
- 구매일 → `LocalDate`
- 사용 기록 날짜 → `LocalDate`
- 관리 기록 날짜 → `LocalDate`
- 다음 관리 예정일 → `LocalDate`
- 스타일 플랜 시간 → `LocalTime`
- 생성 시각 → `Instant`
- 수정 시각 → `Instant`

정확한 시각을 저장하기 위해 `LocalDateTime`을 사용하지 않는다.
`java.util.Date`, `Calendar`, `Timestamp`도 신규 도메인 코드에서는 사용하지 않는다.

날짜와 시간을 화면 표시용 `String`으로 백엔드에서 가공하지 않는다.

### 8.4 생성·수정 시각

Entity의 생성·수정 시각은 다음 기준을 사용한다.

- Java 타입: `Instant`
- DB 저장 기준: UTC
- 필드명: `createdAt`, `updatedAt`
- DB 컬럼명: `created_at`, `updated_at`
- 생성 시각은 최초 저장 시 자동 기록한다.
- 수정 시각은 Entity 변경 시 자동 갱신한다.
- API에 노출하는 경우 UTC ISO 8601 형식으로 전달한다.
- 모든 응답 DTO에 생성·수정 시각을 의무적으로 포함하지 않는다.

---

## 9. Enum 표현

Enum은 영문 대문자 `SNAKE_CASE`로 전달한다.

```json
{
  "status": "IN_PROGRESS",
  "style": "STREET_CASUAL",
  "season": "SPRING"
}
```

사용자에게 보일 한글 문구는 프론트에서 변환한다.

```ts
const statusLabel = {
  IN_PROGRESS: "진행 중",
  COMPLETED: "완료",
} as const;
```

---

## 10. 응답의 `null`, 빈 배열, 빈 문자열

이 절은 백엔드가 프론트에 반환하는 **응답 필드**의 규칙이다. 요청 필드의 생략·`null` 규칙은 3.3과 3.4를 따른다.

| 상황                               | 반환값                                         |
| ---------------------------------- | ---------------------------------------------- |
| 선택값이 아직 없거나 설정되지 않음 | `null`                                         |
| 목록에 항목이 없음                 | `[]`                                           |
| 문자열 입력이 비어 있음            | 빈 문자열 자체가 의미 있을 때만 `""`           |
| 필드가 API 계약에 있지만 값이 없음 | 필드를 생략하지 않고 `null` 또는 정해진 기본값 |

```json
{
  "nickname": null,
  "profileImageUrl": null,
  "items": []
}
```

- 배열은 항상 배열로 반환하여 프론트에서 바로 `map`, `filter` 등을 사용할 수 있게 한다.
- 선택 필드가 응답마다 사라지지 않게 하여 프론트 타입을 안정적으로 유지한다.
- 공백 문자열을 `null` 대신 사용하지 않는다.
- 단, 성공·오류 공통 응답에서 사용하지 않는 최상위 `data` 또는 `error`는 `null`로 보내지 않고 생략한다.

---

## 11. ID와 금액 자료형

### 11.1 ID

DB에서 숫자로 저장하더라도 API 요청과 응답에서는 문자열로 전달한다.

```json
{
  "productId": "1234567890123456789",
  "myItemId": "987654321"
}
```

API 필드명은 `id` 단독 사용보다 `productId`, `myItemId`, `placeId`처럼 대상 리소스가 드러나는 이름을 우선 사용한다.

JavaScript의 안전한 정수 범위를 넘는 ID가 숫자로 전달될 때 값이 달라질 수 있으므로, 모든 ID를 문자열로 통일한다.

### 11.2 금액

현재 MVP의 금액은 `KRW`를 기준으로 하며, 원화 금액은 원 단위 정수로 전달한다.

```json
{
  "price": 1250000
}
```

구매 가격도 같은 기준을 사용한다.

```json
{
  "purchasePrice": 1250000
}
```

현재 MVP에서는 `currency` 필드를 매 요청·응답마다 포함하지 않는다. 향후 다중 통화를 지원하게 되는 경우에만 `currency` 필드를 추가한다.

API에서 `"1,250,000원"`처럼 표시용 문자열을 보내지 않는다. 쉼표와 통화 표시는 프론트에서 처리한다.

```ts
`${price.toLocaleString("ko-KR")}원`;
```

---

## 12. API 변경의 분류

변경 전에 프론트 영향도를 기준으로 다음과 같이 분류한다.

### 12.1 호환 가능한 변경

- 응답에 새로운 선택 필드 추가
- 새로운 API Endpoint 추가
- 새로운 선택 Query Parameter 추가
- 기존 동작을 바꾸지 않는 문서·설명 수정

### 12.2 호환성이 깨지는 변경

- Endpoint 또는 HTTP Method 변경
- 기존 요청·응답 필드의 삭제 또는 이름 변경
- 필드 자료형 변경
- 필수 요청 필드 추가
- Enum 값의 삭제 또는 이름 변경
- `null` 가능 여부 변경
- 페이지네이션 구조 또는 시작 번호 변경
- 기존 상태 코드나 오류 코드의 의미 변경

---

## 13. 분리된 프론트·백엔드 저장소에서 API 변경 공유 방법


프론트가 원하는 응답 구조가 있다고 해서 프론트 저장소에서 API 계약을 먼저 확정하지 않는다.

1. 프론트에서 백엔드 저장소에 API 변경 요청 Issue를 생성한다.
2. 현재 불편한 점과 원하는 요청·응답 예시를 작성한다.
3. 백엔드 담당자와 변경 가능 여부 및 형식을 합의한다.
4. 백엔드 담당자는 해당 협의안을 코드에 적용하고 PR을 생성한다.
5. 프론트는 확정된 백엔드 PR을 기준으로 구현한다.

Issue 제목 예시:

```text
[API 요청] 제품 목록 응답에 대표 이미지 URL 추가
```

기존 프론트가 바로 깨지는 변경이라면 백엔드가 잠시 이전 필드와 새 필드를 함께 제공하는 방식이 가장 안전하다.(배포 후 참고)

```json
{
  "image": "https://example.com/old.jpg",
  "imageUrl": "https://example.com/new.jpg"
}
```

프론트 전환과 배포가 끝난 뒤 별도 백엔드 PR에서 이전 필드를 제거한다.

---

## 14. API 변경 PR 템플릿

백엔드 저장소의 `.github/pull_request_template.md` 또는 API 변경 PR 본문에 다음 양식을 사용한다.

````md
## 변경 유형

- [ ] 새로운 API 추가
- [ ] 호환 가능한 변경
- [ ] 호환성이 깨지는 변경
- [ ] 문서만 변경

## 대상 API

- Method: `GET`
- Path: `/api/products`

## 변경 이유

제품 목록 데이터가 많아질 때 전체 데이터를 한 번에 불러오는 문제를 막기 위해 페이지네이션을 적용합니다.

## 변경 전

```json
{
  "success": true,
  "data": []
}
```

## 변경 후

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

## 요청 규칙

- `page`: 0부터 시작, 기본값 0
- `size`: 기본값 20, 최댓값 100
- `sort`: `필드명,asc|desc`

## 프론트 영향

- 목록 타입을 배열에서 페이지 객체로 변경해야 합니다.
- `data.items`를 기준으로 렌더링해야 합니다.
- 페이지 이동 시 `page`, `size`를 Query Parameter로 전달해야 합니다.

## 오류 및 상태 코드

- `200`: 조회 성공
- `400`: 잘못된 페이지·정렬 조건

## 관련 작업

- Backend Issue: #번호
- Frontend Issue/PR: 상대 저장소 URL 또는 `없음`

## 적용 및 배포 순서

1. 백엔드 개발 서버 배포
2. 프론트 연동 확인
3. 프론트 병합 및 배포

## 확인 체크리스트

- [ ] 프론트 담당자가 변경 내용을 확인했습니다.
- [ ] 기존 API 사용자에게 미치는 영향을 확인했습니다.
- [ ] 테스트와 CI가 통과했습니다.
- [ ] 팀 채널에 PR 링크와 적용 환경을 공유했습니다.

````

### PR 제목 규칙

```text
[API] 제품 목록 페이지네이션 적용
[API] 추천 결과에 reason 필드 추가
[API][Breaking] image 필드를 imageUrl로 변경
```

호환성이 깨지는 변경은 제목에 `[Breaking]`을 추가한다.

---

## 15. 프론트 대응 PR 템플릿

```md
## 관련 API 변경

- Backend PR: 상대 백엔드 저장소 PR URL
- 대상 API: `GET /api/products`

## 프론트 변경 내용

- 페이지 응답 타입을 추가했습니다.
- 제품 목록을 `data.items`로 렌더링하도록 수정했습니다.
- 페이지 이동 시 `page`, `size`를 전달하도록 수정했습니다.

## 연동 환경

- [ ] Mock 데이터
- [ ] 로컬 백엔드
- [ ] 개발 서버

## 확인 항목

- [ ] 첫 페이지 조회
- [ ] 다음·이전 페이지 이동
- [ ] 빈 목록 처리
- [ ] 로딩 처리
- [ ] 일반 오류 처리
- [ ] Validation 오류 처리
```

---

## 16. API 변경 완료 조건

다음 조건을 모두 만족해야 API 변경이 완료된 것으로 본다.

- [ ] 구현 코드가 완료되었다.
- [ ] 성공, 일반 오류, Validation 오류가 공통 형식을 따른다.
- [ ] 성공 응답에는 `error`가, 오류 응답에는 `data`가 포함되지 않는다.
- [ ] Validation 오류가 아닌 경우 `fields`가 포함되지 않는다.
- [ ] 페이지네이션이 필요한 목록에 공통 페이지 형식을 적용했다.
- [ ] 백엔드 테스트와 CI가 통과했다.
- [ ] 프론트 영향도를 PR에 작성했다.
- [ ] 필요한 프론트 Issue/PR과 상호 링크했다.
- [ ] 프론트 담당자가 개발 환경에서 연동을 확인했다.
- [ ] 병합·배포 순서가 필요한 경우 양쪽 담당자가 확인했다.

---
