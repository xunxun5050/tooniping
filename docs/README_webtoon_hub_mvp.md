# 📚 웹툰 허브 (Webtoon Hub)

> 네이버 웹툰부터 시작하는 웹툰 링크 허브 서비스  
> 여러 플랫폼에 흩어진 웹툰 정보를 한곳에서 검색하고, 원본 플랫폼으로 이동할 수 있게 돕는 서비스

---

## 1. 프로젝트 개요

**웹툰 허브(Webtoon Hub)**는 웹툰 정보를 한곳에서 검색하고 확인할 수 있는 웹툰 링크 허브 서비스입니다.

1차 버전에서는 **네이버 웹툰**을 대상으로 시작합니다.  
웹툰 본문이나 회차 이미지를 제공하는 뷰어 서비스가 아니라, 사용자가 웹툰 정보를 확인한 뒤 **원본 네이버 웹툰 페이지로 이동**할 수 있도록 돕는 링크 허브 역할을 합니다.

향후에는 카카오웹툰, 카카오페이지, 레진코믹스 등 다른 플랫폼까지 확장하여 통합 웹툰 탐색 서비스로 발전시키는 것을 목표로 합니다.

---

## 2. 1차 버전 목표

1차 버전의 목표는 실제 운영 가능한 최소 기능을 빠르게 구현하고 배포하는 것입니다.

### 핵심 목표

- 네이버 웹툰 데이터를 초기 크롤링으로 DB에 적재
- 사용자가 웹툰 목록 조회 가능
- 제목/작가명 기반 검색 가능
- 요일/장르/연재 상태 기준 필터링 가능
- 웹툰 상세 정보 조회 가능
- 네이버 웹툰 원본 페이지로 이동 가능
- 로그인 없이 사용할 수 있는 공개형 서비스 구현
- 관리자 화면 없이 관리자 API와 DB 기반으로 수동 등록/수정 가능
- 주 1회 데이터 업데이트 또는 필요 시 수동 업데이트 가능

---

## 3. 1차 버전 범위

### 포함 기능

#### 3.1 웹툰 목록 조회

사용자는 수집된 네이버 웹툰 목록을 카드 형태로 확인할 수 있습니다.

목록 화면 표시 정보:

- 웹툰 썸네일
- 웹툰 제목
- 작가명
- 장르
- 연재 요일
- 연재 상태
- 플랫폼명
- 네이버 웹툰 바로가기 버튼

#### 3.2 웹툰 검색

사용자는 다음 조건으로 웹툰을 검색할 수 있습니다.

- 제목 검색
- 작가명 검색

#### 3.3 웹툰 필터링

사용자는 다음 조건으로 웹툰 목록을 필터링할 수 있습니다.

- 연재 요일
- 장르
- 연재 상태

1차 버전에서는 플랫폼이 네이버 웹툰 하나이므로 플랫폼 필터는 내부 데이터 구조만 준비하고, 사용자 화면에서는 생략할 수 있습니다.

#### 3.4 웹툰 상세 페이지

사용자는 웹툰 상세 페이지에서 다음 정보를 확인할 수 있습니다.

- 웹툰 제목
- 대표 썸네일 이미지
- 작가명
- 장르
- 연재 요일
- 연재 상태
- 작품 소개
- 플랫폼명
- 원본 웹툰 URL
- 네이버 웹툰 바로가기 버튼
- 비슷한 장르의 웹툰 추천

#### 3.5 원본 플랫폼 이동

사용자는 웹툰 상세 페이지 또는 목록 화면에서 네이버 웹툰 원본 페이지로 이동할 수 있습니다.

1차 버전에서는 특정 회차 직접 이동 기능은 제외하고, **작품 메인 페이지 이동**만 제공합니다.

#### 3.6 관리자 API 기반 수동 등록

관리자 화면은 1차 버전에서 제외합니다.

대신 관리자 API 또는 DB 직접 등록 방식으로 누락된 웹툰을 등록/수정할 수 있도록 합니다.

관리자 API를 통해 관리할 수 있는 항목:

- 웹툰 수동 등록
- 웹툰 정보 수정
- 웹툰 비노출 처리
- 썸네일 재수집
- 크롤링 실행
- 크롤링 이력 조회

---

## 4. 1차 버전 제외 범위

다음 기능은 1차 버전에서는 제외하고 이후 버전에서 추가합니다.

