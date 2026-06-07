package com.webtoonhub.webtoon.repository;

import com.webtoonhub.common.exception.NotFoundException;
import com.webtoonhub.webtoon.dto.CodeNameDto;
import com.webtoonhub.webtoon.dto.HomeResponseDto;
import com.webtoonhub.webtoon.dto.MenuCountDto;
import com.webtoonhub.webtoon.dto.PagedResultDto;
import com.webtoonhub.webtoon.dto.PlatformDto;
import com.webtoonhub.webtoon.dto.SimpleWebtoonDto;
import com.webtoonhub.webtoon.dto.ThumbnailDto;
import com.webtoonhub.webtoon.dto.WebtoonDetailDto;
import com.webtoonhub.webtoon.dto.WebtoonFiltersDto;
import com.webtoonhub.webtoon.dto.WebtoonListItemDto;
import com.webtoonhub.webtoon.dto.WebtoonSearchCondition;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WebtoonQueryRepository {

    private static final Map<String, String> STATUS_NAMES = Map.of(
        "ONGOING", "연재중",
        "COMPLETED", "완결",
        "HIATUS", "휴재",
        "UNKNOWN", "알 수 없음"
    );
    private static final String POPULARITY_GLOBAL_TYPE = "GLOBAL";
    private static final String POPULARITY_GLOBAL_KEY = "ALL";
    private static final String POPULARITY_GENRE_TYPE = "GENRE";
    private static final String POPULARITY_WEEKDAY_TYPE = "WEEKDAY";
    private static final String POPULARITY_STATUS_TYPE = "STATUS";

    private final NamedParameterJdbcTemplate jdbc;

    public WebtoonQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PagedResultDto<WebtoonListItemDto> findWebtoons(WebtoonSearchCondition condition) {
        return findWebtoonsInternal(condition, false);
    }

    public PagedResultDto<WebtoonListItemDto> findAdminWebtoons(String keyword, int page, int size) {
        WebtoonSearchCondition condition = new WebtoonSearchCondition(
            keyword,
            null,
            null,
            null,
            null,
            page,
            size,
            "updated"
        );
        return findWebtoonsInternal(condition, true);
    }

    private PagedResultDto<WebtoonListItemDto> findWebtoonsInternal(WebtoonSearchCondition condition, boolean includeInactive) {
        int safePage = Math.max(condition.page(), 0);
        int safeSize = clampSize(condition.size());
        MapSqlParameterSource params = new MapSqlParameterSource();
        String normalizedSort = normalizeSort(condition.sort());

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        if (!includeInactive) {
            where.append(" AND w.is_active = TRUE ");
        }

        if (hasText(condition.keyword())) {
            where.append(" AND (LOWER(w.title) LIKE :keyword OR LOWER(COALESCE(w.author, '')) LIKE :keyword) ");
            params.addValue("keyword", "%" + condition.keyword().toLowerCase() + "%");
        }
        if (hasText(condition.platform())) {
            where.append(" AND p.code = :platform ");
            params.addValue("platform", condition.platform());
        }
        if (hasText(condition.genre())) {
            where.append(" AND EXISTS (SELECT 1 FROM webtoon_genres wg2 JOIN genres g2 ON g2.id = wg2.genre_id WHERE wg2.webtoon_id = w.id AND g2.code = :genre) ");
            params.addValue("genre", condition.genre());
        }
        if (hasText(condition.weekday())) {
            where.append(" AND EXISTS (SELECT 1 FROM webtoon_weekdays ww2 JOIN weekdays wd2 ON wd2.id = ww2.weekday_id WHERE ww2.webtoon_id = w.id AND wd2.code = :weekday) ");
            params.addValue("weekday", condition.weekday());
        }
        if (hasText(condition.status())) {
            where.append(" AND w.status = :status ");
            params.addValue("status", condition.status());
        }

        String countSql = """
            SELECT COUNT(*)
            FROM webtoons w
            JOIN platforms p ON p.id = w.platform_id
            """ + where;

        Long totalElements = jdbc.queryForObject(countSql, params, Long.class);
        long total = totalElements == null ? 0L : totalElements;

        String popularityJoin = "";
        if ("popular".equals(normalizedSort)) {
            PopularityScope popularityScope = resolvePopularityScope(condition);
            params.addValue("popularityType", popularityScope.rankingType());
            params.addValue("popularityKey", popularityScope.rankingKey());
            popularityJoin = """
                LEFT JOIN webtoon_popularity_rankings pop
                  ON pop.webtoon_id = w.id
                 AND pop.ranking_type = :popularityType
                 AND pop.ranking_key = :popularityKey
                """;
        }

        String orderBy = switch (normalizedSort) {
            case "popular" -> """
                 ORDER BY
                   CASE WHEN pop.rank_position IS NULL THEN 1 ELSE 0 END ASC,
                   pop.rank_position ASC,
                   p.id ASC,
                   w.created_at DESC,
                   w.id DESC
                """;
            case "title" -> " ORDER BY w.title ASC, w.id DESC ";
            case "weekday" -> " ORDER BY (SELECT MIN(wd3.sort_order) FROM webtoon_weekdays ww3 JOIN weekdays wd3 ON wd3.id = ww3.weekday_id WHERE ww3.webtoon_id = w.id) ASC, w.title ASC ";
            case "updated" -> " ORDER BY w.updated_at DESC, w.id DESC ";
            default -> " ORDER BY w.created_at DESC, w.id DESC ";
        };

        String selectSql = """
            SELECT
              w.id,
              w.title,
              w.author,
              w.description,
              w.status,
              w.original_url,
              w.last_crawled_at,
              w.created_at,
              w.updated_at,
              w.is_active,
              p.code AS platform_code,
              p.name AS platform_name,
              p.base_url AS platform_base_url,
              COALESCE(img.stored_path, img.source_url) AS thumbnail_url
            FROM webtoons w
            JOIN platforms p ON p.id = w.platform_id
            LEFT JOIN webtoon_images img ON img.webtoon_id = w.id AND img.is_primary = TRUE AND img.image_type = 'THUMBNAIL'
            """ + popularityJoin + where + orderBy + " LIMIT :size OFFSET :offset";

        params.addValue("size", safeSize);
        params.addValue("offset", safePage * safeSize);

        List<BaseWebtoonRow> rows = jdbc.query(selectSql, params, baseWebtoonRowMapper());

        List<Long> ids = rows.stream().map(BaseWebtoonRow::id).toList();
        Map<Long, List<CodeNameDto>> genresByWebtoonId = fetchGenresByWebtoonIds(ids);
        Map<Long, List<CodeNameDto>> weekdaysByWebtoonId = fetchWeekdaysByWebtoonIds(ids);

        List<WebtoonListItemDto> content = rows.stream()
            .map(row -> new WebtoonListItemDto(
                row.id,
                row.title,
                row.author,
                row.description,
                new PlatformDto(row.platformCode, row.platformName, null),
                genresByWebtoonId.getOrDefault(row.id, List.of()),
                weekdaysByWebtoonId.getOrDefault(row.id, List.of()),
                row.status,
                toStatusName(row.status),
                row.thumbnailUrl,
                row.originalUrl
            ))
            .toList();

        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        boolean hasNext = safePage + 1 < totalPages;

        return new PagedResultDto<>(content, safePage, safeSize, total, totalPages, hasNext);
    }

    public Optional<WebtoonDetailDto> findWebtoonDetail(long webtoonId) {
        return findWebtoonDetail(webtoonId, false);
    }

    public Optional<WebtoonDetailDto> findWebtoonDetail(long webtoonId, boolean includeInactive) {
        String detailSql = """
            SELECT
              w.id,
              w.title,
              w.author,
              w.description,
              w.status,
              w.original_url,
              w.last_crawled_at,
              w.created_at,
              w.updated_at,
              p.code AS platform_code,
              p.name AS platform_name,
              p.base_url AS platform_base_url
            FROM webtoons w
            JOIN platforms p ON p.id = w.platform_id
            WHERE w.id = :webtoonId
            """ + (includeInactive ? "" : " AND w.is_active = TRUE ");

        MapSqlParameterSource params = new MapSqlParameterSource("webtoonId", webtoonId);
        List<DetailBaseRow> baseRows = jdbc.query(detailSql, params, detailBaseRowMapper());
        if (baseRows.isEmpty()) {
            return Optional.empty();
        }

        DetailBaseRow base = baseRows.get(0);
        List<Long> ids = List.of(webtoonId);
        List<CodeNameDto> genres = fetchGenresByWebtoonIds(ids).getOrDefault(webtoonId, List.of());
        List<CodeNameDto> weekdays = fetchWeekdaysByWebtoonIds(ids).getOrDefault(webtoonId, List.of());
        ThumbnailDto thumbnail = fetchThumbnail(webtoonId);

        return Optional.of(new WebtoonDetailDto(
            base.id,
            base.title,
            base.author,
            base.description,
            new PlatformDto(base.platformCode, base.platformName, base.platformBaseUrl),
            genres,
            weekdays,
            base.status,
            toStatusName(base.status),
            thumbnail,
            base.originalUrl,
            base.lastCrawledAt,
            base.createdAt,
            base.updatedAt
        ));
    }

    public WebtoonDetailDto getWebtoonDetailOrThrow(long webtoonId, boolean includeInactive) {
        return findWebtoonDetail(webtoonId, includeInactive)
            .orElseThrow(() -> new NotFoundException("웹툰을 찾을 수 없습니다."));
    }

    public List<WebtoonListItemDto> findSimilarWebtoons(long webtoonId, int size) {
        int safeSize = Math.max(Math.min(size, 30), 1);

        String genreIdSql = """
            SELECT genre_id
            FROM webtoon_genres
            WHERE webtoon_id = :webtoonId
            """;
        List<Long> genreIds = jdbc.query(genreIdSql, new MapSqlParameterSource("webtoonId", webtoonId),
            (rs, rowNum) -> rs.getLong("genre_id"));

        if (genreIds.isEmpty()) {
            return List.of();
        }

        String similarIdSql = """
            SELECT w.id
            FROM webtoons w
            JOIN webtoon_genres wg ON wg.webtoon_id = w.id
            WHERE w.is_active = TRUE
              AND w.id <> :webtoonId
              AND wg.genre_id IN (:genreIds)
            GROUP BY w.id, w.updated_at
            ORDER BY w.updated_at DESC, w.id DESC
            LIMIT :size
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("webtoonId", webtoonId)
            .addValue("genreIds", genreIds)
            .addValue("size", safeSize);

        List<Long> ids = jdbc.query(similarIdSql, params, (rs, rowNum) -> rs.getLong("id"));
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, WebtoonListItemDto> dataById = fetchWebtoonCardsByIds(ids);
        return ids.stream()
            .map(dataById::get)
            .filter(Objects::nonNull)
            .toList();
    }

    public HomeResponseDto findHomeData() {
        String recentSql = """
            SELECT
              w.id,
              w.title,
              w.author,
              COALESCE(img.stored_path, img.source_url) AS thumbnail_url
            FROM webtoons w
            LEFT JOIN webtoon_images img ON img.webtoon_id = w.id AND img.is_primary = TRUE AND img.image_type = 'THUMBNAIL'
            WHERE w.is_active = TRUE
            ORDER BY w.created_at DESC, w.id DESC
            LIMIT 8
            """;

        List<SimpleWebtoonDto> recentWebtoons = jdbc.query(recentSql, (rs, rowNum) -> new SimpleWebtoonDto(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("thumbnail_url")
        ));

        String weekdayMenuSql = """
            SELECT
              wd.code,
              wd.name,
              COUNT(DISTINCT w.id) AS count
            FROM weekdays wd
            LEFT JOIN webtoon_weekdays ww ON ww.weekday_id = wd.id
            LEFT JOIN webtoons w ON w.id = ww.webtoon_id AND w.is_active = TRUE
            GROUP BY wd.id, wd.code, wd.name, wd.sort_order
            ORDER BY wd.sort_order ASC
            """;

        List<MenuCountDto> weekdayMenus = jdbc.query(weekdayMenuSql, (rs, rowNum) -> new MenuCountDto(
            rs.getString("code"),
            rs.getString("name"),
            rs.getLong("count")
        ));

        String genreMenuSql = """
            SELECT
              g.code,
              g.name,
              COUNT(DISTINCT w.id) AS count
            FROM genres g
            LEFT JOIN webtoon_genres wg ON wg.genre_id = g.id
            LEFT JOIN webtoons w ON w.id = wg.webtoon_id AND w.is_active = TRUE
            WHERE g.is_active = TRUE
            GROUP BY g.id, g.code, g.name, g.sort_order
            ORDER BY g.sort_order ASC
            """;

        List<MenuCountDto> genreMenus = jdbc.query(genreMenuSql, (rs, rowNum) -> new MenuCountDto(
            rs.getString("code"),
            rs.getString("name"),
            rs.getLong("count")
        ));

        return new HomeResponseDto(recentWebtoons, weekdayMenus, genreMenus);
    }

    public WebtoonFiltersDto findFilters() {
        List<CodeNameDto> platforms = jdbc.query(
            """
                SELECT code, name
                FROM platforms
                WHERE is_active = TRUE
                ORDER BY id ASC
                """,
            (rs, rowNum) -> new CodeNameDto(rs.getString("code"), rs.getString("name"))
        );

        List<CodeNameDto> genres = jdbc.query(
            """
                SELECT code, name
                FROM genres
                WHERE is_active = TRUE
                ORDER BY sort_order ASC
                """,
            (rs, rowNum) -> new CodeNameDto(rs.getString("code"), rs.getString("name"))
        );

        List<CodeNameDto> weekdays = jdbc.query(
            """
                SELECT code, name
                FROM weekdays
                ORDER BY sort_order ASC
                """,
            (rs, rowNum) -> new CodeNameDto(rs.getString("code"), rs.getString("name"))
        );

        List<CodeNameDto> statuses = Arrays.asList(
            new CodeNameDto("ONGOING", "연재중"),
            new CodeNameDto("COMPLETED", "완결"),
            new CodeNameDto("HIATUS", "휴재"),
            new CodeNameDto("UNKNOWN", "알 수 없음")
        );

        return new WebtoonFiltersDto(platforms, genres, weekdays, statuses);
    }

    private Map<Long, WebtoonListItemDto> fetchWebtoonCardsByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }

        String sql = """
            SELECT
              w.id,
              w.title,
              w.author,
              w.description,
              w.status,
              w.original_url,
              w.last_crawled_at,
              w.created_at,
              w.updated_at,
              w.is_active,
              p.code AS platform_code,
              p.name AS platform_name,
              p.base_url AS platform_base_url,
              COALESCE(img.stored_path, img.source_url) AS thumbnail_url
            FROM webtoons w
            JOIN platforms p ON p.id = w.platform_id
            LEFT JOIN webtoon_images img ON img.webtoon_id = w.id AND img.is_primary = TRUE AND img.image_type = 'THUMBNAIL'
            WHERE w.id IN (:ids)
            """;

        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
        List<BaseWebtoonRow> rows = jdbc.query(sql, params, baseWebtoonRowMapper());

        Map<Long, List<CodeNameDto>> genresByWebtoonId = fetchGenresByWebtoonIds(ids);
        Map<Long, List<CodeNameDto>> weekdaysByWebtoonId = fetchWeekdaysByWebtoonIds(ids);

        Map<Long, WebtoonListItemDto> result = new HashMap<>();
        for (BaseWebtoonRow row : rows) {
            result.put(row.id, new WebtoonListItemDto(
                row.id,
                row.title,
                row.author,
                row.description,
                new PlatformDto(row.platformCode, row.platformName, null),
                genresByWebtoonId.getOrDefault(row.id, List.of()),
                weekdaysByWebtoonId.getOrDefault(row.id, List.of()),
                row.status,
                toStatusName(row.status),
                row.thumbnailUrl,
                row.originalUrl
            ));
        }

        return result;
    }

    private Map<Long, List<CodeNameDto>> fetchGenresByWebtoonIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        String sql = """
            SELECT wg.webtoon_id, g.code, g.name
            FROM webtoon_genres wg
            JOIN genres g ON g.id = wg.genre_id
            WHERE wg.webtoon_id IN (:ids)
            ORDER BY g.sort_order ASC
            """;
        return fetchCodeNamesByWebtoonIds(sql, ids);
    }

    private Map<Long, List<CodeNameDto>> fetchWeekdaysByWebtoonIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        String sql = """
            SELECT ww.webtoon_id, wd.code, wd.name
            FROM webtoon_weekdays ww
            JOIN weekdays wd ON wd.id = ww.weekday_id
            WHERE ww.webtoon_id IN (:ids)
            ORDER BY wd.sort_order ASC
            """;
        return fetchCodeNamesByWebtoonIds(sql, ids);
    }

    private Map<Long, List<CodeNameDto>> fetchCodeNamesByWebtoonIds(String sql, List<Long> ids) {
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);

        Map<Long, List<CodeNameDto>> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long webtoonId = ((Number) row.get("webtoon_id")).longValue();
            String code = (String) row.get("code");
            String name = (String) row.get("name");

            result.computeIfAbsent(webtoonId, key -> new ArrayList<>())
                .add(new CodeNameDto(code, name));
        }
        return result;
    }

    private ThumbnailDto fetchThumbnail(long webtoonId) {
        String sql = """
            SELECT source_url, stored_path
            FROM webtoon_images
            WHERE webtoon_id = :webtoonId
              AND image_type = 'THUMBNAIL'
            ORDER BY is_primary DESC, id ASC
            LIMIT 1
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("webtoonId", webtoonId);
        List<ThumbnailDto> thumbnails = jdbc.query(sql, params, (rs, rowNum) -> new ThumbnailDto(
            rs.getString("source_url"),
            rs.getString("stored_path")
        ));
        return thumbnails.isEmpty() ? new ThumbnailDto(null, null) : thumbnails.get(0);
    }

    private RowMapper<BaseWebtoonRow> baseWebtoonRowMapper() {
        return (rs, rowNum) -> new BaseWebtoonRow(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("description"),
            rs.getString("status"),
            rs.getString("original_url"),
            rs.getString("platform_code"),
            rs.getString("platform_name"),
            rs.getString("platform_base_url"),
            rs.getString("thumbnail_url"),
            toLocalDateTime(rs.getTimestamp("last_crawled_at")),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")),
            rs.getObject("is_active") == null || rs.getBoolean("is_active")
        );
    }

    private RowMapper<DetailBaseRow> detailBaseRowMapper() {
        return (rs, rowNum) -> new DetailBaseRow(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("description"),
            rs.getString("status"),
            rs.getString("original_url"),
            rs.getString("platform_code"),
            rs.getString("platform_name"),
            rs.getString("platform_base_url"),
            toLocalDateTime(rs.getTimestamp("last_crawled_at")),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private String normalizeSort(String sort) {
        if (!hasText(sort)) {
            return "latest";
        }
        return sort.toLowerCase();
    }

    private PopularityScope resolvePopularityScope(WebtoonSearchCondition condition) {
        if (hasText(condition.genre())) {
            return new PopularityScope(POPULARITY_GENRE_TYPE, condition.genre());
        }
        if (hasText(condition.weekday())) {
            return new PopularityScope(POPULARITY_WEEKDAY_TYPE, condition.weekday());
        }
        if ("COMPLETED".equalsIgnoreCase(condition.status())) {
            return new PopularityScope(POPULARITY_STATUS_TYPE, "COMPLETED");
        }
        return new PopularityScope(POPULARITY_GLOBAL_TYPE, POPULARITY_GLOBAL_KEY);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String toStatusName(String status) {
        if (status == null) {
            return "알 수 없음";
        }
        return STATUS_NAMES.getOrDefault(status, "알 수 없음");
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record BaseWebtoonRow(
        Long id,
        String title,
        String author,
        String description,
        String status,
        String originalUrl,
        String platformCode,
        String platformName,
        String platformBaseUrl,
        String thumbnailUrl,
        LocalDateTime lastCrawledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean isActive
    ) {
    }

    private record DetailBaseRow(
        Long id,
        String title,
        String author,
        String description,
        String status,
        String originalUrl,
        String platformCode,
        String platformName,
        String platformBaseUrl,
        LocalDateTime lastCrawledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    private record PopularityScope(String rankingType, String rankingKey) {
    }
}
