package com.webtoonhub.auth.service;

import com.webtoonhub.auth.dto.UserProfileDto;
import com.webtoonhub.common.exception.BadRequestException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private static final int MAX_GENERATE_ATTEMPTS = 20;
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[0-9A-Za-z가-힣 ._-]{2,24}$");
    private static final String[] MOODS = {
        "반짝이는", "말랑한", "용감한", "느긋한", "수상한", "달콤한", "우주맛", "새벽의", "폭풍의", "싱싱한"
    };
    private static final String[] OBJECTS = {
        "만두", "젤리", "라면", "별사탕", "고구마", "쿠키", "소다", "도넛", "김밥", "푸딩"
    };
    private static final String[] ROLES = {
        "탐정", "기사", "마법사", "선장", "박사", "작가", "연금술사", "수집가", "연구원", "감독"
    };
    private static final String[] FALLBACK_NICKNAMES = {
        "웹툰탐험가", "웹툰수집가", "웹툰항해자", "웹툰연구원", "웹툰감별사",
        "만화방지기", "별빛독자", "쿠키수호자", "장면수집가", "페이지여행자"
    };

    private final NamedParameterJdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();

    public UserProfileService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UserProfileDto ensureProfile(
        String username,
        String provider,
        String providerUserId,
        String sourceNickname
    ) {
        String normalizedUsername = normalizeUsername(username);
        UserProfileDto existing = findProfile(normalizedUsername);
        if (existing != null) {
            touchProviderInfo(normalizedUsername, provider, providerUserId, sourceNickname);
            return findProfile(normalizedUsername);
        }

        String nickname = generateUniqueNickname();
        String sql = """
            INSERT INTO user_profiles (
              username,
              nickname,
              provider,
              provider_user_id,
              source_nickname,
              created_at,
              updated_at
            )
            VALUES (
              :username,
              :nickname,
              :provider,
              :providerUserId,
              :sourceNickname,
              CURRENT_TIMESTAMP,
              CURRENT_TIMESTAMP
            )
            """;

        try {
            jdbc.update(sql, new MapSqlParameterSource()
                .addValue("username", normalizedUsername)
                .addValue("nickname", nickname)
                .addValue("provider", normalizeNullable(provider))
                .addValue("providerUserId", normalizeNullable(providerUserId))
                .addValue("sourceNickname", normalizeNullable(sourceNickname)));
        } catch (DuplicateKeyException ignored) {
            return ensureProfile(normalizedUsername, provider, providerUserId, sourceNickname);
        }

        return findProfile(normalizedUsername);
    }

    public UserProfileDto getOrCreateProfile(String username) {
        return ensureProfile(username, null, null, null);
    }

    public UserProfileDto updateNickname(String username, String nickname) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedNickname = normalizeNickname(nickname);

        UserProfileDto profile = getOrCreateProfile(normalizedUsername);
        if (profile.nickname().equals(normalizedNickname)) {
            return profile;
        }
        if (nicknameExists(normalizedNickname)) {
            throw new BadRequestException("이미 사용 중인 닉네임입니다.");
        }

        String sql = """
            UPDATE user_profiles
            SET nickname = :nickname,
                updated_at = CURRENT_TIMESTAMP
            WHERE username = :username
            """;
        try {
            jdbc.update(sql, new MapSqlParameterSource()
                .addValue("username", normalizedUsername)
                .addValue("nickname", normalizedNickname));
        } catch (DuplicateKeyException e) {
            throw new BadRequestException("이미 사용 중인 닉네임입니다.");
        }
        return findProfile(normalizedUsername);
    }

    @Transactional
    public void deleteAccount(String username) {
        String normalizedUsername = normalizeUsername(username);
        MapSqlParameterSource params = new MapSqlParameterSource("username", normalizedUsername);

        jdbc.update("DELETE FROM user_favorite_webtoons WHERE username = :username", params);
        jdbc.update("DELETE FROM user_profiles WHERE username = :username", params);
    }

    private void touchProviderInfo(String username, String provider, String providerUserId, String sourceNickname) {
        String normalizedProvider = normalizeNullable(provider);
        String normalizedProviderUserId = normalizeNullable(providerUserId);
        String normalizedSourceNickname = normalizeNullable(sourceNickname);
        if (normalizedProvider == null && normalizedProviderUserId == null && normalizedSourceNickname == null) {
            return;
        }

        String sql = """
            UPDATE user_profiles
            SET provider = COALESCE(:provider, provider),
                provider_user_id = COALESCE(:providerUserId, provider_user_id),
                source_nickname = COALESCE(:sourceNickname, source_nickname),
                updated_at = CURRENT_TIMESTAMP
            WHERE username = :username
            """;
        jdbc.update(sql, new MapSqlParameterSource()
            .addValue("username", username)
            .addValue("provider", normalizedProvider)
            .addValue("providerUserId", normalizedProviderUserId)
            .addValue("sourceNickname", normalizedSourceNickname));
    }

    private UserProfileDto findProfile(String username) {
        String sql = """
            SELECT username, nickname, provider, created_at, updated_at
            FROM user_profiles
            WHERE username = :username
            """;
        List<UserProfileDto> profiles = jdbc.query(sql,
            new MapSqlParameterSource("username", username),
            (rs, rowNum) -> new UserProfileDto(
                rs.getString("username"),
                rs.getString("nickname"),
                rs.getString("provider"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at"))
            ));
        return profiles.isEmpty() ? null : profiles.get(0);
    }

    private String generateUniqueNickname() {
        for (int attempt = 0; attempt < MAX_GENERATE_ATTEMPTS; attempt++) {
            String nickname = MOODS[random.nextInt(MOODS.length)]
                + OBJECTS[random.nextInt(OBJECTS.length)]
                + ROLES[random.nextInt(ROLES.length)];
            if (!nicknameExists(nickname)) {
                return nickname;
            }
        }
        for (String nickname : FALLBACK_NICKNAMES) {
            if (!nicknameExists(nickname)) {
                return nickname;
            }
        }
        throw new BadRequestException("사용 가능한 자동 닉네임을 만들지 못했습니다.");
    }

    private boolean nicknameExists(String nickname) {
        String sql = "SELECT COUNT(*) FROM user_profiles WHERE nickname = :nickname";
        Long count = jdbc.queryForObject(sql, new MapSqlParameterSource("nickname", nickname), Long.class);
        return count != null && count > 0;
    }

    private String normalizeNickname(String nickname) {
        if (nickname == null) {
            throw new BadRequestException("닉네임을 입력해 주세요.");
        }
        String normalized = nickname.trim().replaceAll("\\s+", " ");
        if (!NICKNAME_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException("닉네임은 한글, 영문, 숫자, 공백, ., _, -만 사용할 수 있습니다.");
        }
        return normalized;
    }

    private String normalizeUsername(String username) {
        String normalized = normalizeNullable(username);
        if (normalized == null) {
            throw new BadRequestException("사용자 정보가 올바르지 않습니다.");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