- 로그인
- 회원가입
- 즐겨찾기
- 평점
- 공개 코멘트
- 마이페이지
- 캐릭터/등급 성장
- 다중 플랫폼 통합
- 회차별 직접 이동
- 사용자 기반 랭킹
- 관리자 화면

---

## 5. 향후 확장 계획

### 2차 버전

- 로그인
- 즐겨찾기
- 사용자 평점
- 공개 코멘트
- 코멘트 수정/삭제
- 코멘트 신고
- 마이페이지

### 3차 버전

- 카카오웹툰 추가
- 카카오페이지 추가
- 레진코믹스 추가
- 플랫폼별 필터 강화
- 통합 검색 고도화
- 사용자 활동 기반 인기 웹툰 랭킹

### 4차 버전

- 취향 분석
- 장르별 추천 고도화
- 활동량 기반 캐릭터/등급 성장
- 웹툰 소식 페이지
- 드라마화/애니화/완결/휴재 소식 큐레이션

---

## 6. 데이터 수집 정책

### 6.1 초기 데이터 적재

서비스 초기에는 네이버 웹툰 데이터를 전체적으로 한 번 크롤링하여 DB에 적재합니다.

초기 적재 대상 데이터:

- 웹툰 제목
- 작가명
- 장르
- 연재 요일
- 연재 상태
- 작품 소개
- 대표 썸네일 이미지 URL
- 대표 썸네일 이미지 파일
- 원본 웹툰 URL
- 플랫폼명

### 6.2 이후 데이터 업데이트

초기 데이터 적재 이후에는 다음 방식으로 데이터를 갱신합니다.

- 관리자 API를 통한 수동 등록/수정
- 주 1회 정기 업데이트
- 누락 작품 발견 시 수동 보완

1차 버전에서는 실시간 크롤링을 하지 않습니다.

### 6.3 썸네일 이미지 저장 방식

웹툰 썸네일은 다음 두 가지 정보를 모두 저장합니다.

- 원본 이미지 URL
- 서버 또는 스토리지에 저장한 이미지 파일 경로

화면에서는 저장된 이미지 파일을 우선 사용하고, 저장 이미지가 없을 경우 원본 이미지 URL을 대체로 사용할 수 있습니다.

### 6.4 크롤링 고려 사항

크롤링 구현 전 다음 사항을 확인합니다.

- 네이버 웹툰 `robots.txt` 확인
- 네이버 웹툰 이용약관 확인
- 요청 빈도 제한
- 이미지 저장 가능 범위 검토
- 원본 출처 표기 방식 검토
- 상업적 운영 가능 여부 검토

서비스는 웹툰 본문, 회차 이미지, 유료 콘텐츠를 복제하여 제공하지 않습니다.

---

## 7. 화면 목록

## 7.1 사용자 화면

| 화면 ID | 화면명 | URL 예시 | 설명 |
|---|---|---|---|
| U-001 | 메인 화면 | `/` | 서비스 소개, 검색, 요일별 바로가기, 최근 등록 웹툰 |
| U-002 | 웹툰 목록 화면 | `/webtoons` | 전체 웹툰 목록 조회, 검색, 필터링 |
| U-003 | 웹툰 상세 화면 | `/webtoons/{id}` | 웹툰 상세 정보 조회 및 원본 링크 이동 |
| U-004 | 검색 결과 화면 | `/webtoons?keyword=...` | 검색어 기반 웹툰 목록 표시 |
| U-005 | 요일별 목록 화면 | `/webtoons?weekday=MONDAY` | 특정 요일 연재작 목록 표시 |
| U-006 | 장르별 목록 화면 | `/webtoons?genre=FANTASY` | 특정 장르 웹툰 목록 표시 |
| U-007 | 서비스 소개/정책 화면 | `/about` | 서비스 목적, 원본 출처 안내, 문의 안내 |

검색 결과, 요일별 목록, 장르별 목록은 별도 화면으로 만들기보다 웹툰 목록 화면의 쿼리 파라미터로 처리합니다.

---

## 8. 화면별 상세 명세

## 8.1 메인 화면

### URL

`/`

### 목적

사용자가 서비스에 처음 진입했을 때 웹툰을 빠르게 탐색할 수 있도록 합니다.

### 주요 구성 요소

- 상단 헤더
- 서비스 로고
- 검색창
- 요일별 바로가기
- 장르별 바로가기
- 최근 등록된 웹툰
- 원본 출처 안내

