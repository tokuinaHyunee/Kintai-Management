# 근태 관리 시스템 (勤怠管理システム)

사원의 출퇴근 기록, 월간 집계, 관리자의 CSV 일괄 등록을 지원하는 풀스택 근태 관리 웹 애플리케이션입니다.
UI 언어는 일본어, 코드 주석은 한국어로 작성되어 있습니다.

---

## 기술 스택

### 프론트엔드

| 항목             | 버전                     |
| ---------------- | ------------------------ |
| React            | 19                       |
| TypeScript       | 6                        |
| Vite             | 8                        |
| React Router DOM | 7                        |
| Axios            | 1.15                     |
| 스타일링         | 자체 CSS (CSS Variables) |

### 백엔드

| 항목                        | 버전   |
| --------------------------- | ------ |
| Spring Boot                 | 3.4.4  |
| Java                        | 21     |
| Spring Security + JWT       | -      |
| Spring Data JPA (Hibernate) | -      |
| MySQL                       | 8.x    |
| Lombok                      | -      |
| 빌드 도구                   | Gradle |

---

## 프로젝트 구조

```
kintai-frontend/              # React + TypeScript 프론트엔드 (Vite, port 5173)
├── src/
│   ├── api/api.ts            # Axios API 클라이언트 (인터셉터 포함)
│   ├── components/
│   │   ├── Topbar.tsx        # 상단 헤더 (CSV 송신 버튼 포함)
│   │   ├── Sidebar.tsx       # 좌측 네비게이션 메뉴
│   │   └── AdminMailbox/     # 관리자 CSV 메일함 패널
│   ├── hooks/useAuth.ts      # JWT 인증 상태 관리 훅
│   ├── pages/
│   │   ├── Login/            # 사원 로그인
│   │   ├── AdminLogin/       # 관리자 로그인
│   │   ├── Dashboard/        # 출퇴근 대시보드
│   │   ├── MonthlyRecord/    # 월간 근태 실적
│   │   ├── AdminMonthlyRecord/ # 전 사원 월간 실적 (관리자)
│   │   ├── EmployeeMaster/   # 사원 계정 관리 (관리자)
│   │   └── Upload/           # CSV 업로드 (관리자)
│   ├── styles/global.css     # 전역 CSS 변수 및 공통 스타일
│   └── types/index.ts        # 공통 TypeScript 타입 정의

kintai-backend/               # Spring Boot 백엔드 (port 8080, context: /kintai-backend)
└── src/main/java/com/example/kintai/
    ├── config/               # Spring Security 설정, JWT 필터
    ├── controller/
    │   ├── AuthController.java          # 로그인 / JWT 발급
    │   ├── AttendanceController.java    # 출퇴근 타임스탬프 / CSV 메일함 송신
    │   ├── SummaryController.java       # 월간·주간 집계 / CSV 내보내기
    │   ├── ImportController.java        # 관리자 CSV 일괄 등록
    │   ├── CsvSubmissionController.java # CSV 메일함 (목록·내용조회·완료처리·삭제)
    │   ├── AdminController.java         # 전 사원 실적 조회
    │   └── EmployeeMasterController.java# 사원·계정 CRUD
    ├── dto/                  # 요청·응답 DTO 클래스
    ├── entity/               # JPA 엔티티
    ├── repository/           # Spring Data JPA Repository 인터페이스
    ├── security/             # JWT 토큰 프로바이더 및 필터
    └── service/              # 비즈니스 로직 (AuthService 등)
```

---

## 로컬 개발 환경 설정

### 사전 요구사항

- Java 21 이상
- Node.js 18 이상
- MySQL 8.x

### 1. 데이터베이스 생성

```sql
CREATE DATABASE kintai_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> `spring.jpa.hibernate.ddl-auto=update` 설정으로 서버 최초 기동 시 테이블이 자동 생성됩니다.

### 2. 백엔드 설정

`kintai-backend/src/main/resources/application.properties`를 환경에 맞게 수정합니다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/kintai_db?useSSL=false&serverTimezone=Asia/Tokyo&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD

server.port=8080
server.servlet.context-path=/kintai-backend

jwt.secret=YOUR_JWT_SECRET_KEY
jwt.expiration=1800000
```

### 3. 백엔드 실행

Eclipse에서 `KintaiBackendApplication.java`를 Spring Boot App으로 실행하거나:

```bash
cd kintai-backend
./gradlew bootRun
```

→ `http://localhost:8080/kintai-backend`

