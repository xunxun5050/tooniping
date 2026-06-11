CREATE TABLE IF NOT EXISTS platforms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS webtoons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform_id BIGINT NOT NULL,
    external_id VARCHAR(100),
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255),
    description TEXT,
    original_url VARCHAR(700) NOT NULL,
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

CREATE TABLE IF NOT EXISTS genres (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS webtoon_genres (
    webtoon_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (webtoon_id, genre_id),
    CONSTRAINT fk_webtoon_genres_webtoon
        FOREIGN KEY (webtoon_id) REFERENCES webtoons(id),
    CONSTRAINT fk_webtoon_genres_genre
        FOREIGN KEY (genre_id) REFERENCES genres(id)
);

CREATE TABLE IF NOT EXISTS weekdays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL
);

CREATE TABLE IF NOT EXISTS webtoon_weekdays (
    webtoon_id BIGINT NOT NULL,
    weekday_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (webtoon_id, weekday_id),
    CONSTRAINT fk_webtoon_weekdays_webtoon
        FOREIGN KEY (webtoon_id) REFERENCES webtoons(id),
    CONSTRAINT fk_webtoon_weekdays_weekday
        FOREIGN KEY (weekday_id) REFERENCES weekdays(id)
);

CREATE TABLE IF NOT EXISTS webtoon_popularity_rankings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    webtoon_id BIGINT NOT NULL,
    ranking_type VARCHAR(30) NOT NULL,
    ranking_key VARCHAR(50) NOT NULL,
    rank_position INT NOT NULL,
    collected_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_webtoon_popularity_rankings_webtoon
        FOREIGN KEY (webtoon_id) REFERENCES webtoons(id),
    CONSTRAINT uk_webtoon_popularity_rankings_scope
        UNIQUE (webtoon_id, ranking_type, ranking_key),
    INDEX idx_webtoon_popularity_rankings_lookup
        (ranking_type, ranking_key, rank_position)
);

CREATE TABLE IF NOT EXISTS user_profiles (
    username VARCHAR(100) PRIMARY KEY,
    nickname VARCHAR(40) NOT NULL UNIQUE,
    avatar_seed VARCHAR(80) NOT NULL,
    avatar_palette VARCHAR(30) NOT NULL,
    provider VARCHAR(30),
    provider_user_id VARCHAR(100),
    source_nickname VARCHAR(100),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS user_accounts (
    username VARCHAR(100) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS user_refresh_tokens (
    token_hash VARCHAR(128) PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    last_used_at DATETIME,
    INDEX idx_user_refresh_tokens_username (username),
    INDEX idx_user_refresh_tokens_expires_at (expires_at)
);

CREATE TABLE IF NOT EXISTS email_verification_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    expires_at DATETIME NOT NULL,
    verified_at DATETIME,
    created_at DATETIME NOT NULL,
    INDEX idx_email_verification_lookup (email, purpose, expires_at),
    INDEX idx_email_verification_verified (email, purpose, verified_at)
);

CREATE TABLE IF NOT EXISTS webtoon_images (
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

CREATE TABLE IF NOT EXISTS crawl_histories (
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

CREATE TABLE IF NOT EXISTS crawl_failures (
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

CREATE TABLE IF NOT EXISTS user_favorite_webtoons (
    username VARCHAR(100) NOT NULL,
    webtoon_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (username, webtoon_id),
    CONSTRAINT fk_user_favorite_webtoons_webtoon
        FOREIGN KEY (webtoon_id) REFERENCES webtoons(id)
);

CREATE TABLE IF NOT EXISTS user_webtoon_evaluations (
    username VARCHAR(100) NOT NULL,
    webtoon_id BIGINT NOT NULL,
    rating VARCHAR(10) NOT NULL,
    emotion_tags VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (username, webtoon_id),
    CONSTRAINT fk_user_webtoon_evaluations_webtoon
        FOREIGN KEY (webtoon_id) REFERENCES webtoons(id)
);