### 사용자 액션

| 액션 | 결과 |
|---|---|
| 검색어 입력 후 검색 | 웹툰 목록 화면으로 이동 |
| 요일 클릭 | 해당 요일 필터가 적용된 목록 화면으로 이동 |
| 장르 클릭 | 해당 장르 필터가 적용된 목록 화면으로 이동 |
| 웹툰 카드 클릭 | 웹툰 상세 화면으로 이동 |

---

## 8.2 웹툰 목록 화면

### URL

`/webtoons`

예시:

- `/webtoons?keyword=신의탑`
- `/webtoons?weekday=MONDAY&genre=FANTASY&status=ONGOING`

### 목적

사용자가 조건에 맞는 웹툰을 탐색할 수 있도록 합니다.

### 주요 구성 요소

- 검색창
- 요일 필터
- 장르 필터
- 연재 상태 필터
- 정렬 옵션
- 웹툰 카드 리스트
- 페이지네이션

### 검색 조건

| 조건 | 설명 |
|---|---|
| keyword | 제목 또는 작가명 검색 |
| weekday | 연재 요일 |
| genre | 장르 |
| status | 연재 상태 |
| sort | 정렬 기준 |
| page | 페이지 번호 |
| size | 페이지 크기 |

### 정렬 옵션

| 값 | 설명 |
|---|---|
| latest | 최근 등록순 |
| title | 제목순 |
| weekday | 요일순 |
| updated | 최근 수정순 |

---

## 8.3 웹툰 상세 화면

### URL

`/webtoons/{id}`

### 목적

사용자가 특정 웹툰의 상세 정보를 확인하고 원본 플랫폼으로 이동할 수 있도록 합니다.

### 주요 구성 요소

- 대표 썸네일
- 웹툰 제목
- 작가명
- 작품 소개
- 장르
- 연재 요일
- 연재 상태
- 플랫폼명
- 원본 출처
- 네이버 웹툰 바로가기 버튼
- 비슷한 장르의 웹툰 추천

### 사용자 액션

| 액션 | 결과 |
|---|---|
| 네이버 웹툰 바로가기 클릭 | 새 탭으로 원본 페이지 이동 |
| 비슷한 장르 웹툰 클릭 | 해당 웹툰 상세 화면 이동 |
| 목록으로 돌아가기 클릭 | 이전 목록 화면 이동 |

---

## 9. DB 테이블 설계

## 9.1 설계 방향

1차 버전에서는 네이버 웹툰만 지원하지만, 향후 다중 플랫폼 확장을 고려하여 플랫폼 정보를 별도 테이블로 분리합니다.

웹툰의 장르와 연재 요일은 다대다 관계가 될 수 있으므로 별도 매핑 테이블로 분리합니다.

주요 테이블:

- `platforms`
- `webtoons`
- `genres`
- `webtoon_genres`
- `weekdays`
- `webtoon_weekdays`
- `webtoon_images`
- `crawl_histories`
- `crawl_failures`

---

## 9.2 platforms

플랫폼 정보를 저장합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 플랫폼 ID |
| code | VARCHAR(50) | UNIQUE, NOT NULL | 플랫폼 코드 |
| name | VARCHAR(100) | NOT NULL | 플랫폼명 |
| base_url | VARCHAR(500) | NOT NULL | 플랫폼 기본 URL |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | 사용 여부 |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NOT NULL | 수정일 |

예시:

| code | name | base_url |
|---|---|---|
| NAVER_WEBTOON | 네이버 웹툰 | https://comic.naver.com |

---

## 9.3 webtoons

웹툰 기본 정보를 저장합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 웹툰 ID |
| platform_id | BIGINT | FK, NOT NULL | 플랫폼 ID |
| external_id | VARCHAR(100) | NULL | 플랫폼 내부 웹툰 ID |
| title | VARCHAR(255) | NOT NULL | 웹툰 제목 |
| author | VARCHAR(255) | NULL | 작가명 |
| description | TEXT | NULL | 작품 소개 |
| original_url | VARCHAR(1000) | NOT NULL | 원본 웹툰 URL |
| status | VARCHAR(30) | NOT NULL | 연재 상태 |
| is_adult | BOOLEAN | NOT NULL, DEFAULT FALSE | 성인 여부 |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | 사용자 화면 노출 여부 |
| last_crawled_at | DATETIME | NULL | 마지막 크롤링 일시 |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NOT NULL | 수정일 |

