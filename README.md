# Hackathon BE

멋쟁이사자처럼 중앙해커톤을 위한 Spring Boot 기반 Backend 프로젝트입니다.

백엔드 담당: 정승원, 장재준

## 기술 스택과 선택 이유

| 기술 | 선택 이유 |
| --- | --- |
| Java 21 | LTS 버전을 사용해 팀원의 로컬 개발환경과 빌드 환경을 통일합니다. Gradle Java Toolchain에서도 Java 21을 사용하도록 설정돼 있습니다. |
| Spring Boot 4.1.0 | 웹 서버, 의존성 관리, 데이터베이스 연결 등 백엔드 애플리케이션의 기본 구성을 빠르게 구축하기 위해 사용합니다. |
| Gradle 9.5.1 | 프로젝트 빌드와 의존성 관리를 담당합니다. Gradle Wrapper를 저장소에 포함해 팀원이 별도로 Gradle을 설치하지 않아도 같은 버전으로 빌드할 수 있습니다. |
| Spring Web MVC | JSON 기반 REST API와 Controller를 구현하고, 내장 Tomcat을 이용해 HTTP 서버를 실행하기 위해 사용합니다. |
| Spring Data JPA | Entity와 Repository를 통해 MySQL 데이터를 객체 중심으로 저장하고 조회하기 위해 사용합니다. |
| Hibernate | JPA 구현체로 사용되며 Entity와 데이터베이스 테이블 간의 매핑을 처리합니다. |
| MySQL | 서비스의 사용자·콘텐츠·추천 결과 등 영속 데이터를 저장하기 위한 관계형 데이터베이스입니다. |
| Validation | 요청 DTO의 필수값, 문자열 길이, 숫자 범위 등을 검증해 잘못된 요청이 비즈니스 로직까지 전달되지 않도록 합니다. |
| JUnit | Controller·Service·Repository와 애플리케이션 실행 여부를 자동으로 검증하기 위해 사용합니다. |
| CORS | `http://localhost:3000`에서 실행되는 프론트엔드가 백엔드의 `/api/**` 경로를 호출할 수 있도록 설정합니다. |

## 현재 구현 상태

| 항목 | 상태 |
| --- | --- |
| Java 21 및 Spring Boot 프로젝트 구성 | 완료 |
| Gradle Wrapper 구성 | 완료 |
| MySQL 연결 설정 | 완료 |
| Spring Data JPA | 완료 |
| Validation | 완료 |
| 기본 상태 확인 API | 완료 |
| 프론트엔드 CORS 설정 | 완료 |
| 실제 도메인 Entity·Repository·Service | 기능 확정 후 구현 |
| 공통 예외 처리 | 추가 예정 |
| Swagger/OpenAPI | 추가 예정 |
| 테스트용 데이터베이스 분리 | 추가 예정 |
| GitHub Actions CI | 추가 예정 |
| 배포 설정 | 추가 예정 |

## 설계 가정

기능 구현 전에 시스템이 정상적으로 동작하기 위해 전제하는 조건을 기록합니다. 가정이 변경되면 관련 코드와 테스트, API 명세도 함께 갱신합니다.

| ID | 설계 가정 | 근거 | 검증 방법 | 가정이 틀릴 때의 대응 | 상태 |
| --- | --- | --- | --- | --- | --- |
| ASM-001 | 로컬 프론트엔드는 `http://localhost:3000`에서 실행됩니다. | Next.js 개발 서버 기본 주소 | 프론트엔드에서 `/api/health` 호출 | 실제 실행 주소를 CORS 허용 목록에 추가 | 승인 |
| ASM-002 | 로컬 백엔드는 `http://localhost:8080`에서 실행됩니다. | Spring Boot 기본 포트 | `/api/health` 접속 | `server.port`와 프론트엔드 API 주소 동시 변경 | 승인 |
| ASM-003 | 개발자는 로컬 MySQL을 사용합니다. | 현재 데이터소스 설정 | Spring Boot 실행 및 DB 연결 로그 확인 | 공용 개발 DB 또는 컨테이너 도입 | 승인 |
| ASM-004 | DB 비밀번호는 Git에 저장하지 않습니다. | 보안 및 팀원별 비밀번호 분리 | `DB_PASSWORD` 환경변수 확인 | 애플리케이션 실행 중단 및 설정 안내 | 승인 |
| ASM-005 | 초기 개발 중 스키마는 JPA의 `ddl-auto=update`로 관리합니다. | 빠른 기능 개발 | Entity 변경 후 테이블 확인 | Flyway 도입 또는 스키마 초기화 | 검토 중 |

