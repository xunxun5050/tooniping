# 배포 가이드

이 프로젝트는 `backend` Spring Boot API와 `frontend` Next.js 앱을 따로 배포합니다.

## 권장 조합

- 백엔드: Railway
- DB: Railway MySQL
- 프론트엔드: Vercel

Railway는 monorepo 배포 시 서비스별 Root Directory 설정을 지원하고, MySQL 서비스는 `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE`, `MYSQL_URL` 변수를 제공합니다.

## 1. 백엔드 배포: Railway

Railway 프로젝트를 만들고 GitHub 저장소에서 백엔드 서비스를 추가합니다.

서비스 설정:

- Root Directory: `/backend`
- Dockerfile: `backend/Dockerfile`
- Config File: `/backend/railway.toml`
- Health Check Path: `/api/health`

Railway 프로젝트에 MySQL 서비스를 하나 추가한 뒤, 백엔드 서비스에 아래 환경변수를 설정합니다.

필수 환경변수:

```bash
SPRING_PROFILES_ACTIVE=mysql
SPRING_DATASOURCE_URL=jdbc:mysql://<MYSQLHOST>:<MYSQLPORT>/<MYSQLDATABASE>?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=utf8
SPRING_DATASOURCE_USERNAME=<MYSQLUSER>
SPRING_DATASOURCE_PASSWORD=<MYSQLPASSWORD>
APP_AUTH_SECRET=<충분히 긴 랜덤 문자열>
APP_AUTH_TOKEN_VALID_MINUTES=480
APP_AUTH_OAUTH_FRONTEND_BASE_URL=https://<프론트엔드 도메인>
```

소셜 로그인 사용 시 추가:

```bash
APP_AUTH_OAUTH_KAKAO_CLIENT_ID=<카카오 REST API 키>
APP_AUTH_OAUTH_KAKAO_CLIENT_SECRET=<카카오 client secret>
APP_AUTH_OAUTH_KAKAO_REDIRECT_URI=https://<백엔드 도메인>/api/auth/oauth/kakao/callback
APP_AUTH_OAUTH_NAVER_CLIENT_ID=<네이버 client id>
APP_AUTH_OAUTH_NAVER_CLIENT_SECRET=<네이버 client secret>
APP_AUTH_OAUTH_NAVER_REDIRECT_URI=https://<백엔드 도메인>/api/auth/oauth/naver/callback
```

개발자 콘솔에도 위 redirect URI를 동일하게 등록해야 합니다.

Railway 변수 참조를 쓰는 경우 MySQL 서비스 이름이 `MySQL`일 때 예시는 아래처럼 둘 수 있습니다. 서비스 이름을 바꾸면 `MySQL` 부분도 맞춰 바꿔야 합니다.

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=utf8
SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}
SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}
```

## 2. 프론트엔드 배포: Vercel

Vercel 기준:

- Root directory: `frontend`
- Framework: Next.js
- Install command: `pnpm install --frozen-lockfile`
- Build command: `pnpm build`
- Output: Next.js 기본값

필수 환경변수:

```bash
NEXT_PUBLIC_API_BASE_URL=https://<백엔드 도메인>
```

예시 파일은 `frontend/env.production.example`에 있습니다.

배포 후에는 백엔드의 `APP_AUTH_OAUTH_FRONTEND_BASE_URL`을 실제 프론트엔드 도메인으로 바꾸고 백엔드를 재배포합니다.

## 3. 배포 후 확인

```bash
curl https://<백엔드 도메인>/api/health
curl https://<백엔드 도메인>/api/home
curl https://<프론트엔드 도메인>/
curl https://<프론트엔드 도메인>/webtoons
```

확인할 화면:

- 메인 페이지에서 오늘 요일 인기 웹툰 노출
- 상단 작은 검색창으로 검색
- 카카오/네이버 OAuth 로그인
- 웹툰 즐겨찾기와 마이페이지