### 제약 조건

| 제약 | 설명 |
|---|---|
| UNIQUE(platform_id, external_id) | 같은 플랫폼 내 동일 웹툰 중복 방지 |
| UNIQUE(platform_id, original_url) | 같은 플랫폼 내 동일 URL 중복 방지 |

### status 값

| 값 | 설명 |
|---|---|
| ONGOING | 연재중 |
| COMPLETED | 완결 |
| HIATUS | 휴재 |
| UNKNOWN | 알 수 없음 |

---

## 9.4 genres

장르 정보를 저장합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 장르 ID |
| code | VARCHAR(50) | UNIQUE, NOT NULL | 장르 코드 |
| name | VARCHAR(100) | NOT NULL | 장르명 |
| sort_order | INT | NOT NULL, DEFAULT 0 | 노출 순서 |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | 사용 여부 |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NOT NULL | 수정일 |

---

## 9.5 webtoon_genres

웹툰과 장르의 매핑 정보를 저장합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| webtoon_id | BIGINT | PK, FK | 웹툰 ID |
| genre_id | BIGINT | PK, FK | 장르 ID |
| created_at | DATETIME | NOT NULL | 생성일 |

---

## 9.6 weekdays

요일 정보를 저장합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 요일 ID |
| code | VARCHAR(30) | UNIQUE, NOT NULL | 요일 코드 |
| name | VARCHAR(50) | NOT NULL | 요일명 |
| sort_order | INT | NOT NULL | 정렬 순서 |

요일 코드:

| code | name | sort_order |
|---|---|---|
| MONDAY | 월요일 | 1 |
| TUESDAY | 화요일 | 2 |
| WEDNESDAY | 수요일 | 3 |
| THURSDAY | 목요일 | 4 |
| FRIDAY | 금요일 | 5 |
| SATURDAY | 토요일 | 6 |
| SUNDAY | 일요일 | 7 |
| COMPLETED | 완결 | 8 |

---

## 9.7 webtoon_weekdays

웹툰과 연재 요일의 매핑 정보를 저장합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| webtoon_id | BIGINT | PK, FK | 웹툰 ID |
| weekday_id | BIGINT | PK, FK | 요일 ID |
| created_at | DATETIME | NOT NULL | 생성일 |

---

## 9.8 webtoon_images

웹툰 이미지 정보를 저장합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 이미지 ID |
| webtoon_id | BIGINT | FK, NOT NULL | 웹툰 ID |
| image_type | VARCHAR(30) | NOT NULL | 이미지 타입 |
| source_url | VARCHAR(1000) | NULL | 원본 이미지 URL |
| stored_path | VARCHAR(1000) | NULL | 저장된 이미지 경로 |
| file_name | VARCHAR(255) | NULL | 저장 파일명 |
| content_type | VARCHAR(100) | NULL | 이미지 MIME 타입 |
| file_size | BIGINT | NULL | 파일 크기 |
| is_primary | BOOLEAN | NOT NULL, DEFAULT FALSE | 대표 이미지 여부 |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NOT NULL | 수정일 |

### image_type 값

| 값 | 설명 |
|---|---|
| THUMBNAIL | 대표 썸네일 |
| BANNER | 배너 이미지 |
| ETC | 기타 이미지 |

1차 버전에서는 `THUMBNAIL`만 사용합니다.

---

## 9.9 crawl_histories

크롤링 실행 이력을 저장합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 크롤링 이력 ID |
| platform_id | BIGINT | FK, NOT NULL | 플랫폼 ID |
| crawl_type | VARCHAR(30) | NOT NULL | 크롤링 유형 |
| status | VARCHAR(30) | NOT NULL | 실행 상태 |
| total_count | INT | NOT NULL, DEFAULT 0 | 전체 대상 수 |
| success_count | INT | NOT NULL, DEFAULT 0 | 성공 수 |
| fail_count | INT | NOT NULL, DEFAULT 0 | 실패 수 |
| started_at | DATETIME | NOT NULL | 시작 시간 |
| ended_at | DATETIME | NULL | 종료 시간 |
| message | TEXT | NULL | 실행 메시지 |
| created_at | DATETIME | NOT NULL | 생성일 |

### crawl_type 값