추가로 기능 확정 후 결정할 항목:

- 예상 사용자 수와 동시 요청량
- 로그인 및 인증 방식
- 개인정보와 민감정보의 범위
- 외부 AI API 사용 여부
- 외부 API 응답 제한 시간
- 파일 업로드 방식과 최대 크기
- 배포 서버와 운영 데이터베이스
- 데이터 삭제 및 보관 정책

## API 기본 규칙

### 기본 주소

```text
로컬 백엔드: http://localhost:8080
API 기본 경로: /api
```

### URL 작성 기준

- URL은 가능하면 명사와 복수형을 사용합니다.
- 기능별 API는 `/api` 아래에 구성합니다.
- URL에 동작을 표현하기보다 HTTP 메서드로 동작을 구분합니다.

예시:

```http
GET    /api/users
GET    /api/users/{userId}
POST   /api/users
PATCH  /api/users/{userId}
DELETE /api/users/{userId}
```

### JSON 작성 기준

- 요청과 응답의 필드명은 `camelCase`를 사용합니다.
- Entity를 Controller에서 직접 반환하지 않고 응답 DTO를 사용합니다.
- 날짜와 시간은 ISO 8601 형식을 사용합니다.
- 비밀번호, 토큰, 내부 오류 정보는 응답에 포함하지 않습니다.

### HTTP 상태 코드

| 상태 코드 | 사용 기준 |
| --- | --- |
| `200 OK` | 조회·수정 요청 성공 |
| `201 Created` | 새로운 리소스 생성 성공 |
| `204 No Content` | 응답 본문이 필요 없는 삭제 성공 |
| `400 Bad Request` | 입력값 누락 또는 형식 오류 |
| `401 Unauthorized` | 인증이 필요하거나 인증 정보가 유효하지 않음 |
| `403 Forbidden` | 인증됐지만 해당 작업 권한이 없음 |
| `404 Not Found` | 요청한 데이터가 존재하지 않음 |
| `409 Conflict` | 중복 데이터 또는 현재 상태와 충돌 |
| `500 Internal Server Error` | 예상하지 못한 서버 오류 |

## 공통 예외 처리 정책

기능별 Controller에서 서로 다른 오류 형식을 만들지 않도록 공통 예외 처리 기준을 사용합니다.

공통 예외 처리 구현 전까지는 아래 형식을 기준으로 협의합니다.

```json
{
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다.",
  "timestamp": "2026-08-02T23:30:00+09:00"
}
```

| 예외 분류 | 감지 위치 | HTTP 상태 | 처리 기준 |
| --- | --- | --- | --- |
| 요청값 검증 실패 | Controller 진입 전 Validation | 400 | 어떤 필드가 잘못됐는지 안전한 범위에서 전달 |
| 존재하지 않는 데이터 | Service | 404 | 도메인별 오류 코드 반환 |
| 중복 데이터 | Service 또는 DB | 409 | 중복된 대상과 처리 방법 안내 |
| 인증 실패 | Security 도입 후 인증 계층 | 401 | 내부 인증 실패 원인은 노출하지 않음 |
| 권한 부족 | Security 도입 후 인가 계층 | 403 | 허용되지 않은 작업임을 안내 |
| 외부 API 실패 | 외부 API 연동 계층 | 502 또는 503 | 외부 응답 내용을 그대로 사용자에게 노출하지 않음 |
| 예상하지 못한 오류 | 전역 예외 처리기 | 500 | 공통 메시지를 반환하고 서버 로그에 원인 기록 |

공통 원칙:

- [ ] 사용자에게 보여줄 메시지와 내부 오류 정보를 분리합니다.
- [ ] 비밀번호, 토큰, 개인정보를 로그에 기록하지 않습니다.
- [ ] Controller에서 예외를 반복해서 처리하지 않습니다.
- [ ] 예상 가능한 예외와 시스템 오류를 구분합니다.
- [ ] 오류 응답의 `code`와 `message` 형식을 통일합니다.
- [ ] 같은 오류가 중복으로 기록되지 않도록 합니다.
- [ ] 서버 내부 Stack Trace를 API 응답으로 전달하지 않습니다.

## 엣지 케이스 처리 계획

### 데이터베이스

| 시나리오 | 기대 동작 | 처리 기준 | 테스트 방법 |
| --- | --- | --- | --- |
| MySQL 서버가 실행되지 않음 | 애플리케이션 실행 실패 | 연결 오류 로그를 확인하고 MySQL 실행 상태 점검 | MySQL 중지 후 실행 |
| `DB_PASSWORD` 누락 | 애플리케이션 실행 실패 | 환경변수 설정 안내 | 환경변수 제거 후 실행 |
| 존재하지 않는 데이터 조회 | 404 응답 | 도메인 예외 발생 | 존재하지 않는 ID 요청 |
| 중복 데이터 생성 | 409 응답 | DB 제약조건과 Service 검증 사용 | 같은 값으로 두 번 생성 |
| 트랜잭션 중 일부 작업 실패 | 전체 작업 롤백 | Service 계층에 트랜잭션 경계 설정 | 중간 단계에서 예외 발생 |
| 동시에 같은 데이터 수정 | 정책에 맞게 한 요청만 반영 | 필요하면 낙관적 잠금 도입 | 동시 수정 테스트 |

### 사용자 입력

| 시나리오 | 기대 동작 | 처리 기준 |
| --- | --- | --- |
| 필수값 누락 | 400 응답 | `@NotNull`, `@NotBlank` 등 사용 |
| 허용 범위를 벗어난 숫자 | 400 응답 | `@Min`, `@Max` 등 사용 |
| 지나치게 긴 문자열 | 400 응답 | DTO와 DB 컬럼 길이를 함께 제한 |
| 정의되지 않은 Enum 값 | 400 응답 | 허용 가능한 값만 안내 |
| 잘못된 JSON | 400 응답 | 공통 오류 형식으로 반환 |
| 빠른 연속 요청 | 중복 데이터가 생성되지 않도록 처리 | DB 제약조건·중복 검증·필요 시 멱등성 적용 |

### 외부 API 및 네트워크

외부 AI API 등을 도입할 때 아래 항목을 확정합니다.

| 항목 | 결정 내용 |
| --- | --- |
| 연결 타임아웃 | `[외부 API 확정 후 작성]` |
| 응답 타임아웃 | `[외부 API 확정 후 작성]` |
| 최대 재시도 횟수 | `[외부 API 확정 후 작성]` |
| 재시도 간격 | `[외부 API 확정 후 작성]` |
| 재시도 가능한 HTTP 메서드 | `[외부 API 확정 후 작성]` |
| 외부 API 장애 시 대체 동작 | `[외부 API 확정 후 작성]` |
| 요청 제한 초과 처리 | `Retry-After` 헤더와 제공사 정책 확인 |

## 시작하기

### 1. 저장소 Clone

```powershell
git clone https://github.com/pro660/Hackathon_BE.git
cd Hackathon_BE
```

### 2. 버전 확인

Windows PowerShell:

```powershell
java -version
.\gradlew.bat --version
```

기준 버전:

```text
Java: 21
Gradle Wrapper: 9.5.1
Spring Boot: 4.1.0
```

### 3. MySQL 데이터베이스 준비

MySQL에 관리자 계정으로 접속한 뒤 데이터베이스를 만듭니다.

```sql
CREATE DATABASE hackathon_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

`hackathon` 사용자가 없다면 생성합니다.

```sql
CREATE USER 'hackathon'@'localhost'
IDENTIFIED BY '자신의_DB_비밀번호';

GRANT ALL PRIVILEGES
ON hackathon_db.*
TO 'hackathon'@'localhost';

