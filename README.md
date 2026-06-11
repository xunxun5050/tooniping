# Tooniping

네이버 웹툰과 카카오 웹툰을 한곳에서 탐색하고, 즐겨찾기한 작품을 내 서재처럼 관리하는 웹툰 통합 서비스입니다.

백엔드는 Spring Boot API, 프론트엔드는 Next.js App Router로 구성되어 있으며, 운영 환경은 Railway와 Vercel에 나누어 배포합니다.

## 운영 URL

- Frontend: https://tooniping.vercel.app
- Backend: https://backend-production-9cc0.up.railway.app
- Backend health check: https://backend-production-9cc0.up.railway.app/api/health

## 주요 기능

### 웹툰 탐색

- 네이버 웹툰, 카카오 웹툰 통합 목록 제공
- 메인 화면에서 오늘 요일 웹툰을 인기순으로 노출
- 상단 검색창으로 제목/작가 검색
- 요일, 장르, 연재 상태 필터
- 인기순, 최신순, 제목순, 요일순 정렬
- 웹툰 상세 페이지와 유사 작품 추천
- 웹툰 카드/상세에서 원본 플랫폼 이동
- 플랫폼별 시각 구분
  - 네이버 웹툰: 초록색
  - 카카오 웹툰: 노란색

### 크롤링과 데이터 적재

- 네이버 웹툰 초기/주간 크롤링
- 카카오 웹툰 초기/주간 크롤링
- 썸네일 URL 수집 및 대표 이미지 저장
- 요일, 장르, 상태, 플랫폼 메타데이터 적재
- 인기순 정렬을 위한 랭킹 데이터 저장
- 크롤링 이력과 실패 정보 조회

### 회원 기능

- 이메일 회원가입과 JWT 로그인
- 회원가입 시 6자리 이메일 인증번호 발송/확인
- 기본 관리자 로그인
- 카카오 OAuth 로그인
- 네이버 OAuth 로그인
- 가입/첫 로그인 시 재미있는 단어 조합 닉네임 자동 생성
- 닉네임 수정
- 회원 탈퇴
  - 마이페이지에서 탈퇴 전용 페이지로 이동
  - `탈퇴` 입력 후 탈퇴 실행
  - 프로필과 즐겨찾기 데이터 삭제

### 즐겨찾기와 내 서재

- 웹툰 카드에서 즐겨찾기 추가/해제
- 웹툰 상세 페이지에서 즐겨찾기 추가/해제
- 우측 상단 닉네임/사용자 버튼으로 내 서재 이동
- 내 서재에서 연재중 즐겨찾기만 보기
- 기본 요일별 달력형 레이아웃
- 장르별 보기 전환
- 매일+ 작품은 월요일부터 일요일까지 모든 요일에 표시

### 마이페이지

- 닉네임 수정
- 내 즐겨찾기 작품 목록
- 내 활동 영역
- 계정 관리에서 회원 탈퇴 페이지 이동

## 기술 스택

### Backend

- Java 17
- Spring Boot 3.3.5
- Spring Web
- Spring JDBC
- Bean Validation
- H2 file database for local development
- MySQL for production
- Maven

### Frontend

- Next.js 15 App Router
- React 18
- TypeScript
- pnpm
- CSS Modules가 아닌 전역 CSS 기반의 경량 UI

### Infrastructure

- Railway: Spring Boot backend, MySQL
- Vercel: Next.js frontend
- GitHub: source repository and deployment trigger

## 프로젝트 구조

```text
.
├── backend
│   ├── src/main/java/com/webtoonhub
│   │   ├── admin       # 관리자 웹툰 CRUD, 썸네일 갱신
│   │   ├── auth        # 로그인, OAuth, 닉네임, 회원 탈퇴
│   │   ├── common      # 공통 응답, 예외 처리, CORS, health/root
│   │   ├── crawler     # 네이버/카카오 크롤러 실행과 이력 조회
│   │   ├── favorite    # 사용자 즐겨찾기 API
│   │   └── webtoon     # 홈, 목록, 상세, 필터, 유사 작품
│   ├── src/main/resources
│   │   ├── schema.sql
│   │   ├── data.sql
│   │   ├── application.yml
│   │   └── application-mysql.yml
│   ├── Dockerfile
│   ├── railway.toml
│   └── run-backend.sh
├── frontend
│   ├── app
│   │   ├── page.tsx
│   │   ├── webtoons
│   │   ├── favorites
│   │   ├── mypage
│   │   ├── login
│   │   └── about
│   ├── components
│   ├── lib
│   └── package.json
├── docs
│   ├── deployment.md
│   └── README_webtoon_hub_mvp.md
└── docker-compose.yml
```