| 값 | 설명 |
|---|---|
| INITIAL | 초기 전체 적재 |
| WEEKLY_UPDATE | 주간 업데이트 |
| MANUAL | 관리자 수동 실행 |

### status 값

| 값 | 설명 |
|---|---|
| RUNNING | 실행중 |
| SUCCESS | 성공 |
| PARTIAL_SUCCESS | 일부 성공 |
| FAILED | 실패 |

---

## 9.10 crawl_failures

크롤링 실패 정보를 저장합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 실패 ID |
| crawl_history_id | BIGINT | FK, NOT NULL | 크롤링 이력 ID |
| target_url | VARCHAR(1000) | NULL | 실패 URL |
| external_id | VARCHAR(100) | NULL | 외부 웹툰 ID |
| title | VARCHAR(255) | NULL | 웹툰 제목 |
| error_type | VARCHAR(100) | NULL | 오류 유형 |
| error_message | TEXT | NULL | 오류 메시지 |
| retry_count | INT | NOT NULL, DEFAULT 0 | 재시도 횟수 |
| created_at | DATETIME | NOT NULL | 생성일 |

---

## 10. API 명세

## 10.1 공통 응답 형식

### 성공 응답

```json
{
  "success": true,
  "data": {},
  "message": null
}
```

### 실패 응답

```json
{
  "success": false,
  "data": null,
  "message": "오류 메시지"
}
```

### 페이징 응답

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true
  },
  "message": null
}
```

---

## 10.2 사용자 API

## 10.2.1 웹툰 목록 조회

### Endpoint

`GET /api/webtoons`

### 설명

조건에 맞는 웹툰 목록을 조회합니다.

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| keyword | String | N | 제목 또는 작가명 검색어 |
| platform | String | N | 플랫폼 코드 |
| genre | String | N | 장르 코드 |
| weekday | String | N | 요일 코드 |
| status | String | N | 연재 상태 |
| page | Integer | N | 페이지 번호, 기본값 0 |
| size | Integer | N | 페이지 크기, 기본값 20 |
| sort | String | N | 정렬 기준 |

### Request 예시

```http
GET /api/webtoons?keyword=신의탑&weekday=MONDAY&status=ONGOING&page=0&size=20&sort=latest
```

### Response 예시

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "신의 탑",
        "author": "SIU",
        "description": "작품 소개 내용",
        "platform": {
          "code": "NAVER_WEBTOON",
          "name": "네이버 웹툰"
        },
        "genres": [
          {
            "code": "FANTASY",
            "name": "판타지"
          }
        ],
        "weekdays": [
          {
            "code": "MONDAY",
            "name": "월요일"
          }
        ],
        "status": "ONGOING",
        "statusName": "연재중",
        "thumbnailUrl": "/images/webtoons/1/thumbnail.jpg",
        "originalUrl": "https://comic.naver.com/webtoon/list?titleId=183559"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  },
  "message": null
}
```

---

## 10.2.2 웹툰 상세 조회

### Endpoint

`GET /api/webtoons/{webtoonId}`

### 설명

특정 웹툰의 상세 정보를 조회합니다.

### Response 예시

```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "신의 탑",
    "author": "SIU",
    "description": "작품 소개 내용",
    "platform": {
      "code": "NAVER_WEBTOON",
      "name": "네이버 웹툰",
      "baseUrl": "https://comic.naver.com"
    },
    "genres": [
      {
        "code": "FANTASY",
        "name": "판타지"
      }
    ],
    "weekdays": [
      {
        "code": "MONDAY",
        "name": "월요일"
      }
    ],
    "status": "ONGOING",
    "statusName": "연재중",
    "thumbnail": {
      "sourceUrl": "https://image-comic.pstatic.net/...",
      "storedUrl": "/images/webtoons/1/thumbnail.jpg"
    },
    "originalUrl": "https://comic.naver.com/webtoon/list?titleId=183559",
    "lastCrawledAt": "2026-05-31T10:00:00",
    "createdAt": "2026-05-31T10:00:00",
    "updatedAt": "2026-05-31T10:00:00"
  },
  "message": null
}
```

---

## 10.2.3 비슷한 장르 웹툰 조회

### Endpoint

`GET /api/webtoons/{webtoonId}/similar`

### 설명

같은 장르를 가진 웹툰 목록을 조회합니다.