FLUSH PRIVILEGES;
```

이미 `hackathon` 사용자가 있다면 사용자 생성 명령은 다시 실행하지 않아도 됩니다.

각 개발자의 MySQL 비밀번호는 서로 달라도 됩니다. 비밀번호를 GitHub나 코드에 올리지 않습니다.

### 4. 환경변수 설정

현재 PowerShell 창에서만 설정:

```powershell
$env:DB_PASSWORD="자신의_DB_비밀번호"
```

확인:

```powershell
echo $env:DB_PASSWORD
```

Windows 사용자 환경변수로 저장:

```powershell
[Environment]::SetEnvironmentVariable(
    "DB_PASSWORD",
    "자신의_DB_비밀번호",
    "User"
)
```

사용자 환경변수로 저장한 뒤에는 IntelliJ와 PowerShell을 다시 실행해야 할 수 있습니다.

IntelliJ에서는 다음 위치에서도 설정할 수 있습니다.

```text
Run
→ Edit Configurations
→ HackathonBeApplication
→ Environment variables
→ DB_PASSWORD=자신의_DB_비밀번호
```

### 5. 빌드

Windows:

```powershell
.\gradlew.bat clean build
```

macOS 또는 Linux:

```bash
./gradlew clean build
```

성공 결과:

```text
BUILD SUCCESSFUL
```

### 6. 서버 실행

Windows:

```powershell
.\gradlew.bat bootRun
```

macOS 또는 Linux:

```bash
./gradlew bootRun
```

또는 IntelliJ에서 `HackathonBeApplication`을 실행합니다.

### 7. 서버 상태 확인

브라우저 또는 Postman에서 접속합니다.

```http
GET http://localhost:8080/api/health
```

정상 응답:

```json
{
  "status": "ok",
  "message": "Hackathon backend is running"
}
```

## 데이터베이스 설정

현재 개발환경에서는 다음 데이터베이스를 사용합니다.

```text
Host: localhost
Port: 3306
Database: hackathon_db
Username: hackathon
Password: DB_PASSWORD 환경변수
Timezone: Asia/Seoul
Character Encoding: UTF-8
```

현재 JPA 설정:

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.format_sql=true
```

주의사항:

- `ddl-auto=update`는 초기 개발 편의를 위한 설정입니다.
- 운영환경에서는 그대로 사용하지 않습니다.
- 팀원이 Entity를 변경하면 다른 백엔드 팀원에게 공유합니다.
- 데이터베이스 테이블을 임의로 직접 변경하지 않습니다.
- 필요하면 이후 Flyway를 도입해 DB 변경 이력을 관리합니다.

## CORS 설정

현재 다음 프론트엔드 개발 주소를 허용합니다.

```text
http://localhost:3000
```

적용 범위:

```text
/api/**
```

허용 HTTP 메서드:

```text
GET
POST
PUT
PATCH
DELETE
OPTIONS
```

현재는 쿠키 기반 인증을 사용하지 않으므로 자격 증명 전송을 허용하지 않습니다.

```text
allowCredentials: false
```

배포 주소가 정해지면 운영 프론트엔드 주소를 허용 목록에 추가해야 합니다.

## 명령어

Windows PowerShell:

```powershell
.\gradlew.bat clean          # 기존 빌드 결과 삭제
.\gradlew.bat compileJava    # Java 코드 컴파일
.\gradlew.bat test           # 테스트 실행
.\gradlew.bat clean build    # 전체 테스트 및 빌드
.\gradlew.bat bootRun        # 개발 서버 실행
.\gradlew.bat --version      # Gradle과 JVM 정보 확인
```

macOS 또는 Linux:

```bash
./gradlew clean
./gradlew compileJava
./gradlew test
./gradlew clean build
./gradlew bootRun
./gradlew --version
```

## 기본 구조

현재 구조:

```text
src/
├─ main/
│  ├─ java/org/likelionhsu/hackathon/
│  │  ├─ common/
│  │  │  ├─ config/
│  │  │  │  └─ WebConfig.java
│  │  │  └─ controller/
│  │  │     └─ HealthController.java
│  │  └─ HackathonBeApplication.java
│  └─ resources/
│     └─ application.properties
└─ test/
   └─ java/org/likelionhsu/hackathon/
      └─ HackathonBeApplicationTests.java
```

