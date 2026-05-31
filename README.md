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
curl -X POST http://localhost:8080/api/admin/crawlers/naver-webtoon/initial
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
- `GET /api/admin/webtoons`
- `POST /api/admin/webtoons`

## 참고

초기 기획 명세는 [`docs/README_webtoon_hub_mvp.md`](docs/README_webtoon_hub_mvp.md)로 복사해 두었습니다.
