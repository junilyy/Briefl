# BRIEFL

BRIEFL은 관심 종목 기반 AI 뉴스 분석 리포트 서비스입니다. 사용자가 관심 종목을 입력하면 관련 뉴스를 수집하고, AI가 호재/악재/중립 가능성, 핵심 뉴스, 뉴스 요약, 가격 영향 변수, 체크 포인트를 정리해줍니다.

현재 MVP는 대표 종목 5개를 기준으로 리포트 생성을 체험할 수 있습니다.

- 삼성전자
- NAVER
- 카카오
- Tesla
- NVIDIA

## 주요 기능

- 지원 종목 목록 조회
- 관심 종목 뉴스 수집
- 오늘 날짜 리포트 생성 및 저장
- 이미 생성된 오늘 리포트 재조회
- AI 분석 모드 선택
  - `mock`: OpenAI API 호출 없이 mock 분석 결과 생성
  - `openai`: OpenAI API를 호출해 실제 분석 생성
- React 기반 리포트 생성/조회 UI
- Google Apps Script 기반 방문자/피드백 수집

## 기술 스택

- Frontend: React, TypeScript, Vite
- Backend: Java 17, Spring Boot, Spring MVC, Spring Data JPA
- Database: PostgreSQL
- External API: Naver News Search API, OpenAI API
- Deploy: Render, Netlify

## 프로젝트 구조

```text
.
├── backend/              # Spring Boot API 서버
│   ├── src/
│   ├── build.gradle
│   ├── docker-compose.yml
│   └── .env.example
├── frontend/             # React/Vite 프론트엔드
│   ├── src/
│   ├── package.json
│   └── .env.example
├── netlify.toml
└── README.md
```

## 사전 준비

로컬 실행을 위해 아래가 필요합니다.

- Java 17
- Node.js 20 이상 권장
- Docker Desktop 또는 로컬 PostgreSQL
- Naver Developers Client ID / Secret
- OpenAI API Key

## 환경변수

환경변수 값은 별도 제출/공유되는 값을 사용하면 됩니다. 로컬에서는 아래 파일을 만들어 넣어주세요.

### Backend

`backend/.env` 파일을 생성합니다.

```env
POSTGRES_PORT=5432
POSTGRES_DB=briefl
POSTGRES_USER=briefl
POSTGRES_PASSWORD=briefl

DATABASE_URL=jdbc:postgresql://localhost:5432/briefl
DATABASE_USERNAME=briefl
DATABASE_PASSWORD=briefl

NAVER_CLIENT_ID=발급받은_네이버_CLIENT_ID
NAVER_CLIENT_SECRET=발급받은_네이버_CLIENT_SECRET
NAVER_API_REQUEST_TIMEOUT_SECONDS=10

AI_ANALYSIS_MODE=openai
OPENAI_MODEL=gpt-5.4-nano
OPENAI_API_KEY=발급받은_OPENAI_API_KEY
OPENAI_MAX_OUTPUT_TOKENS=2000
OPENAI_API_REQUEST_TIMEOUT_SECONDS=90

EXTERNAL_API_CONNECT_TIMEOUT_MILLIS=3000
EXTERNAL_API_RESPONSE_TIMEOUT_SECONDS=10
```

OpenAI 비용 없이 화면 흐름만 확인하려면 아래처럼 mock 모드로 변경할 수 있습니다.

```env
AI_ANALYSIS_MODE=mock
OPENAI_API_KEY=
```

### Frontend

`frontend/.env` 파일을 생성합니다.

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_APPS_SCRIPT_URL=Google_Apps_Script_URL
```

`VITE_GOOGLE_APPS_SCRIPT_URL`이 비어 있으면 Google Sheets로 실제 전송하지 않고 콘솔에 payload를 출력한 뒤 제출 완료 상태를 보여줍니다.

## 로컬 실행

### 1. PostgreSQL 실행

Docker를 사용하는 경우:

```bash
cd backend
docker compose up -d
```

PostgreSQL을 직접 설치해서 사용하는 경우에는 `backend/.env`의 `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`를 로컬 DB 설정에 맞게 수정하면 됩니다.

### 2. Backend 실행

```bash
cd backend
./gradlew bootRun
```

백엔드는 기본적으로 `http://localhost:8080`에서 실행됩니다.

정상 실행 확인:

```bash
curl http://localhost:8080/api/health
```

예상 응답:

```json
{"status":"ok"}
```

지원 종목 목록 확인:

```bash
curl http://localhost:8080/api/stocks
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

### 3. Frontend 실행

새 터미널에서 실행합니다.

```bash
cd frontend
npm ci
npm run dev
```

프론트엔드는 기본적으로 아래 주소에서 확인할 수 있습니다.

```text
http://localhost:5173
```

## 리포트 생성 테스트

프론트 화면에서 `삼성전자`, `NAVER`, `카카오`, `Tesla`, `NVIDIA` 중 하나를 입력하거나 선택한 뒤 리포트를 생성하면 됩니다.

API로 직접 테스트하려면:

```bash
curl -X POST http://localhost:8080/api/reports \
  -H "Content-Type: application/json" \
  -d '{"stockName":"삼성전자"}'
```

오늘 생성된 리포트 조회:

```bash
curl "http://localhost:8080/api/reports?stockName=삼성전자"
```

## 빌드 및 검사

### Backend

```bash
cd backend
./gradlew clean build
```

### Frontend

```bash
cd frontend
npm run lint
npm run build
```

## 운영 배포 참고

### Backend(Render)

Render Web Service는 backend 디렉터리의 Dockerfile을 사용해 배포할 수 있습니다.

주요 설정:

- Root Directory: `backend`
- Runtime: Docker
- Dockerfile Path: `./Dockerfile`
- Region: PostgreSQL과 같은 region 권장
- Health Check Path: `/api/health`

Render 환경변수에는 `backend/.env`에 해당하는 값을 넣습니다. Render PostgreSQL을 사용할 경우 `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`는 Render에서 제공하는 값을 사용합니다.

### Frontend(Netlify)

`netlify.toml`에 빌드 설정이 포함되어 있습니다.

```toml
[build]
  command = "cd frontend && npm ci && npm run build"
  publish = "frontend/dist"
```

Netlify 환경변수에는 아래 값을 넣습니다.

```env
VITE_API_BASE_URL=https://배포된-백엔드-주소
VITE_GOOGLE_APPS_SCRIPT_URL=Google_Apps_Script_URL
```

## Google Sheets 수집 데이터

방문자 로그는 `visitors_ver2` 시트로 전송합니다.

```text
id, landingUrl, ip, referer, time_stamp, utm, device
```

피드백은 `beta_testers_ver2` 시트로 전송합니다.

```text
id, email, stockName, reportId, reportDate, lossAvoidanceHelp, mostUseful,
willingnessToPay, expectedFeature, comment, landingUrl, ip, utm, device,
userAgent, time_stamp
```

## 주의사항

- OpenAI API Key가 제공된 경우 `AI_ANALYSIS_MODE=openai`로 실제 분석을 테스트할 수 있습니다.
- OpenAI 비용 없이 화면 흐름만 확인하려면 `AI_ANALYSIS_MODE=mock`으로 실행하세요.
- Naver News API 인증 정보가 없으면 실제 뉴스 수집이 실패할 수 있습니다.
- `/health`가 아니라 `/api/health`가 헬스 체크 경로입니다.
- Render 무료 인스턴스는 일정 시간 요청이 없으면 sleep 될 수 있습니다.
