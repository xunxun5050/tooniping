# Webtoon Hub MVP

웹툰 허브(Webtoon Hub) 1차 MVP 저장소입니다.

## 구조

- `backend`: Spring Boot API 서버 (목록/상세/필터/홈 + 관리자/크롤링 스켈레톤)
- `frontend`: Next.js 사용자 웹
- `docs`: 프로젝트 문서

## 빠른 시작

### 1) DB 실행 (선택)

```bash
docker compose up -d
```

기본 로컬 개발은 H2(파일 DB, `backend/data/webtoon_hub.mv.db`)로 실행되어 재시작 후에도 데이터가 유지됩니다.
MySQL 연결이 필요하면 `backend/src/main/resources/application-mysql.yml` 프로필을 사용하세요.

### 2) Backend 실행

```bash
cd backend
mvn spring-boot:run
```

기본 포트: `8080`

`mvn` 또는 시스템 JDK가 없는 경우:

```bash
cd backend
./run-backend.sh
```

초기 웹툰 데이터 적재:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin1234"}' | jq -r '.data.token')

curl -X POST http://localhost:8080/api/admin/crawlers/naver-webtoon/initial \
  -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:8080/api/admin/crawlers/kakao-webtoon/initial \
  -H "Authorization: Bearer $TOKEN"
```

### 3) Frontend 실행

```bash
cd frontend
pnpm install
pnpm dev
```

기본 포트: `3000`

### 4) 주요 엔드포인트

- `GET /api/home`
- `GET /api/webtoon-filters`
- `GET /api/webtoons`
- `GET /api/webtoons/{id}`
- `GET /api/webtoons/{id}/similar`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/admin/webtoons` (로그인 토큰 필요)
- `POST /api/admin/webtoons` (로그인 토큰 필요)

## 로그인 (관리자)

- 기본 계정: `admin`
- 기본 비밀번호: `admin1234`
- 로그인 후 발급된 토큰을 `Authorization: Bearer <token>` 헤더로 전달하면 관리자 API를 호출할 수 있습니다.

환경변수로 계정 변경:

- `APP_AUTH_USERNAME`
- `APP_AUTH_PASSWORD`
- `APP_AUTH_SECRET`
- `APP_AUTH_TOKEN_VALID_MINUTES`

## 소셜 회원가입/로그인 (카카오/네이버)

- 로그인 페이지(`/login`)에서 `카카오로 시작하기`, `네이버로 시작하기` 버튼으로 회원가입/로그인이 가능합니다.
- 백엔드 환경변수를 설정해야 실제 OAuth 연동이 동작합니다.

로컬 설정 파일 생성:

```bash
cp backend/oauth.env.example backend/oauth.env
```

`backend/oauth.env`에 실제 키를 입력한 뒤, 백엔드는 `./run-backend.sh`로 실행하면 자동 로드됩니다.

필수 환경변수:

- `APP_AUTH_OAUTH_FRONTEND_BASE_URL` (예: `http://localhost:3000`)
- `APP_AUTH_OAUTH_KAKAO_CLIENT_ID`
- `APP_AUTH_OAUTH_KAKAO_CLIENT_SECRET`
- `APP_AUTH_OAUTH_KAKAO_REDIRECT_URI` (예: `http://localhost:8080/api/auth/oauth/kakao/callback`)
- `APP_AUTH_OAUTH_NAVER_CLIENT_ID`
- `APP_AUTH_OAUTH_NAVER_CLIENT_SECRET`
- `APP_AUTH_OAUTH_NAVER_REDIRECT_URI` (예: `http://localhost:8080/api/auth/oauth/naver/callback`)

각 개발자 콘솔(카카오/네이버)에도 위 `REDIRECT_URI`를 동일하게 등록해야 합니다.

권장 등록 값(로컬 개발):

- 카카오 Redirect URI: `http://localhost:8080/api/auth/oauth/kakao/callback`
- 네이버 Callback URL: `http://localhost:8080/api/auth/oauth/naver/callback`

## 참고

초기 기획 명세는 [`docs/README_webtoon_hub_mvp.md`](docs/README_webtoon_hub_mvp.md)로 복사해 두었습니다.