## 로컬 실행

### 사전 준비

- Java 17
- Node.js 20 이상 권장
- pnpm
- Docker, 선택 사항

이 저장소에는 로컬 백엔드 실행을 위한 JDK 경로를 사용하는 `backend/run-backend.sh`가 있습니다. 시스템에 Java/Maven 설정이 부족하면 이 스크립트를 쓰는 편이 가장 간단합니다.

### 1. 저장소 클론

```bash
git clone git@github.com:xunxun5050/tooniping.git
cd tooniping
```

### 2. 백엔드 실행

기본 개발 DB는 H2 file DB입니다. 데이터는 `backend/data/webtoon_hub.mv.db`에 유지됩니다.

```bash
cd backend
./run-backend.sh
```

시스템 Java/Maven이 준비되어 있다면 아래 명령도 사용할 수 있습니다.

```bash
cd backend
./mvnw spring-boot:run
```

백엔드 기본 주소:

```text
http://localhost:8080
```

정상 동작 확인:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/home
```

### 3. 프론트엔드 실행

```bash
cd frontend
pnpm install
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 pnpm dev
```

프론트엔드 기본 주소:

```text
http://localhost:3000
```

### 4. MySQL로 실행하고 싶을 때

로컬 MySQL 컨테이너를 띄웁니다.

```bash
docker compose up -d
```

백엔드는 `mysql` 프로필로 실행합니다.

```bash
cd backend
SPRING_PROFILES_ACTIVE=mysql ./run-backend.sh
```

기본 MySQL 접속 정보는 `docker-compose.yml`에 있습니다.

```text
database: webtoon_hub
username: webtoon
password: webtoon
root password: root
port: 3306
```

## 환경변수

### Backend

로컬 OAuth 설정 파일을 만들 수 있습니다.

```bash
cp backend/oauth.env.example backend/oauth.env
```

`backend/run-backend.sh`는 `backend/oauth.env`를 자동으로 로드합니다.

| 변수 | 설명 | 로컬 예시 |
| --- | --- | --- |
| `APP_AUTH_USERNAME` | 기본 관리자 계정 | `admin` |
| `APP_AUTH_PASSWORD` | 기본 관리자 비밀번호 | `admin1234` |
| `APP_AUTH_SECRET` | JWT 서명용 secret | 충분히 긴 임의 문자열 |
| `APP_AUTH_TOKEN_VALID_MINUTES` | 토큰 유효 시간 | `480` |
| `MAIL_HOST` | 이메일 인증번호 발송용 SMTP host | SMTP 제공자 값 |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_USERNAME` | SMTP 계정 | SMTP 제공자 값 |
| `MAIL_PASSWORD` | SMTP 비밀번호 또는 앱 비밀번호 | SMTP 제공자 값 |
| `MAIL_SMTP_AUTH` | SMTP 인증 사용 여부 | `true` |
| `MAIL_SMTP_STARTTLS_ENABLE` | STARTTLS 사용 여부 | `true` |
| `APP_AUTH_EMAIL_VERIFICATION_FROM` | 인증 메일 발신자 | `no-reply@tooniping.app` |
| `APP_AUTH_EMAIL_VERIFICATION_EXPIRE_MINUTES` | 인증번호 유효 시간 | `10` |
| `APP_AUTH_EMAIL_VERIFICATION_MAX_ATTEMPTS` | 인증번호 확인 최대 시도 횟수 | `5` |
| `APP_AUTH_OAUTH_FRONTEND_BASE_URL` | OAuth 완료 후 돌아갈 프론트 주소 | `http://localhost:3000` |
| `APP_AUTH_OAUTH_KAKAO_CLIENT_ID` | 카카오 REST API 키 | 카카오 개발자 콘솔 값 |
| `APP_AUTH_OAUTH_KAKAO_CLIENT_SECRET` | 카카오 client secret | 카카오 개발자 콘솔 값 |
| `APP_AUTH_OAUTH_KAKAO_REDIRECT_URI` | 카카오 callback URI | `http://localhost:8080/api/auth/oauth/kakao/callback` |
| `APP_AUTH_OAUTH_NAVER_CLIENT_ID` | 네이버 client id | 네이버 개발자 센터 값 |
| `APP_AUTH_OAUTH_NAVER_CLIENT_SECRET` | 네이버 client secret | 네이버 개발자 센터 값 |
| `APP_AUTH_OAUTH_NAVER_REDIRECT_URI` | 네이버 callback URI | `http://localhost:8080/api/auth/oauth/naver/callback` |
| `app.cors.allowed-origins` | 허용할 프론트 오리진 | `http://localhost:3000,http://127.0.0.1:3000` |