### 4. 프론트엔드 실행

```bash
cd kintai-frontend
npm install
npm run dev
```

→ `http://localhost:5173`

> Vite 개발 서버는 `/api` 요청을 `http://localhost:8080/kintai-backend`로 자동 프록시합니다.

---

## 초기 관리자 계정 생성

최초 기동 후 DB에 직접 관리자 계정을 삽입합니다.

```sql
INSERT INTO employee (employee_name, department, active_flag, created_at, updated_at)
VALUES ('관리자', '管理部', 1, NOW(), NOW());

INSERT INTO account (employee_id, login_id, password, password_plain, role, active_flag, created_at, updated_at)
VALUES (
  LAST_INSERT_ID(),
  'ADMIN001',
  '$2a$10$...',   -- BCrypt 해시 (비밀번호: admin1234)
  'admin1234',
  'ADMIN', 1, NOW(), NOW()
);
```

> BCrypt 해시는 온라인 BCrypt Generator 또는 Spring Security `BCryptPasswordEncoder`로 생성하세요.

---

## 주요 기능

### 사원 화면

| 기능             | 설명                                                 |
| ---------------- | ---------------------------------------------------- |
| 출근 / 퇴근      | 버튼 클릭으로 현재 시각 타임스탬프 기록              |
| 외출 / 외출 복귀 | 업무 중 외출 기록                                    |
| 업무 내용 메모   | 당일 업무 내용 자유 입력 및 저장                     |
| 월간 근태 실적   | 달력 + 주간 집계 바 + 근무기록 목록 (월간·주간 전환) |
| CSV 송신         | 근무표 CSV 파일을 관리자 메일함으로 전송             |

### 관리자 화면

| 기능              | 설명                                                       |
| ----------------- | ---------------------------------------------------------- |
| 전 사원 월간 실적 | 전체 사원 월별 출근일수·노동시간·잔업시간 조회 및 PDF 출력 |
| CSV 업로드        | CSV 파일 미리보기 후 근태 데이터 일괄 등록                 |
| CSV 메일함        | 사원 송신 CSV 수신 → 업로드 탭에서 내용 확인 후 등록       |
| 사원 관리         | 계정 생성 (8자리 사원번호 + 자동 비밀번호 발급) / 삭제     |

---

## CSV 플로우

```
[사원]
  Topbar "CSV送信" 버튼
    → CSV 파일 선택
    → 원본 파일 그대로 관리자 메일함으로 전송
       POST /api/attendance/submit-csv

[관리자]
  Topbar 메일함 아이콘 (미확인 건수 배지 / 30초 폴링)
    → 수신 목록에서 [確認・取込] 버튼 클릭
       GET /api/admin/csv-submissions/{id}/content
    → CSVアップロード 페이지로 자동 이동 + CSV 내용 자동 로드
    → 미리보기 확인 후 [N件 登録] 클릭
       POST /api/admin/import/attendance
    → DB 등록 완료 + 메일함 자동 [登録済] 처리
       POST /api/admin/csv-submissions/{id}/mark-imported
```

---

## CSV 파일 형식

일본식 근무표 CSV 형식을 지원합니다.

| 헤더 패턴                        | 매핑 필드        | 비고                                  |
| -------------------------------- | ---------------- | ------------------------------------- |
| `氏名：홍길동` (메타 행)         | 사원명 자동 추출 | 괄호 내 로마자 자동 제거              |
| `月日` / `日付` / `勤務日`       | 근무일           | `4月1日(水)` → `YYYY-MM-DD` 자동 변환 |
| `始業時刻` / `出勤`              | 출근 시각        | `H:mm` 또는 `HH:mm`                   |
| `終業時刻` / `退勤`              | 퇴근 시각        | 동상                                  |
| `休憩` / `休憩(分)`              | 휴식 시간        | `1:00` → `60`분 자동 변환             |
| `備考` / `業務内容` / `納入物等` | 업무 메모        |                                       |

- 휴식 시간 미지정 시: 6시간 초과 근무에 60분 자동 공제
- 사원명이 DB와 불일치할 경우: 업로드 화면에서 경고 모달 표시

---

## API 엔드포인트

### 인증

| 메서드 | URL                 | 설명                       |
| ------ | ------------------- | -------------------------- |
| POST   | `/api/auth/login`   | 로그인 / JWT 발급          |
| POST   | `/api/auth/refresh` | 토큰 갱신 (유효 토큰 필요) |

