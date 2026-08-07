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
| 값 없음         | 단일 값은 `null`, 목록은 `[]`을 반환한다.                                  |
| ID              | API에서는 문자열로 전달한다.                                               |
| 원화 금액       | 원 단위 정수로 전달한다.                                                   |
| 목록 조회       | `page`, `size`, `sort` 기반 페이지네이션을 사용한다.                       |

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

GET /api/style-recommendations?page=0&size=20&sort=createdAt,desc
```

다음과 같이 동사를 URL에 중복해서 쓰는 방식은 피한다.

```http
POST /api/createProduct
GET  /api/getProducts
```

---

## 4. 성공 응답 형식

### 4.1 성공·오류 필드 포함 규칙

- 성공 응답에는 `success`와 `data`를 포함한다.
- 성공 응답에는 `error`를 포함하지 않는다.
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
    "price": 1250000,
    "currency": "KRW"
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
| `size`   | 한 페이지의 항목 수이다.               |             `20` |
| `sort`   | `필드명,정렬방향` 형식이다.            | `createdAt,desc` |

- `size`의 최댓값은 `100`으로 제한한다.
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
        "id": "123",
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
- `field`는 프론트 요청 DTO의 필드명과 정확히 일치시킨다.
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

## 10. `null`, 빈 배열, 빈 문자열

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
  "id": "1234567890123456789",
  "productId": "987654321"
}
```

JavaScript의 안전한 정수 범위를 넘는 ID가 숫자로 전달될 때 값이 달라질 수 있으므로, 모든 ID를 문자열로 통일한다.

### 11.2 금액

원화 금액은 원 단위 정수와 통화 코드를 함께 전달한다.

```json
{
  "price": 1250000,
  "currency": "KRW"
}
```

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

---

프론트가 원하는 응답 구조가 있다고 해서 프론트 저장소에서 API 계약을 먼저 확정하지 않는다.

1. 프론트에서 백엔드 저장소에 API 변경 요청 Issue를 생성한다.
2. 현재 불편한 점과 원하는 요청·응답 예시를 작성한다.
3. 백엔드 담당자와 변경 가능 여부 및 형식을 합의한다.
4. 백엔드 담당자는 해당 협의안을 코드에 적용한다.(PR 또는 직접 작성)
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

이후 카톡 또는 깃허브 PR로 변경했다고 알려주기.

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