주의: 실제 OAuth 키와 secret은 절대 커밋하지 않습니다.

### Frontend

| 변수 | 설명 | 로컬 예시 |
| --- | --- | --- |
| `NEXT_PUBLIC_API_BASE_URL` | 브라우저와 서버 컴포넌트가 호출할 백엔드 API 주소 | `http://localhost:8080` |

운영에서는 Vercel 환경변수로 아래처럼 설정합니다.

```bash
NEXT_PUBLIC_API_BASE_URL=https://backend-production-9cc0.up.railway.app
```

## 데이터 적재

관리자 토큰을 발급합니다.

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin","password":"admin1234"}' | jq -r '.data.token')
```

네이버 웹툰 초기 적재:

```bash
curl -X POST http://localhost:8080/api/admin/crawlers/naver-webtoon/initial \
  -H "Authorization: Bearer $TOKEN"
```

카카오 웹툰 초기 적재:

```bash
curl -X POST http://localhost:8080/api/admin/crawlers/kakao-webtoon/initial \
  -H "Authorization: Bearer $TOKEN"
```

주간 갱신:

```bash
curl -X POST http://localhost:8080/api/admin/crawlers/naver-webtoon/weekly \
  -H "Authorization: Bearer $TOKEN"

curl -X POST http://localhost:8080/api/admin/crawlers/kakao-webtoon/weekly \
  -H "Authorization: Bearer $TOKEN"
```

크롤링 이력 확인:

```bash
curl "http://localhost:8080/api/admin/crawl-histories?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

## 주요 화면

| 경로 | 설명 |
| --- | --- |
| `/` | 오늘 요일 웹툰과 장르/요일 진입점 |
| `/webtoons` | 웹툰 목록, 검색, 필터, 정렬 |
| `/webtoons/[id]` | 웹툰 상세, 유사 작품, 즐겨찾기 |
| `/favorites` | 연재중 즐겨찾기를 요일별/장르별로 보는 내 서재 |
| `/mypage` | 닉네임 수정, 즐겨찾기 요약, 활동, 계정 관리 |
| `/mypage/withdrawal` | 회원 탈퇴 전용 확인 페이지 |
| `/login` | 기본 로그인, 카카오/네이버 OAuth 시작 |
| `/about` | 서비스 소개 |
| `/admin` | 관리자 작업 화면 |

## API 요약

모든 API는 아래 공통 응답 형식을 사용합니다.

```json
{
  "success": true,
  "data": {},
  "message": null
}
```

