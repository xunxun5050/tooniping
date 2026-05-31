# Implementation Roadmap (MVP)

## 현재 완료

- 저장소 기본 구조 생성 (`backend`, `frontend`, `docs`)
- 명세서 사본 정리
- DB DDL/시드 SQL 작성
- 사용자 API 핵심 엔드포인트 구현
  - `GET /api/home`
  - `GET /api/webtoon-filters`
  - `GET /api/webtoons`
  - `GET /api/webtoons/{id}`
  - `GET /api/webtoons/{id}/similar`
- 관리자 API 1차 구현
  - 목록/상세/등록/수정/활성화/비활성화/썸네일 갱신
- 크롤링 API 스켈레톤 구현
  - 초기/주간 실행
  - 이력 목록/상세 조회
- 프론트 사용자 화면 구현
  - `/`, `/webtoons`, `/webtoons/:id`, `/about`

## 다음 우선순위

1. 인증/권한 적용
   - 관리자 API 접근 제어
   - 운영 환경 비밀키/환경변수 정리
2. 크롤러 실제 구현
   - 네이버 정책 검토 후 크롤러 스크립트 작성
   - 크롤링 실패 재시도/백오프
3. 이미지 저장 전략 고도화
   - 로컬 저장 정책 및 정리 작업
   - S3/R2 전환 준비
4. 운영 배포
   - Backend/Frontend 배포 파이프라인
   - robots.txt / sitemap / 메타 태그
5. 테스트
   - 백엔드 통합 테스트
   - 프론트 e2e 또는 주요 화면 스모크 테스트