기능 개발 후 권장 구조:

```text
org.likelionhsu.hackathon
├─ common/
│  ├─ config/
│  ├─ exception/
│  └─ response/
├─ user/
│  ├─ controller/
│  ├─ service/
│  ├─ repository/
│  ├─ entity/
│  └─ dto/
└─ [기능명]/
   ├─ controller/
   ├─ service/
   ├─ repository/
   ├─ entity/
   └─ dto/
```

기능 중심 패키지 구조를 사용하고, 기능 내부에서 역할별로 Controller·Service·Repository·Entity·DTO를 분리합니다.

## Git과 브랜치 규칙

본격적인 기능 개발부터 `main` 브랜치에 직접 Push하지 않습니다.

작업 흐름:

```text
main 최신화
→ 작업 브랜치 생성
→ 기능 개발
→ 빌드·테스트
→ Commit
→ Push
→ Pull Request
→ 다른 백엔드 팀원 검토
→ main에 Merge
```

작업 시작:

```powershell
git switch main
git pull origin main
git switch -c feat/기능이름
```

브랜치 이름:

| 접두사 | 용도 | 예시 |
| --- | --- | --- |
| `feat/` | 새로운 기능 | `feat/user-api` |
| `fix/` | 오류 수정 | `fix/user-validation` |
| `chore/` | 환경·설정 작업 | `chore/swagger-config` |
| `refactor/` | 코드 구조 개선 | `refactor/user-service` |
| `test/` | 테스트 추가 | `test/user-service` |
| `docs/` | 문서 수정 | `docs/readme` |

Commit 메시지:

```text
feat: 사용자 생성 API 구현
fix: 사용자 조회 오류 수정
chore: Swagger 설정 추가
refactor: 사용자 서비스 로직 분리
test: 사용자 서비스 테스트 추가
docs: README 실행 방법 보완
```

PR을 만들기 전에 확인합니다.

- [ ] 최신 `main`을 반영했습니다.
- [ ] 서버가 정상 실행됩니다.
- [ ] `clean build`에 성공했습니다.
- [ ] 새 API를 직접 테스트했습니다.
- [ ] 요청·응답 DTO를 확인했습니다.
- [ ] DB 변경 내용을 팀원에게 공유했습니다.
- [ ] 비밀번호와 개인정보가 Commit에 포함되지 않았습니다.

## 예외 및 설계 결정 기록

중요한 설정이나 팀 규칙이 변경되면 아래 표에 기록합니다.

| 날짜 | 결정 ID | 변경 내용 | 변경 이유 | 영향 범위 | 결정자 | 관련 PR·문서 |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-07-27 | DEC-001 | Java 21·Spring Boot 4.1.0·MySQL 사용 | 백엔드 개발환경 통일 | 전체 백엔드 | 백엔드 팀 | 초기 설정 |
| 2026-08-02 | DEC-002 | DB 비밀번호를 `DB_PASSWORD` 환경변수로 관리 | 비밀번호의 Git 노출 방지 | 실행환경 | 백엔드 팀 | 초기 설정 |
| 2026-08-02 | DEC-003 | `http://localhost:3000` CORS 허용 | 프론트엔드 개발 서버와 연동 | `/api/**` | 백엔드 팀 | CORS 설정 |
| `YYYY-MM-DD` | `DEC-004` | `[작성]` | `[작성]` | `[작성]` | `[작성]` | `[링크]` |

## 추가 예정

본격적인 기능 개발 전후로 다음 항목을 순차적으로 추가합니다.

- [ ] 테스트 전용 데이터베이스 설정
- [ ] GitHub Actions 백엔드 CI
- [ ] 공통 오류 응답 및 전역 예외 처리
- [ ] Swagger/OpenAPI 문서
- [ ] API 명세
- [ ] ERD 및 Entity 관계
- [ ] 운영환경 설정 분리
- [ ] 배포 설정