### Public

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/` | 프론트엔드 URL로 리다이렉트 |
| `GET` | `/api/health` | health check |
| `GET` | `/api/home` | 메인 화면용 최근 웹툰, 요일 메뉴, 장르 메뉴 |
| `GET` | `/api/webtoon-filters` | 플랫폼, 장르, 요일, 상태 필터 |
| `GET` | `/api/webtoons` | 웹툰 목록 검색 |
| `GET` | `/api/webtoons/{id}` | 웹툰 상세 |
| `GET` | `/api/webtoons/{id}/similar` | 유사 웹툰 |

`GET /api/webtoons` 주요 쿼리:

| Query | 설명 |
| --- | --- |
| `keyword` | 제목/작가 검색어 |
| `platform` | `NAVER_WEBTOON`, `KAKAO_WEBTOON` |
| `genre` | 장르 코드 |
| `weekday` | `MONDAY` ... `SUNDAY`, `DAILY_PLUS` |
| `status` | `ONGOING`, `COMPLETED`, `HIATUS`, `UNKNOWN` |
| `page` | 0부터 시작 |
| `size` | 페이지 크기 |
| `sort` | `popular`, `latest`, `title`, `weekday` |

예시:

```bash
curl "http://localhost:8080/api/webtoons?sort=popular&weekday=MONDAY&page=0&size=20"
```

### Auth

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/auth/signup/email-code` | 이메일 회원가입 인증번호 발송 |
| `POST` | `/api/auth/signup/email-code/verify` | 이메일 회원가입 인증번호 확인 |
| `POST` | `/api/auth/signup` | 이메일 회원가입 |
| `POST` | `/api/auth/login` | 이메일/기본 관리자 로그인 |
| `POST` | `/api/auth/refresh` | 로그인 유지 토큰 갱신 |
| `POST` | `/api/auth/logout` | 로그아웃 |
| `GET` | `/api/auth/me` | 내 프로필 |
| `PATCH` | `/api/auth/me/nickname` | 닉네임 수정 |
| `DELETE` | `/api/auth/me` | 회원 탈퇴 |
| `GET` | `/api/auth/oauth/{provider}/start` | OAuth 시작 |
| `GET` | `/api/auth/oauth/{provider}/callback` | OAuth callback |

인증 API 예시:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin","password":"admin1234"}'
```

### Favorites

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/me/favorites` | 내 즐겨찾기 목록 |
| `PUT` | `/api/me/favorites/{webtoonId}` | 즐겨찾기 추가 |
| `DELETE` | `/api/me/favorites/{webtoonId}` | 즐겨찾기 삭제 |

### Admin

