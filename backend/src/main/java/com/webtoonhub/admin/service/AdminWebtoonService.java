package com.webtoonhub.admin.service;

import com.webtoonhub.admin.dto.AdminWebtoonUpsertRequest;
import com.webtoonhub.common.exception.BadRequestException;
import com.webtoonhub.common.exception.NotFoundException;
import com.webtoonhub.webtoon.dto.PagedResultDto;
import com.webtoonhub.webtoon.dto.WebtoonDetailDto;
import com.webtoonhub.webtoon.dto.WebtoonListItemDto;
import com.webtoonhub.webtoon.repository.WebtoonQueryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminWebtoonService {

    private final NamedParameterJdbcTemplate jdbc;
    private final WebtoonQueryRepository webtoonQueryRepository;

    public AdminWebtoonService(
        NamedParameterJdbcTemplate jdbc,
        WebtoonQueryRepository webtoonQueryRepository
    ) {
        this.jdbc = jdbc;
        this.webtoonQueryRepository = webtoonQueryRepository;
    }

    public PagedResultDto<WebtoonListItemDto> getWebtoons(String keyword, int page, int size) {
        return webtoonQueryRepository.findAdminWebtoons(keyword, page, size);
    }

    public WebtoonDetailDto getWebtoon(long webtoonId) {
        return webtoonQueryRepository.getWebtoonDetailOrThrow(webtoonId, true);
    }

    @Transactional
    public WebtoonDetailDto createWebtoon(AdminWebtoonUpsertRequest request) {
        validateStatus(request.status());

        Long platformId = findPlatformIdByCode(request.platformCode());
        KeyHolder keyHolder = new GeneratedKeyHolder();

        String sql = """
            INSERT INTO webtoons (
              platform_id,
              external_id,
              title,
              author,
              description,
              original_url,
              status,
              is_adult,
              is_active,
              last_crawled_at,
              created_at,
              updated_at
            )
            VALUES (
              :platformId,
              :externalId,
              :title,
              :author,
              :description,
              :originalUrl,
              :status,
              :isAdult,
              :isActive,
              CURRENT_TIMESTAMP,
              CURRENT_TIMESTAMP,
              CURRENT_TIMESTAMP
            )
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("platformId", platformId)
            .addValue("externalId", normalizeBlank(request.externalId()))
            .addValue("title", request.title())
            .addValue("author", normalizeBlank(request.author()))
            .addValue("description", normalizeBlank(request.description()))
            .addValue("originalUrl", request.originalUrl())
            .addValue("status", request.status())
            .addValue("isAdult", request.isAdult())
            .addValue("isActive", request.isActive());

        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("웹툰 생성 ID를 가져오지 못했습니다.");
        }

        long webtoonId = key.longValue();
        replaceGenreMappings(webtoonId, request.genreCodes());
        replaceWeekdayMappings(webtoonId, request.weekdayCodes());
        upsertThumbnail(webtoonId, request.thumbnail() == null ? null : request.thumbnail().sourceUrl());

        return webtoonQueryRepository.getWebtoonDetailOrThrow(webtoonId, true);
    }

    @Transactional
    public WebtoonDetailDto updateWebtoon(long webtoonId, AdminWebtoonUpsertRequest request) {
        validateStatus(request.status());
        ensureWebtoonExists(webtoonId);

        Long platformId = findPlatformIdByCode(request.platformCode());

        String sql = """
            UPDATE webtoons
            SET
              platform_id = :platformId,
              external_id = :externalId,
              title = :title,
              author = :author,
              description = :description,
              original_url = :originalUrl,
              status = :status,
              is_adult = :isAdult,
              is_active = :isActive,
              updated_at = CURRENT_TIMESTAMP
            WHERE id = :webtoonId
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("webtoonId", webtoonId)
            .addValue("platformId", platformId)
            .addValue("externalId", normalizeBlank(request.externalId()))
            .addValue("title", request.title())
            .addValue("author", normalizeBlank(request.author()))
            .addValue("description", normalizeBlank(request.description()))
            .addValue("originalUrl", request.originalUrl())
            .addValue("status", request.status())
            .addValue("isAdult", request.isAdult())
            .addValue("isActive", request.isActive());

        jdbc.update(sql, params);

        replaceGenreMappings(webtoonId, request.genreCodes());
        replaceWeekdayMappings(webtoonId, request.weekdayCodes());
        upsertThumbnail(webtoonId, request.thumbnail() == null ? null : request.thumbnail().sourceUrl());

        return webtoonQueryRepository.getWebtoonDetailOrThrow(webtoonId, true);
    }

    @Transactional
    public WebtoonDetailDto setActive(long webtoonId, boolean active) {
        ensureWebtoonExists(webtoonId);

        String sql = """
            UPDATE webtoons
            SET is_active = :active,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :webtoonId
            """;

        jdbc.update(sql, new MapSqlParameterSource()
            .addValue("webtoonId", webtoonId)
            .addValue("active", active));

        return webtoonQueryRepository.getWebtoonDetailOrThrow(webtoonId, true);
    }

    @Transactional
    public WebtoonDetailDto refreshThumbnail(long webtoonId) {
        ensureWebtoonExists(webtoonId);
        String sql = """
            UPDATE webtoon_images
            SET updated_at = CURRENT_TIMESTAMP
            WHERE webtoon_id = :webtoonId
              AND image_type = 'THUMBNAIL'
            """;
        jdbc.update(sql, new MapSqlParameterSource("webtoonId", webtoonId));
        return webtoonQueryRepository.getWebtoonDetailOrThrow(webtoonId, true);
    }

    private void ensureWebtoonExists(long webtoonId) {
        String sql = "SELECT COUNT(*) FROM webtoons WHERE id = :webtoonId";
        Long count = jdbc.queryForObject(sql, new MapSqlParameterSource("webtoonId", webtoonId), Long.class);
        if (count == null || count == 0L) {
            throw new NotFoundException("웹툰을 찾을 수 없습니다.");
        }
    }

    private Long findPlatformIdByCode(String code) {
        String sql = "SELECT id FROM platforms WHERE code = :code";
        List<Long> ids = jdbc.query(sql, new MapSqlParameterSource("code", code), (rs, rowNum) -> rs.getLong("id"));
        if (ids.isEmpty()) {
            throw new BadRequestException("유효하지 않은 platformCode입니다.");
        }
        return ids.get(0);
    }

    private void replaceGenreMappings(long webtoonId, List<String> codes) {
        List<Long> genreIds = resolveIdsByCodes("genres", codes, "장르 코드가 유효하지 않습니다.");

        jdbc.update(
            "DELETE FROM webtoon_genres WHERE webtoon_id = :webtoonId",
            new MapSqlParameterSource("webtoonId", webtoonId)
        );

        String insertSql = """
            INSERT INTO webtoon_genres (webtoon_id, genre_id, created_at)
            VALUES (:webtoonId, :genreId, CURRENT_TIMESTAMP)
            """;

        List<MapSqlParameterSource> batchParams = new ArrayList<>();
        for (Long genreId : genreIds) {
            batchParams.add(new MapSqlParameterSource()
                .addValue("webtoonId", webtoonId)
                .addValue("genreId", genreId));
        }
        jdbc.batchUpdate(insertSql, batchParams.toArray(MapSqlParameterSource[]::new));
    }

    private void replaceWeekdayMappings(long webtoonId, List<String> codes) {
        List<Long> weekdayIds = resolveIdsByCodes("weekdays", codes, "요일 코드가 유효하지 않습니다.");

        jdbc.update(
            "DELETE FROM webtoon_weekdays WHERE webtoon_id = :webtoonId",
            new MapSqlParameterSource("webtoonId", webtoonId)
        );

        String insertSql = """
            INSERT INTO webtoon_weekdays (webtoon_id, weekday_id, created_at)
            VALUES (:webtoonId, :weekdayId, CURRENT_TIMESTAMP)
            """;

        List<MapSqlParameterSource> batchParams = new ArrayList<>();
        for (Long weekdayId : weekdayIds) {
            batchParams.add(new MapSqlParameterSource()
                .addValue("webtoonId", webtoonId)
                .addValue("weekdayId", weekdayId));
        }
        jdbc.batchUpdate(insertSql, batchParams.toArray(MapSqlParameterSource[]::new));
    }

    private void upsertThumbnail(long webtoonId, String sourceUrl) {
        String normalized = normalizeBlank(sourceUrl);
        if (normalized == null) {
            return;
        }

        String countSql = """
            SELECT COUNT(*)
            FROM webtoon_images
            WHERE webtoon_id = :webtoonId
              AND image_type = 'THUMBNAIL'
              AND is_primary = TRUE
            """;

        Long count = jdbc.queryForObject(countSql, new MapSqlParameterSource("webtoonId", webtoonId), Long.class);
        if (count == null || count == 0) {
            String insertSql = """
                INSERT INTO webtoon_images (
                  webtoon_id,
                  image_type,
                  source_url,
                  stored_path,
                  is_primary,
                  created_at,
                  updated_at
                )
                VALUES (
                  :webtoonId,
                  'THUMBNAIL',
                  :sourceUrl,
                  NULL,
                  TRUE,
                  CURRENT_TIMESTAMP,
                  CURRENT_TIMESTAMP
                )
                """;
            jdbc.update(insertSql, new MapSqlParameterSource()
                .addValue("webtoonId", webtoonId)
                .addValue("sourceUrl", normalized));
        } else {
            String updateSql = """
                UPDATE webtoon_images
                SET source_url = :sourceUrl,
                    updated_at = CURRENT_TIMESTAMP
                WHERE webtoon_id = :webtoonId
                  AND image_type = 'THUMBNAIL'
                  AND is_primary = TRUE
                """;
            jdbc.update(updateSql, new MapSqlParameterSource()
                .addValue("webtoonId", webtoonId)
                .addValue("sourceUrl", normalized));
        }
    }

    private List<Long> resolveIdsByCodes(String tableName, List<String> codes, String errorMessage) {
        List<String> normalizedCodes = codes == null
            ? List.of()
            : codes.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();

        if (normalizedCodes.isEmpty()) {
            throw new BadRequestException(errorMessage);
        }

        String sql = "SELECT code, id FROM " + tableName + " WHERE code IN (:codes)";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, new MapSqlParameterSource("codes", normalizedCodes));

        Map<String, Long> idByCode = new HashMap<>();
        for (Map<String, Object> row : rows) {
            idByCode.put((String) row.get("code"), ((Number) row.get("id")).longValue());
        }

        List<String> missing = normalizedCodes.stream()
            .filter(code -> !idByCode.containsKey(code))
            .toList();
        if (!missing.isEmpty()) {
            throw new BadRequestException(errorMessage + " (" + String.join(", ", missing) + ")");
        }

        return normalizedCodes.stream().map(idByCode::get).toList();
    }

    private void validateStatus(String status) {
        List<String> allowed = List.of("ONGOING", "COMPLETED", "HIATUS", "UNKNOWN");
        if (!allowed.contains(status)) {
            throw new BadRequestException("status 값은 ONGOING, COMPLETED, HIATUS, UNKNOWN 중 하나여야 합니다.");
        }
    }

    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