1차 버전에서는 단순히 같은 장르 기준으로 조회합니다.

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| size | Integer | N | 조회 개수, 기본값 6 |

---

## 10.2.4 메인 화면 데이터 조회

### Endpoint

`GET /api/home`

### 설명

메인 화면에 필요한 데이터를 한 번에 조회합니다.

### Response 예시

```json
{
  "success": true,
  "data": {
    "recentWebtoons": [
      {
        "id": 1,
        "title": "신규 웹툰",
        "author": "작가명",
        "thumbnailUrl": "/images/webtoons/1/thumbnail.jpg"
      }
    ],
    "weekdayMenus": [
      {
        "code": "MONDAY",
        "name": "월요일",
        "count": 50
      }
    ],
    "genreMenus": [
      {
        "code": "FANTASY",
        "name": "판타지",
        "count": 30
      }
    ]
  },
  "message": null
}
```

---

## 10.2.5 필터 옵션 조회

### Endpoint

`GET /api/webtoon-filters`

### 설명

검색/필터 UI에서 사용할 플랫폼, 장르, 요일, 상태값 목록을 조회합니다.

---

## 10.3 관리자 API

관리자 화면은 1차 버전에서 제외하지만, 데이터 관리를 위해 관리자 API는 준비합니다.

운영 시 관리자 API는 반드시 인증 또는 접근 제한을 적용해야 합니다.

---

## 10.3.1 관리자 웹툰 목록 조회

### Endpoint

`GET /api/admin/webtoons`

### 설명

관리자가 웹툰 데이터를 조회합니다. 사용자 목록 API와 달리 비노출 데이터도 조회할 수 있습니다.

---

## 10.3.2 관리자 웹툰 상세 조회

### Endpoint

`GET /api/admin/webtoons/{webtoonId}`

---

## 10.3.3 웹툰 수동 등록

### Endpoint

`POST /api/admin/webtoons`

### Request Body

```json
{
  "platformCode": "NAVER_WEBTOON",
  "externalId": "183559",
  "title": "신의 탑",
  "author": "SIU",
  "description": "작품 소개 내용",
  "originalUrl": "https://comic.naver.com/webtoon/list?titleId=183559",
  "status": "ONGOING",
  "isAdult": false,
  "isActive": true,
  "genreCodes": ["FANTASY"],
  "weekdayCodes": ["MONDAY"],
  "thumbnail": {
    "sourceUrl": "https://image-comic.pstatic.net/..."
  }
}
```

---

## 10.3.4 웹툰 정보 수정

### Endpoint

`PUT /api/admin/webtoons/{webtoonId}`

---

## 10.3.5 웹툰 비노출 처리

### Endpoint

`PATCH /api/admin/webtoons/{webtoonId}/inactive`

---

## 10.3.6 웹툰 재노출 처리

### Endpoint

`PATCH /api/admin/webtoons/{webtoonId}/active`

---

## 10.3.7 썸네일 이미지 재수집

### Endpoint

`POST /api/admin/webtoons/{webtoonId}/thumbnail/refresh`

---

## 10.4 크롤링 API

## 10.4.1 네이버 웹툰 초기 적재 실행

### Endpoint

`POST /api/admin/crawlers/naver-webtoon/initial`

### 설명

네이버 웹툰 전체 데이터를 처음 적재합니다.

---

## 10.4.2 네이버 웹툰 주간 업데이트 실행

### Endpoint

`POST /api/admin/crawlers/naver-webtoon/weekly`

### 설명

네이버 웹툰 데이터를 주간 업데이트합니다.

---

## 10.4.3 크롤링 이력 목록 조회

### Endpoint

`GET /api/admin/crawl-histories`

---

## 10.4.4 크롤링 이력 상세 조회

### Endpoint

`GET /api/admin/crawl-histories/{crawlHistoryId}`

---

## 11. SQL DDL 초안

아래 DDL은 MySQL 기준 초안입니다.

