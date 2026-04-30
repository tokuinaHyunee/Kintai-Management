# 勤怠管理システム (Kintai Management)

React + Spring Boot + MySQL 기반의 일본어 근태관리 웹 애플리케이션입니다.

---

## 기술 스택

| 영역 | 기술 |
|---|---|
| Frontend | React 18, TypeScript, Vite, Axios, React Router v6 |
| Backend | Spring Boot 3, Spring Security, JWT, JPA/Hibernate |
| Database | MySQL 8 |
| Build | Gradle (백엔드), npm (프론트엔드) |

---

## 주요 기능

### 일반 사원
- 출근 / 퇴근 / 외출 / 외출 복귀 타임스탬프
- 업무 내용 메모 저장
- 월간·주간 근무 기록 조회 (캘린더 뷰)
- 공휴일 자동 표시 (일본 공휴일 API 연동)
- 연차 잔여일수 실시간 표시
- 휴가 신청 (연차 / 반차 오전·오후 / 병가)
- 근무 데이터 CSV 내보내기 · 관리자 제출

### 관리자
- 전사원 월별 근무 집계 및 PDF 출력
- 휴가 신청 승인 / 거절
- 사원 계정 생성 · 삭제
- CSV 일괄 임포트

---

## 프로젝트 구조

```
kintai/
├── kintai-backend/          # Spring Boot
│   └── src/main/java/com/example/kintai/
│       ├── config/          # SecurityConfig, GlobalExceptionHandler
│       ├── controller/      # REST API 엔드포인트
│       ├── service/         # 비즈니스 로직 (AttendanceService, LeaveService)
│       ├── entity/          # JPA 엔티티
│       ├── repository/      # Spring Data JPA
│       ├── dto/             # 요청·응답 DTO
│       ├── exception/       # 커스텀 예외 (BusinessException)
│       ├── security/        # JWT 필터·프로바이더
│       └── util/            # DateTimeUtil, EmployeeResolver
│
└── kintai-frontend/         # React + TypeScript
    └── src/
        ├── api/             # Axios 클라이언트
        ├── contexts/        # AuthContext (전역 인증 상태)
        ├── hooks/           # useAuth
        ├── pages/           # 페이지 컴포넌트
        ├── components/      # 공통 컴포넌트 (Topbar, Sidebar)
        ├── types/           # TypeScript 타입 정의
        └── utils/           # 날짜·에러·레이블 유틸
```

---

## 시작하기

### 사전 요구 사항
- Java 17+
- Node.js 18+
- MySQL 8

### 데이터베이스 설정

```sql
CREATE DATABASE `kintai-mana` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 백엔드 실행

```bash
cd kintai-backend
./gradlew bootRun
# → http://localhost:8080/kintai-backend
```

`src/main/resources/application.properties` 주요 설정:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/kintai-mana?...
jwt.secret=${JWT_SECRET}          # 환경변수로 관리 권장
cors.allowed-origins=http://localhost:5173
```

### 프론트엔드 실행

```bash
cd kintai-frontend
npm install
npm run dev
# → http://localhost:5173
```

---

## API 엔드포인트 요약

| 분류 | 메서드 | 경로 | 설명 |
|---|---|---|---|
| 인증 | POST | `/api/auth/login` | 로그인 |
| 인증 | POST | `/api/auth/refresh` | 토큰 갱신 |
| 근태 | GET | `/api/attendance?month=` | 월별 기록 조회 |
| 근태 | POST | `/api/attendance/clock-in` | 출근 |
| 근태 | POST | `/api/attendance/clock-out` | 퇴근 |
| 근태 | POST | `/api/attendance/go-out` | 외출 |
| 근태 | POST | `/api/attendance/go-out-return` | 외출 복귀 |
| 집계 | GET | `/api/summary/monthly?month=` | 월간 집계 |
| 휴가 | POST | `/api/leaves` | 휴가 신청 |
| 휴가 | GET | `/api/leaves` | 내 신청 목록 |
| 관리자 | GET | `/api/admin/monthly-summary?month=` | 전사원 집계 |
| 관리자 | GET | `/api/admin/leaves` | 휴가 신청 목록 |
| 관리자 | PUT | `/api/admin/leaves/{id}/approve` | 승인 |
| 관리자 | PUT | `/api/admin/leaves/{id}/reject` | 거절 |
| 관리자 | GET | `/api/admin/accounts` | 사원 목록 |
| 관리자 | POST | `/api/admin/accounts` | 사원 생성 |
| 관리자 | DELETE | `/api/admin/accounts/{id}` | 사원 삭제 |

---

## 아키텍처

```
[React SPA]
    │ JWT Bearer Token
    ▼
[Spring Security (JwtAuthenticationFilter)]
    │
    ▼
[Controller]  ← 요청·응답 처리만 담당
    │
    ▼
[Service]     ← 비즈니스 로직 (@Transactional)
    │
    ▼
[Repository]  ← JPA (JOIN FETCH로 N+1 방지)
    │
    ▼
[MySQL]       ← 복합 인덱스 설정
```

### 인증 흐름
1. 로그인 → JWT 발급 (유효기간 30분)
2. 모든 API 요청에 `Authorization: Bearer {token}` 헤더 포함
3. 25분마다 자동 토큰 갱신 / 30분 비활동 시 자동 로그아웃

---

## 보안

- 비밀번호: BCrypt 해시 저장, 평문 API 미노출
- JWT 비밀키: 환경변수(`JWT_SECRET`) 관리 권장
- CORS: `cors.allowed-origins` 프로퍼티로 허용 오리진 제어
- 관리자 엔드포인트: `hasRole('ADMIN')` 경로·메서드 이중 검증 (`@PreAuthorize`)
- 에러 응답: 내부 스택 트레이스 미노출 (서버 로그에만 기록)

---

## 성능 최적화

- `WorkTimeRepository.findWithGoOutRecordsByEmployeeAndDateBetween` — `LEFT JOIN FETCH`로 GoOutRecord N+1 해소
- `AdminController.getMonthlySummary` — 전사원 집계를 2쿼리(Account JOIN FETCH + WorkTime JOIN FETCH)로 처리
- `LeaveRequest`, `WorkTime` 테이블 복합 인덱스 설정

---

## 환경 변수 (프로덕션 권장)

| 변수 | 설명 | 예시 |
|---|---|---|
| `JWT_SECRET` | JWT 서명 키 (32바이트 이상) | `openssl rand -base64 32` |
| `DB_PASSWORD` | MySQL 비밀번호 | — |
| `cors.allowed-origins` | 허용 프론트엔드 오리진 | `https://yourdomain.com` |
