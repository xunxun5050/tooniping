INSERT INTO platforms (code, name, base_url, is_active, created_at, updated_at)
VALUES ('NAVER_WEBTOON', '네이버 웹툰', 'https://comic.naver.com', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    base_url = VALUES(base_url),
    is_active = VALUES(is_active),
    updated_at = VALUES(updated_at);

INSERT INTO platforms (code, name, base_url, is_active, created_at, updated_at)
VALUES ('KAKAO_WEBTOON', '카카오 웹툰', 'https://webtoon.kakao.com', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    base_url = VALUES(base_url),
    is_active = VALUES(is_active),
    updated_at = VALUES(updated_at);

INSERT INTO weekdays (code, name, sort_order)
VALUES ('MONDAY', '월요일', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order);

INSERT INTO weekdays (code, name, sort_order)
VALUES ('TUESDAY', '화요일', 2)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order);

INSERT INTO weekdays (code, name, sort_order)
VALUES ('WEDNESDAY', '수요일', 3)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order);

INSERT INTO weekdays (code, name, sort_order)
VALUES ('THURSDAY', '목요일', 4)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order);

INSERT INTO weekdays (code, name, sort_order)
VALUES ('FRIDAY', '금요일', 5)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order);

INSERT INTO weekdays (code, name, sort_order)
VALUES ('SATURDAY', '토요일', 6)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order);

INSERT INTO weekdays (code, name, sort_order)
VALUES ('SUNDAY', '일요일', 7)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order);

INSERT INTO weekdays (code, name, sort_order)
VALUES ('DAILY_PLUS', '매일+', 8)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order);

INSERT INTO weekdays (code, name, sort_order)
VALUES ('COMPLETED', '완결', 9)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order);

INSERT INTO genres (code, name, sort_order, is_active, created_at, updated_at)
VALUES ('FANTASY', '판타지', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order),
    is_active = VALUES(is_active),
    updated_at = VALUES(updated_at);

INSERT INTO genres (code, name, sort_order, is_active, created_at, updated_at)
VALUES ('ROMANCE', '로맨스', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order),
    is_active = VALUES(is_active),
    updated_at = VALUES(updated_at);

INSERT INTO genres (code, name, sort_order, is_active, created_at, updated_at)
VALUES ('ACTION', '액션', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order),
    is_active = VALUES(is_active),
    updated_at = VALUES(updated_at);

INSERT INTO genres (code, name, sort_order, is_active, created_at, updated_at)
VALUES ('DAILY', '일상', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order),
    is_active = VALUES(is_active),
    updated_at = VALUES(updated_at);

INSERT INTO genres (code, name, sort_order, is_active, created_at, updated_at)
VALUES ('COMEDY', '개그', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order),
    is_active = VALUES(is_active),
    updated_at = VALUES(updated_at);

INSERT INTO genres (code, name, sort_order, is_active, created_at, updated_at)
VALUES ('THRILLER', '스릴러', 6, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order),
    is_active = VALUES(is_active),
    updated_at = VALUES(updated_at);

INSERT INTO genres (code, name, sort_order, is_active, created_at, updated_at)
VALUES ('DRAMA', '드라마', 7, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order),
    is_active = VALUES(is_active),
    updated_at = VALUES(updated_at);

INSERT INTO genres (code, name, sort_order, is_active, created_at, updated_at)
VALUES ('SPORTS', '스포츠', 8, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order),
    is_active = VALUES(is_active),
    updated_at = VALUES(updated_at);