관리자 API는 `Authorization: Bearer <token>` 헤더가 필요합니다.

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/admin/webtoons` | 관리자 웹툰 목록 |
| `GET` | `/api/admin/webtoons/{id}` | 관리자 웹툰 상세 |
| `POST` | `/api/admin/webtoons` | 웹툰 수동 생성 |
| `PUT` | `/api/admin/webtoons/{id}` | 웹툰 수정 |
| `PATCH` | `/api/admin/webtoons/{id}/inactive` | 비활성화 |
| `PATCH` | `/api/admin/webtoons/{id}/active` | 활성화 |
| `POST` | `/api/admin/webtoons/{id}/thumbnail/refresh` | 썸네일 갱신 |
| `POST` | `/api/admin/crawlers/naver-webtoon/initial` | 네이버 초기 크롤링 |
| `POST` | `/api/admin/crawlers/naver-webtoon/weekly` | 네이버 주간 크롤링 |
| `POST` | `/api/admin/crawlers/kakao-webtoon/initial` | 카카오 초기 크롤링 |
| `POST` | `/api/admin/crawlers/kakao-webtoon/weekly` | 카카오 주간 크롤링 |
| `GET` | `/api/admin/crawl-histories` | 크롤링 이력 목록 |
| `GET` | `/api/admin/crawl-histories/{id}` | 크롤링 이력 상세 |

## 데이터베이스 개요

주요 테이블:

| Table | 설명 |
| --- | --- |
| `platforms` | 네이버/카카오 플랫폼 |
| `webtoons` | 웹툰 기본 정보 |
| `webtoon_images` | 썸네일 등 이미지 |
| `genres` | 장르 코드 |
| `webtoon_genres` | 웹툰-장르 매핑 |
| `weekdays` | 요일 코드 |
| `webtoon_weekdays` | 웹툰-요일 매핑 |
| `webtoon_popularity_rankings` | 인기순 랭킹 |
| `user_accounts` | 이메일 회원 계정과 비밀번호 해시 |
| `user_profiles` | 사용자 프로필과 닉네임 |
| `user_refresh_tokens` | 로그인 유지용 refresh token |
| `email_verification_codes` | 이메일 회원가입 인증번호 해시와 만료 정보 |
| `user_favorite_webtoons` | 사용자 즐겨찾기 |
| `webtoon_evaluations` | 사용자 웹툰 평가와 감정 태그 |
| `crawl_histories` | 크롤링 실행 이력 |
| `crawl_failures` | 크롤링 실패 상세 |

스키마는 `backend/src/main/resources/schema.sql`에서 관리합니다.

## 배포

자세한 배포 절차는 `docs/deployment.md`를 참고하세요.

### Railway backend

권장 설정:

- Root Directory: `/backend`
- Dockerfile: `backend/Dockerfile`
- Config File: `/backend/railway.toml`
- Health Check Path: `/api/health`
- Profile: `mysql`

필수 환경변수:

```bash
SPRING_PROFILES_ACTIVE=mysql
SPRING_DATASOURCE_URL=jdbc:mysql://<MYSQLHOST>:<MYSQLPORT>/<MYSQLDATABASE>?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=utf8
SPRING_DATASOURCE_USERNAME=<MYSQLUSER>
SPRING_DATASOURCE_PASSWORD=<MYSQLPASSWORD>
APP_AUTH_SECRET=<충분히 긴 랜덤 문자열>
APP_AUTH_TOKEN_VALID_MINUTES=480
APP_AUTH_OAUTH_FRONTEND_BASE_URL=https://tooniping.vercel.app
MAIL_HOST=<SMTP 서버 호스트>
MAIL_PORT=587
MAIL_USERNAME=<SMTP 계정>
MAIL_PASSWORD=<SMTP 비밀번호 또는 앱 비밀번호>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
APP_AUTH_EMAIL_VERIFICATION_FROM=<발신자 이메일>
```

OAuth 사용 시 카카오/네이버 client id, secret, redirect URI도 Railway에 설정해야 합니다.

### Vercel frontend

권장 설정:

- Root Directory: `frontend`
- Framework: Next.js
- Install Command: `pnpm install --frozen-lockfile`
- Build Command: `pnpm build`

필수 환경변수:

```bash
NEXT_PUBLIC_API_BASE_URL=https://backend-production-9cc0.up.railway.app
```

## 검증 명령

백엔드:

```bash
cd backend
JAVA_HOME="$PWD/.jdks/jdk-17.0.19+10/Contents/Home" \
PATH="$PWD/.jdks/jdk-17.0.19+10/Contents/Home/bin:$PATH" \
MAVEN_USER_HOME="$PWD/.m2" \
./mvnw test
```

프론트엔드:

```bash
cd frontend
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 pnpm build
```

운영 확인:

```bash
curl https://backend-production-9cc0.up.railway.app/api/health
curl https://backend-production-9cc0.up.railway.app/api/home
curl https://tooniping.vercel.app/
curl https://tooniping.vercel.app/webtoons
curl https://tooniping.vercel.app/mypage/withdrawal
```

## 개발 중 자주 겪는 문제

### Next dev 서버가 빌드 후 이상하게 동작할 때

`pnpm build` 직후 켜져 있던 `next dev`가 React Client Manifest 오류를 낼 수 있습니다. 이때는 dev 서버를 끄고 `.next`를 삭제한 뒤 다시 실행합니다.

```bash
cd frontend
rm -rf .next
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 pnpm dev
```

### 8080 포트로 접속했는데 JSON이 보일 때

백엔드 루트 `/`는 프론트엔드 URL로 리다이렉트합니다. 로컬 앱은 보통 아래 주소로 접속합니다.

```text
http://localhost:3000
```

### OAuth가 실패할 때

- 백엔드 환경변수의 redirect URI와 개발자 콘솔에 등록된 redirect URI가 완전히 같은지 확인합니다.
- 로컬에서는 `http://localhost:8080/api/auth/oauth/kakao/callback`, `http://localhost:8080/api/auth/oauth/naver/callback`을 사용합니다.
- 운영에서는 Railway 백엔드 도메인을 사용합니다.

## 참고 문서

- `docs/deployment.md`: Railway/Vercel 배포 가이드
- `docs/README_webtoon_hub_mvp.md`: 초기 MVP 기획 문서
