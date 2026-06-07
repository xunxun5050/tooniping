package com.webtoonhub.favorite.service;

import com.webtoonhub.common.exception.NotFoundException;
import com.webtoonhub.favorite.dto.FavoriteWebtoonDto;
import com.webtoonhub.webtoon.dto.CodeNameDto;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {

    private static final Map<String, String> STATUS_NAMES = Map.of(
        "ONGOING", "연재중",
        "COMPLETED", "완결",
        "HIATUS", "휴재",
        "UNKNOWN", "알 수 없음"
    );

    private final NamedParameterJdbcTemplate jdbc;

    public FavoriteService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<FavoriteWebtoonDto> getFavorites(String username) {
        String sql = """
            SELECT
              f.webtoon_id AS id,
              w.title,
              COALESCE(w.author, '작가 미상') AS author,
              w.status,
              w.original_url,
              COALESCE(img.stored_path, img.source_url) AS thumbnail_url,
              f.created_at AS added_at
            FROM user_favorite_webtoons f
            JOIN webtoons w ON w.id = f.webtoon_id
            LEFT JOIN webtoon_images img
              ON img.webtoon_id = w.id
             AND img.is_primary = TRUE
             AND img.image_type = 'THUMBNAIL'
            WHERE f.username = :username
            ORDER BY f.created_at DESC, f.webtoon_id DESC
            """;

        List<FavoriteBaseRow> rows = jdbc.query(sql,
            new MapSqlParameterSource("username", username),
            favoriteBaseRowMapper());
        return toFavoriteDtos(rows);
    }

    @Transactional
    public FavoriteWebtoonDto addFavorite(String username, long webtoonId) {
        ensureWebtoonExists(webtoonId);

        if (!existsFavorite(username, webtoonId)) {
            String insertSql = """
                INSERT INTO user_favorite_webtoons (username, webtoon_id, created_at)
                VALUES (:username, :webtoonId, CURRENT_TIMESTAMP)
                """;
            jdbc.update(insertSql, new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("webtoonId", webtoonId));
        }

        return getFavoriteOrThrow(username, webtoonId);
    }

    @Transactional
    public void removeFavorite(String username, long webtoonId) {
        String deleteSql = """
            DELETE FROM user_favorite_webtoons
            WHERE username = :username
              AND webtoon_id = :webtoonId
            """;

        jdbc.update(deleteSql, new MapSqlParameterSource()
            .addValue("username", username)
            .addValue("webtoonId", webtoonId));
    }

    private FavoriteWebtoonDto getFavoriteOrThrow(String username, long webtoonId) {
        String sql = """
            SELECT
              f.webtoon_id AS id,
              w.title,
              COALESCE(w.author, '작가 미상') AS author,
              w.status,
              w.original_url,
              COALESCE(img.stored_path, img.source_url) AS thumbnail_url,
              f.created_at AS added_at
            FROM user_favorite_webtoons f
            JOIN webtoons w ON w.id = f.webtoon_id
            LEFT JOIN webtoon_images img
              ON img.webtoon_id = w.id
             AND img.is_primary = TRUE
             AND img.image_type = 'THUMBNAIL'
            WHERE f.username = :username
              AND f.webtoon_id = :webtoonId
            """;

        List<FavoriteBaseRow> rows = jdbc.query(sql, new MapSqlParameterSource()
            .addValue("username", username)
            .addValue("webtoonId", webtoonId), favoriteBaseRowMapper());

        if (rows.isEmpty()) {
            throw new NotFoundException("즐겨찾기 작품을 찾을 수 없습니다.");
        }
        return toFavoriteDtos(rows).get(0);
    }

    private List<FavoriteWebtoonDto> toFavoriteDtos(List<FavoriteBaseRow> rows) {
        List<Long> webtoonIds = rows.stream().map(FavoriteBaseRow::id).toList();
        Map<Long, List<CodeNameDto>> weekdaysByWebtoonId = fetchWeekdaysByWebtoonIds(webtoonIds);

        return rows.stream()
            .map(row -> new FavoriteWebtoonDto(
                row.id(),
                row.title(),
                row.author(),
                row.thumbnailUrl(),
                row.status(),
                toStatusName(row.status()),
                row.originalUrl(),
                weekdaysByWebtoonId.getOrDefault(row.id(), List.of()),
                row.addedAt()
            ))
            .toList();
    }

    private Map<Long, List<CodeNameDto>> fetchWeekdaysByWebtoonIds(List<Long> webtoonIds) {
        if (webtoonIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String sql = """
            SELECT ww.webtoon_id, wd.code, wd.name
            FROM webtoon_weekdays ww
            JOIN weekdays wd ON wd.id = ww.weekday_id
            WHERE ww.webtoon_id IN (:webtoonIds)
            ORDER BY wd.sort_order ASC
            """;

        Map<Long, List<CodeNameDto>> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = jdbc.queryForList(sql, new MapSqlParameterSource("webtoonIds", webtoonIds));
        for (Map<String, Object> row : rows) {
            Long webtoonId = ((Number) row.get("webtoon_id")).longValue();
            result.computeIfAbsent(webtoonId, ignored -> new java.util.ArrayList<>())
                .add(new CodeNameDto((String) row.get("code"), (String) row.get("name")));
        }
        return result;
    }

    private boolean existsFavorite(String username, long webtoonId) {
        String sql = """
            SELECT COUNT(*)
            FROM user_favorite_webtoons
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

    private org.springframework.jdbc.core.RowMapper<FavoriteBaseRow> favoriteBaseRowMapper() {
        return (rs, rowNum) -> {
            Timestamp addedAt = rs.getTimestamp("added_at");
            return new FavoriteBaseRow(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("status"),
                rs.getString("original_url"),
                rs.getString("thumbnail_url"),
                addedAt == null ? null : addedAt.toInstant().toString()
            );
        };
    }

    private String toStatusName(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_NAMES.get("UNKNOWN");
        }
        return STATUS_NAMES.getOrDefault(status, STATUS_NAMES.get("UNKNOWN"));
    }

    private record FavoriteBaseRow(
        Long id,
        String title,
        String author,
        String status,
        String originalUrl,
        String thumbnailUrl,
        String addedAt
    ) {
    }
}