```sql
CREATE TABLE platforms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE webtoons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform_id BIGINT NOT NULL,
    external_id VARCHAR(100),
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255),
    description TEXT,
    original_url VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    is_adult BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_crawled_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_webtoons_platform
        FOREIGN KEY (platform_id) REFERENCES platforms(id),
    CONSTRAINT uk_webtoons_platform_external
        UNIQUE (platform_id, external_id),
    CONSTRAINT uk_webtoons_platform_url
        UNIQUE (platform_id, original_url)
);

CREATE TABLE genres (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE webtoon_genres (
    webtoon_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (webtoon_id, genre_id),
    CONSTRAINT fk_webtoon_genres_webtoon
        FOREIGN KEY (webtoon_id) REFERENCES webtoons(id),
    CONSTRAINT fk_webtoon_genres_genre
        FOREIGN KEY (genre_id) REFERENCES genres(id)
);

CREATE TABLE weekdays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL
);

CREATE TABLE webtoon_weekdays (
    webtoon_id BIGINT NOT NULL,
    weekday_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (webtoon_id, weekday_id),
    CONSTRAINT fk_webtoon_weekdays_webtoon
        FOREIGN KEY (webtoon_id) REFERENCES webtoons(id),
    CONSTRAINT fk_webtoon_weekdays_weekday
        FOREIGN KEY (weekday_id) REFERENCES weekdays(id)
);

CREATE TABLE webtoon_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    webtoon_id BIGINT NOT NULL,
    image_type VARCHAR(30) NOT NULL,
    source_url VARCHAR(1000),
    stored_path VARCHAR(1000),
    file_name VARCHAR(255),
    content_type VARCHAR(100),
    file_size BIGINT,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_webtoon_images_webtoon
        FOREIGN KEY (webtoon_id) REFERENCES webtoons(id)
);

CREATE TABLE crawl_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform_id BIGINT NOT NULL,
    crawl_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    fail_count INT NOT NULL DEFAULT 0,
    started_at DATETIME NOT NULL,
    ended_at DATETIME,
    message TEXT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_crawl_histories_platform
        FOREIGN KEY (platform_id) REFERENCES platforms(id)
);

CREATE TABLE crawl_failures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crawl_history_id BIGINT NOT NULL,
    target_url VARCHAR(1000),
    external_id VARCHAR(100),
    title VARCHAR(255),
    error_type VARCHAR(100),
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_crawl_failures_history
        FOREIGN KEY (crawl_history_id) REFERENCES crawl_histories(id)
);
```

---

## 12. 초기 코드성 데이터

### 12.1 플랫폼

```sql
INSERT INTO platforms (code, name, base_url, is_active, created_at, updated_at)
VALUES ('NAVER_WEBTOON', '네이버 웹툰', 'https://comic.naver.com', TRUE, NOW(), NOW());
```

### 12.2 요일

```sql
INSERT INTO weekdays (code, name, sort_order)
VALUES
('MONDAY', '월요일', 1),
('TUESDAY', '화요일', 2),
('WEDNESDAY', '수요일', 3),
('THURSDAY', '목요일', 4),
('FRIDAY', '금요일', 5),
('SATURDAY', '토요일', 6),
('SUNDAY', '일요일', 7),
('COMPLETED', '완결', 8);
```

### 12.3 장르

```sql
INSERT INTO genres (code, name, sort_order, is_active, created_at, updated_at)
VALUES
('FANTASY', '판타지', 1, TRUE, NOW(), NOW()),
('ROMANCE', '로맨스', 2, TRUE, NOW(), NOW()),
('ACTION', '액션', 3, TRUE, NOW(), NOW()),
('DAILY', '일상', 4, TRUE, NOW(), NOW()),
('COMEDY', '개그', 5, TRUE, NOW(), NOW()),
('THRILLER', '스릴러', 6, TRUE, NOW(), NOW()),
('DRAMA', '드라마', 7, TRUE, NOW(), NOW()),
('SPORTS', '스포츠', 8, TRUE, NOW(), NOW());
```

---

## 13. 프론트엔드 라우팅 초안

## 13.1 사용자 라우트

| Route | 화면 |
|---|---|
| `/` | 메인 |
| `/webtoons` | 웹툰 목록 |
| `/webtoons/:id` | 웹툰 상세 |
| `/about` | 서비스 소개/정책 |

---

## 14. 백엔드 패키지 구조 초안

Spring Boot 기준 예시입니다.

```text
com.webtoonhub
├── common
│   ├── response
│   ├── exception
│   └── config
├── platform
│   ├── domain
│   ├── repository
│   └── service
├── webtoon
│   ├── controller
│   ├── domain
│   ├── dto
│   ├── repository
│   └── service
├── genre
│   ├── domain
│   ├── repository
│   └── service
├── crawler
│   ├── controller
│   ├── domain
│   ├── repository
│   └── service
├── admin
│   └── controller
└── image
    ├── service
    └── storage
```

