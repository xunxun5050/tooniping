package com.webtoonhub.evaluation.service;

import com.webtoonhub.common.exception.BadRequestException;
import com.webtoonhub.common.exception.NotFoundException;
import com.webtoonhub.evaluation.dto.WebtoonEvaluationDto;
import com.webtoonhub.evaluation.dto.WebtoonEvaluationRequest;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebtoonEvaluationService {

    private static final Set<String> ALLOWED_RATINGS = Set.of("SSS", "S", "A", "B", "C", "D", "F");
    private static final Set<String> ALLOWED_EMOTION_TAGS = Set.of(
        "웃김", "울림", "설렘", "소름", "충격", "힐링", "도파민", "여운"
    );

    private final NamedParameterJdbcTemplate jdbc;

    public WebtoonEvaluationService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<WebtoonEvaluationDto> getEvaluations(String username) {
        String sql = """
            SELECT
              e.webtoon_id,
              w.title,
              COALESCE(w.author, '작가 미상') AS author,
              COALESCE(img.stored_path, img.source_url) AS thumbnail_url,
              e.rating,
              e.emotion_tags,
              e.created_at,
              e.updated_at
            FROM user_webtoon_evaluations e
            JOIN webtoons w ON w.id = e.webtoon_id
            LEFT JOIN webtoon_images img
              ON img.webtoon_id = w.id
             AND img.is_primary = TRUE
             AND img.image_type = 'THUMBNAIL'
            WHERE e.username = :username
            ORDER BY e.updated_at DESC, e.webtoon_id DESC
            """;

        return jdbc.query(sql, new MapSqlParameterSource("username", username), evaluationRowMapper());
    }

    public WebtoonEvaluationDto getEvaluation(String username, long webtoonId) {
        String sql = """
            SELECT
              e.webtoon_id,
              w.title,
              COALESCE(w.author, '작가 미상') AS author,
              COALESCE(img.stored_path, img.source_url) AS thumbnail_url,
              e.rating,
              e.emotion_tags,
              e.created_at,
              e.updated_at
            FROM user_webtoon_evaluations e
            JOIN webtoons w ON w.id = e.webtoon_id
            LEFT JOIN webtoon_images img
              ON img.webtoon_id = w.id
             AND img.is_primary = TRUE
             AND img.image_type = 'THUMBNAIL'
            WHERE e.username = :username
              AND e.webtoon_id = :webtoonId
            """;

        List<WebtoonEvaluationDto> rows = jdbc.query(sql, new MapSqlParameterSource()
            .addValue("username", username)
            .addValue("webtoonId", webtoonId), evaluationRowMapper());
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Transactional
    public WebtoonEvaluationDto saveEvaluation(String username, long webtoonId, WebtoonEvaluationRequest request) {
        ensureWebtoonExists(webtoonId);
        String rating = normalizeRating(request == null ? null : request.rating());
        List<String> emotionTags = normalizeEmotionTags(request == null ? null : request.emotionTags());
        String emotionTagsText = String.join(",", emotionTags);

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("username", username)
            .addValue("webtoonId", webtoonId)
            .addValue("rating", rating)
            .addValue("emotionTags", emotionTagsText);

        if (existsEvaluation(username, webtoonId)) {
            String updateSql = """
                UPDATE user_webtoon_evaluations
                SET rating = :rating,
                    emotion_tags = :emotionTags,
                    updated_at = CURRENT_TIMESTAMP
                WHERE username = :username
                  AND webtoon_id = :webtoonId
                """;
            jdbc.update(updateSql, params);
        } else {
            String insertSql = """
                INSERT INTO user_webtoon_evaluations (
                  username,
                  webtoon_id,
                  rating,
                  emotion_tags,
                  created_at,
                  updated_at
                )
                VALUES (
                  :username,
                  :webtoonId,
                  :rating,
                  :emotionTags,
                  CURRENT_TIMESTAMP,
                  CURRENT_TIMESTAMP
                )
                """;
            jdbc.update(insertSql, params);
        }

        return getEvaluation(username, webtoonId);
    }

    @Transactional
    public void deleteEvaluation(String username, long webtoonId) {
        String sql = """
            DELETE FROM user_webtoon_evaluations
            WHERE username = :username
              AND webtoon_id = :webtoonId
            """;

        jdbc.update(sql, new MapSqlParameterSource()
            .addValue("username", username)
            .addValue("webtoonId", webtoonId));
    }

    private boolean existsEvaluation(String username, long webtoonId) {
        String sql = """
            SELECT COUNT(*)
            FROM user_webtoon_evaluations
            WHERE username = :username
              AND webtoon_id = :webtoonId
            """;

        Long count = jdbc.queryForObject(sql, new MapSqlParameterSource()
            .addValue("username", username)
            .addValue("webtoonId", webtoonId), Long.class);
        return count != null && count > 0;
    }

    private void ensureWebtoonExists(long webtoonId) {
        String sql = """
            SELECT COUNT(*)
            FROM webtoons
            WHERE id = :webtoonId
              AND is_active = TRUE
            """;

        Long count = jdbc.queryForObject(sql, new MapSqlParameterSource("webtoonId", webtoonId), Long.class);
        if (count == null || count == 0L) {
            throw new NotFoundException("웹툰을 찾을 수 없습니다.");
        }
    }

    private String normalizeRating(String rating) {
        String normalized = rating == null ? "" : rating.trim().toUpperCase();
        if (!ALLOWED_RATINGS.contains(normalized)) {
            throw new BadRequestException("레이팅은 SSS, S, A, B, C, D, F 중에서 선택해 주세요.");
        }
        return normalized;
    }

    private List<String> normalizeEmotionTags(List<String> emotionTags) {
        if (emotionTags == null || emotionTags.isEmpty()) {
            throw new BadRequestException("감정 태그를 하나 이상 선택해 주세요.");
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : emotionTags) {
            String text = tag == null ? "" : tag.trim();
            if (text.isEmpty()) {
                continue;
            }
            if (!ALLOWED_EMOTION_TAGS.contains(text)) {
                throw new BadRequestException("지원하지 않는 감정 태그입니다.");
            }
            normalized.add(text);
        }

        if (normalized.isEmpty()) {
            throw new BadRequestException("감정 태그를 하나 이상 선택해 주세요.");
        }
        return List.copyOf(normalized);
    }

    private RowMapper<WebtoonEvaluationDto> evaluationRowMapper() {
        return (rs, rowNum) -> new WebtoonEvaluationDto(
            rs.getLong("webtoon_id"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("thumbnail_url"),
            rs.getString("rating"),
            splitEmotionTags(rs.getString("emotion_tags")),
            toInstantString(rs.getTimestamp("created_at")),
            toInstantString(rs.getTimestamp("updated_at"))
        );
    }

    private List<String> splitEmotionTags(String emotionTags) {
        if (emotionTags == null || emotionTags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(emotionTags.split(","))
            .map(String::trim)
            .filter(tag -> !tag.isEmpty())
            .toList();
    }

    private String toInstantString(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().toString();
    }
}
