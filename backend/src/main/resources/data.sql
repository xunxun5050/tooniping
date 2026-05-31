MERGE INTO platforms (code, name, base_url, is_active, created_at, updated_at)
KEY(code)
VALUES ('NAVER_WEBTOON', '네이버 웹툰', 'https://comic.naver.com', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO weekdays (code, name, sort_order)
KEY(code)
VALUES ('MONDAY', '월요일', 1);

MERGE INTO weekdays (code, name, sort_order)
KEY(code)
VALUES ('TUESDAY', '화요일', 2);

MERGE INTO weekdays (code, name, sort_order)
KEY(code)
VALUES ('WEDNESDAY', '수요일', 3);

MERGE INTO weekdays (code, name, sort_order)
KEY(code)
VALUES ('THURSDAY', '목요일', 4);

MERGE INTO weekdays (code, name, sort_order)
KEY(code)
VALUES ('FRIDAY', '금요일', 5);

MERGE INTO weekdays (code, name, sort_order)
KEY(code)
VALUES ('SATURDAY', '토요일', 6);

MERGE INTO weekdays (code, name, sort_order)
KEY(code)
VALUES ('SUNDAY', '일요일', 7);

MERGE INTO weekdays (code, name, sort_order)
KEY(code)
VALUES ('COMPLETED', '완결', 8);

MERGE INTO genres (code, name, sort_order, is_active, created_at, updated_at)
KEY(code)
VALUES ('FANTASY', '판타지', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO genres (code, name, sort_order, is_active, created_at, updated_at)
KEY(code)
VALUES ('ROMANCE', '로맨스', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO genres (code, name, sort_order, is_active, created_at, updated_at)
KEY(code)
VALUES ('ACTION', '액션', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO genres (code, name, sort_order, is_active, created_at, updated_at)
KEY(code)
VALUES ('DAILY', '일상', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO genres (code, name, sort_order, is_active, created_at, updated_at)
KEY(code)
VALUES ('COMEDY', '개그', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO genres (code, name, sort_order, is_active, created_at, updated_at)
KEY(code)
VALUES ('THRILLER', '스릴러', 6, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO genres (code, name, sort_order, is_active, created_at, updated_at)
KEY(code)
VALUES ('DRAMA', '드라마', 7, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO genres (code, name, sort_order, is_active, created_at, updated_at)
KEY(code)
VALUES ('SPORTS', '스포츠', 8, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