---

## 15. 추천 기술 스택

### Frontend

- Next.js

실제 운영과 SEO를 고려하면 Next.js를 우선 고려합니다.

### Backend

- Spring Boot

웹툰 조회 API, 관리자 API, 크롤링 실행 관리, 향후 로그인/평점/코멘트 기능 확장에 사용합니다.

### Database

- MySQL 또는 PostgreSQL

1차 버전에서는 익숙한 DB를 우선 선택합니다.

### Crawling

- Python 또는 Java/Spring Batch

초기에는 Python 스크립트로 수집 후 DB에 적재하고, 이후 안정화되면 Spring Batch 또는 스케줄러 구조로 전환할 수 있습니다.

### Image Storage

- 초기: 로컬 파일 저장
- 운영 확장: AWS S3 또는 Cloudflare R2

### Deploy

- Frontend: Vercel
- Backend: Render, Railway, Fly.io, AWS Lightsail 중 선택
- Database: Supabase, Railway DB, AWS RDS 중 선택

---

## 16. 우선 개발 순서

### 16.1 1단계: DB 및 기본 API

- platforms 테이블 생성
- webtoons 테이블 생성
- genres 테이블 생성
- weekdays 테이블 생성
- 매핑 테이블 생성
- 웹툰 목록 조회 API
- 웹툰 상세 조회 API
- 필터 옵션 조회 API

### 16.2 2단계: 프론트 기본 화면

- 메인 화면
- 웹툰 목록 화면
- 웹툰 상세 화면
- 검색/필터 UI
- 원본 링크 이동

### 16.3 3단계: 크롤링

- 네이버 웹툰 초기 크롤링 스크립트
- DB 적재 로직
- 썸네일 이미지 다운로드
- 크롤링 이력 저장
- 실패 데이터 저장

### 16.4 4단계: 관리자 API

- 관리자 웹툰 목록 조회
- 웹툰 수동 등록
- 웹툰 수정
- 비노출 처리
- 썸네일 재수집
- 크롤링 이력 조회

### 16.5 5단계: 운영 준비

- 배포
- SEO 설정
- 사이트맵 생성
- robots.txt 설정
- 기본 에러 페이지
- 이미지 저장 경로 정리
- 원본 출처 표기 추가

---

## 17. 1차 버전 완료 기준

1차 버전은 다음 조건을 만족하면 완료로 봅니다.

- 네이버 웹툰 데이터가 DB에 적재되어 있다.
- 사용자가 웹툰 목록을 조회할 수 있다.
- 사용자가 제목 또는 작가명으로 검색할 수 있다.
- 사용자가 요일, 장르, 연재 상태로 필터링할 수 있다.
- 사용자가 웹툰 상세 정보를 볼 수 있다.
- 사용자가 네이버 웹툰 원본 페이지로 이동할 수 있다.
- 썸네일 이미지가 화면에 표시된다.
- 관리자 API로 누락 웹툰을 수동 등록할 수 있다.
- 주간 업데이트 또는 수동 업데이트가 가능하다.
- 크롤링 실패 이력을 확인할 수 있다.
- 실제 배포 URL에서 접근할 수 있다.

---

## 18. 운영 고려 사항

### 18.1 저작권 및 플랫폼 정책

- 웹툰 본문, 회차 이미지, 유료 콘텐츠는 제공하지 않는다.
- 작품 정보와 원본 페이지 링크만 제공한다.
- 썸네일 저장 및 사용 범위는 운영 전 별도 검토한다.
- 상세 페이지에는 원본 출처를 명확히 표시한다.

### 18.2 출처 표기

예시:

```text
출처: 네이버 웹툰
원본 보러가기
```

### 18.3 트래픽 관리

이미지 파일을 직접 저장하는 경우 트래픽과 저장 비용이 발생할 수 있습니다.

향후 트래픽 증가 시 다음 방식을 고려합니다.

- 이미지 CDN 사용
- 이미지 리사이징
- 썸네일 캐싱
- 오래된 이미지 정리

### 18.4 SEO

실제 운영을 고려하여 SEO를 적용합니다.

- 웹툰 상세 페이지별 고유 URL 제공
- title/meta description 설정
- 사이트맵 생성
- robots.txt 설정
- Open Graph 이미지 설정