### 사원 근태 (인증 필요)

| 메서드 | URL                             | 설명                   |
| ------ | ------------------------------- | ---------------------- |
| GET    | `/api/attendance?month=YYYY-MM` | 월간 근태 목록         |
| POST   | `/api/attendance/clock-in`      | 출근                   |
| POST   | `/api/attendance/clock-out`     | 퇴근                   |
| POST   | `/api/attendance/go-out`        | 외출                   |
| POST   | `/api/attendance/go-out-return` | 외출 복귀              |
| POST   | `/api/attendance/work-memo`     | 업무 내용 저장         |
| POST   | `/api/attendance/submit-csv`    | CSV 관리자 메일함 송신 |

### 집계

| 메서드 | URL                                  | 설명         |
| ------ | ------------------------------------ | ------------ |
| GET    | `/api/summary/monthly?month=YYYY-MM` | 월간 집계    |
| GET    | `/api/summary/export?month=YYYY-MM`  | CSV 내보내기 |

### 관리자 (ADMIN 권한 필요)

| 메서드 | URL                                        | 설명               |
| ------ | ------------------------------------------ | ------------------ |
| GET    | `/api/admin/monthly-summary?month=YYYY-MM` | 전 사원 월간 실적  |
| GET    | `/api/admin/accounts`                      | 사원 목록          |
| POST   | `/api/admin/accounts`                      | 사원 등록          |
| DELETE | `/api/admin/accounts/{id}`                 | 사원 삭제          |
| POST   | `/api/admin/import/attendance`             | CSV 근태 일괄 등록 |

### CSV 메일함 (ADMIN 권한 필요)

| 메서드 | URL                                             | 설명               |
| ------ | ----------------------------------------------- | ------------------ |
| GET    | `/api/admin/csv-submissions/pending-count`      | 미확인 건수        |
| GET    | `/api/admin/csv-submissions?status=PENDING`     | 수신 목록          |
| GET    | `/api/admin/csv-submissions/{id}/content`       | CSV 원본 내용 조회 |
| POST   | `/api/admin/csv-submissions/{id}/mark-imported` | 등록 완료 마킹     |
| DELETE | `/api/admin/csv-submissions/{id}`               | 삭제               |

---

## DB 테이블 구조

| 테이블                 | 설명                                                     |
| ---------------------- | -------------------------------------------------------- |
| `employee`             | 사원 기본 정보 (이름, 부서, 활성 여부)                   |
| `account`              | 로그인 계정 (사원번호, 비밀번호 BCrypt, 권한 USER/ADMIN) |
| `work_time`            | 근태 기록 (출근·퇴근·외출·휴식·노동시간·잔업시간·메모)   |
| `csv_submission`       | 사원 송신 CSV 원본 보관 (상태: PENDING / IMPORTED)       |
| `batch_import_history` | CSV 일괄 등록 이력                                       |
| `correction_request`   | (레거시 미사용) 수정 신청 이력                           |

---

## 인증 방식

- **JWT Bearer 토큰** 방식
- 로그인 성공 시 토큰 발급 → `localStorage`에 저장
- Axios 인터셉터에서 모든 요청 헤더에 `Authorization: Bearer <token>` 자동 부여
- 401 응답 시 토큰 삭제 + `/login` 자동 리다이렉트
- 토큰 유효기간: **30분** (`jwt.expiration=1800000`)
- 토큰 자동 갱신: 로그인 후 25분마다 `POST /api/auth/refresh` 호출 (활동 중인 경우에만)
- 자동 로그아웃: 마우스·키보드·터치 등 30분간 **비활동** 시 자동 로그아웃
- 사원번호 입력: **최대 8자리** 제한

---

## 권한 체계

| 권한             | 로그인 URL     | 접근 가능 화면                                  |
| ---------------- | -------------- | ----------------------------------------------- |
| `USER` (사원)    | `/login`       | 출퇴근 대시보드, 월간 실적, CSV 송신            |
| `ADMIN` (관리자) | `/admin/login` | 전 사원 실적, CSV 업로드, 사원 관리, CSV 메일함 |

---

## 빌드

### 프론트엔드 프로덕션 빌드

```bash
cd kintai-frontend
npm run build
# dist/ 디렉터리에 정적 파일 생성
```

### 백엔드 JAR 빌드

```bash
cd kintai-backend
./gradlew build
# build/libs/kintai-backend-0.0.1-SNAPSHOT.jar 생성
```
