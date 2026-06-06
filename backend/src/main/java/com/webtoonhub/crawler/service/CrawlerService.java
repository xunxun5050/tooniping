package com.webtoonhub.crawler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.webtoonhub.common.exception.NotFoundException;
import com.webtoonhub.crawler.dto.CrawlHistoryDto;
import com.webtoonhub.crawler.dto.CrawlRunResponseDto;
import com.webtoonhub.webtoon.dto.PagedResultDto;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CrawlerService {

    private static final String NAVER_PLATFORM_CODE = "NAVER_WEBTOON";
    private static final String KAKAO_PLATFORM_CODE = "KAKAO_WEBTOON";
    private static final String NAVER_WEEKDAY_API_URL = "https://comic.naver.com/api/webtoon/titlelist/weekday?order=user";
    private static final String NAVER_GENRE_API_URL_TEMPLATE = "https://comic.naver.com/api/webtoon/titlelist/genre?genre=%s&order=user&page=%d";
    private static final String KAKAO_TIMETABLE_API_URL_TEMPLATE = "https://gateway-kw.kakao.com/section/v2/timetables/days?placement=%s";
    private static final String NAVER_BASE_URL = "https://comic.naver.com";
    private static final String KAKAO_BASE_URL = "https://webtoon.kakao.com";
    private static final String POPULARITY_GLOBAL_TYPE = "GLOBAL";
    private static final String POPULARITY_GLOBAL_KEY = "ALL";
    private static final String POPULARITY_GENRE_TYPE = "GENRE";
    private static final String POPULARITY_WEEKDAY_TYPE = "WEEKDAY";
    private static final String POPULARITY_STATUS_TYPE = "STATUS";
    private static final String POPULARITY_SECTION_TYPE = "SECTION";

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private static final List<String> NAVER_WEEKDAY_CODES = List.of(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    );

    private static final List<GenreFetchPlan> NAVER_GENRE_FETCH_PLANS = List.of(
        new GenreFetchPlan("PURE", "ROMANCE"),
        new GenreFetchPlan("ACTION", "ACTION"),
        new GenreFetchPlan("FANTASY", "FANTASY"),
        new GenreFetchPlan("DRAMA", "DRAMA"),
        new GenreFetchPlan("SPORTS", "SPORTS"),
        new GenreFetchPlan("DAILY", "DAILY"),
        new GenreFetchPlan("COMIC", "COMEDY"),
        new GenreFetchPlan("THRILL", "THRILLER"),
        new GenreFetchPlan("HISTORICAL", "HISTORICAL"),
        new GenreFetchPlan("SENSIBILITY", "SENSIBILITY")
    );

    private static final List<KakaoTimetableFetchPlan> KAKAO_TIMETABLE_FETCH_PLANS = List.of(
        new KakaoTimetableFetchPlan("timetable_mon", "MONDAY", false),
        new KakaoTimetableFetchPlan("timetable_tue", "TUESDAY", false),
        new KakaoTimetableFetchPlan("timetable_wed", "WEDNESDAY", false),
        new KakaoTimetableFetchPlan("timetable_thu", "THURSDAY", false),
        new KakaoTimetableFetchPlan("timetable_fri", "FRIDAY", false),
        new KakaoTimetableFetchPlan("timetable_sat", "SATURDAY", false),
        new KakaoTimetableFetchPlan("timetable_sun", "SUNDAY", false),
        new KakaoTimetableFetchPlan("timetable_new", null, false),
        new KakaoTimetableFetchPlan("timetable_completed", null, true)
    );

    private static final Map<String, List<String>> KAKAO_GENRE_CODE_MAP = Map.of(
        "ACTION_WUXIA", List.of("ACTION", "HISTORICAL"),
        "COMIC_EVERYDAY_LIFE", List.of("COMEDY", "DAILY"),
        "DRAMA", List.of("DRAMA"),
        "FANTASY_DRAMA", List.of("FANTASY", "DRAMA"),
        "HORROR_THRILLER", List.of("THRILLER"),
        "ROMANCE", List.of("ROMANCE"),
        "ROMANCE_FANTASY", List.of("ROMANCE", "FANTASY"),
        "SCHOOL_ACTION_FANTASY", List.of("ACTION", "FANTASY")
    );

    private static final List<PlatformReference> PLATFORM_REFERENCES = List.of(
        new PlatformReference(NAVER_PLATFORM_CODE, "네이버 웹툰", NAVER_BASE_URL),
        new PlatformReference(KAKAO_PLATFORM_CODE, "카카오 웹툰", KAKAO_BASE_URL)
    );

    private static final List<GenreReference> GENRE_REFERENCES = List.of(
        new GenreReference("FANTASY", "판타지", 1),
        new GenreReference("ROMANCE", "로맨스", 2),
        new GenreReference("ACTION", "액션", 3),
        new GenreReference("DAILY", "일상", 4),
        new GenreReference("COMEDY", "개그", 5),
        new GenreReference("THRILLER", "스릴러", 6),
        new GenreReference("DRAMA", "드라마", 7),
        new GenreReference("SPORTS", "스포츠", 8),
        new GenreReference("HISTORICAL", "시대극", 9),
        new GenreReference("SENSIBILITY", "감성", 10)
    );

    private static final List<WeekdayReference> WEEKDAY_REFERENCES = List.of(
        new WeekdayReference("MONDAY", "월요일", 1),
        new WeekdayReference("TUESDAY", "화요일", 2),
        new WeekdayReference("WEDNESDAY", "수요일", 3),
        new WeekdayReference("THURSDAY", "목요일", 4),
        new WeekdayReference("FRIDAY", "금요일", 5),
        new WeekdayReference("SATURDAY", "토요일", 6),
        new WeekdayReference("SUNDAY", "일요일", 7),
        new WeekdayReference("DAILY_PLUS", "매일+", 8),
        new WeekdayReference("COMPLETED", "완결", 9)
    );

    private final NamedParameterJdbcTemplate jdbc;
    private final RestClient restClient;

    public CrawlerService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.restClient = RestClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public CrawlRunResponseDto runInitialCrawler() {
        return runCrawler("INITIAL", CrawlerTarget.NAVER);
    }

    public CrawlRunResponseDto runWeeklyCrawler() {
        return runCrawler("WEEKLY_UPDATE", CrawlerTarget.NAVER);
    }

    public CrawlRunResponseDto runKakaoInitialCrawler() {
        return runCrawler("INITIAL", CrawlerTarget.KAKAO);
    }

    public CrawlRunResponseDto runKakaoWeeklyCrawler() {
        return runCrawler("WEEKLY_UPDATE", CrawlerTarget.KAKAO);
    }

    public PagedResultDto<CrawlHistoryDto> getCrawlHistories(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);

        String countSql = "SELECT COUNT(*) FROM crawl_histories";
        Long totalElements = jdbc.queryForObject(countSql, new MapSqlParameterSource(), Long.class);
        long total = totalElements == null ? 0L : totalElements;

        String sql = """
            SELECT
              ch.id,
              p.code AS platform_code,
              ch.crawl_type,
              ch.status,
              ch.total_count,
              ch.success_count,
              ch.fail_count,
              ch.started_at,
              ch.ended_at,
              ch.message,
              ch.created_at
            FROM crawl_histories ch
            JOIN platforms p ON p.id = ch.platform_id
            ORDER BY ch.id DESC
            LIMIT :size OFFSET :offset
            """;

        List<CrawlHistoryDto> content = jdbc.query(sql, new MapSqlParameterSource()
            .addValue("size", safeSize)
            .addValue("offset", safePage * safeSize), (rs, rowNum) -> new CrawlHistoryDto(
            rs.getLong("id"),
            rs.getString("platform_code"),
            rs.getString("crawl_type"),
            rs.getString("status"),
            rs.getInt("total_count"),
            rs.getInt("success_count"),
            rs.getInt("fail_count"),
            toLocalDateTime(rs.getTimestamp("started_at")),
            toLocalDateTime(rs.getTimestamp("ended_at")),
            rs.getString("message"),
            toLocalDateTime(rs.getTimestamp("created_at"))
        ));

        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        boolean hasNext = safePage + 1 < totalPages;
        return new PagedResultDto<>(content, safePage, safeSize, total, totalPages, hasNext);
    }

    public CrawlHistoryDto getCrawlHistory(long crawlHistoryId) {
        String sql = """
            SELECT
              ch.id,
              p.code AS platform_code,
              ch.crawl_type,
              ch.status,
              ch.total_count,
              ch.success_count,
              ch.fail_count,
              ch.started_at,
              ch.ended_at,
              ch.message,
              ch.created_at
            FROM crawl_histories ch
            JOIN platforms p ON p.id = ch.platform_id
            WHERE ch.id = :crawlHistoryId
            """;

        List<CrawlHistoryDto> rows = jdbc.query(sql, new MapSqlParameterSource("crawlHistoryId", crawlHistoryId),
            (rs, rowNum) -> new CrawlHistoryDto(
                rs.getLong("id"),
                rs.getString("platform_code"),
                rs.getString("crawl_type"),
                rs.getString("status"),
                rs.getInt("total_count"),
                rs.getInt("success_count"),
                rs.getInt("fail_count"),
                toLocalDateTime(rs.getTimestamp("started_at")),
                toLocalDateTime(rs.getTimestamp("ended_at")),
                rs.getString("message"),
                toLocalDateTime(rs.getTimestamp("created_at"))
            ));

        if (rows.isEmpty()) {
            throw new NotFoundException("크롤링 이력을 찾을 수 없습니다.");
        }
        return rows.get(0);
    }

    private CrawlRunResponseDto runCrawler(String crawlType, CrawlerTarget target) {
        ensureReferenceData();

        Long platformId = getPlatformId(target.platformCode());
        long crawlHistoryId = createCrawlHistory(platformId, crawlType);
        LocalDateTime startedAt = LocalDateTime.now();

        try {
            CrawlCollectionResult collectionResult = switch (target) {
                case NAVER -> collectNaverWebtoons();
                case KAKAO -> collectKakaoWebtoons();
            };
            PersistResult persistResult = persistWebtoons(
                platformId,
                crawlHistoryId,
                collectionResult.webtoonsByExternalId().values(),
                collectionResult.collectionFailures()
            );

            String finalStatus = persistResult.failCount() > 0 ? STATUS_PARTIAL_SUCCESS : STATUS_SUCCESS;
            String message = String.format(
                "%s 적재 완료: 총 %d건, 성공 %d건(신규 %d/수정 %d), 실패 %d건",
                target.displayName(),
                persistResult.totalCount(),
                persistResult.successCount(),
                persistResult.insertedCount(),
                persistResult.updatedCount(),
                persistResult.failCount()
            );

            updateCrawlHistory(
                crawlHistoryId,
                finalStatus,
                persistResult.totalCount(),
                persistResult.successCount(),
                persistResult.failCount(),
                message
            );

            return new CrawlRunResponseDto(
                crawlHistoryId,
                crawlType,
                finalStatus,
                startedAt,
                LocalDateTime.now(),
                message
            );
        } catch (Exception exception) {
            String errorMessage = trimErrorMessage(exception);
            String message = target.displayName() + " 크롤링 실패: " + errorMessage;

            updateCrawlHistory(crawlHistoryId, STATUS_FAILED, 0, 0, 0, message);
            throw new IllegalStateException(message, exception);
        }
    }

    private CrawlCollectionResult collectNaverWebtoons() {
        Map<String, CrawlWebtoon> webtoonsByExternalId = new LinkedHashMap<>();
        List<CrawlFailureInput> collectionFailures = new ArrayList<>();

        collectFromGenreApi(webtoonsByExternalId, collectionFailures);
        collectWeekdayMappings(webtoonsByExternalId, collectionFailures);
        assignGlobalPopularityRankings(webtoonsByExternalId.values());
        applyDerivedWeekdayMappings(webtoonsByExternalId.values());

        return new CrawlCollectionResult(webtoonsByExternalId, collectionFailures);
    }

    private void collectFromGenreApi(
        Map<String, CrawlWebtoon> webtoonsByExternalId,
        List<CrawlFailureInput> failures
    ) {
        for (GenreFetchPlan genrePlan : NAVER_GENRE_FETCH_PLANS) {
            String firstPageUrl = NAVER_GENRE_API_URL_TEMPLATE.formatted(genrePlan.apiGenreCode(), 1);
            JsonNode firstPage;
            try {
                firstPage = fetchJson(firstPageUrl);
            } catch (Exception exception) {
                failures.add(new CrawlFailureInput(
                    firstPageUrl,
                    null,
                    null,
                    exception.getClass().getSimpleName(),
                    trimErrorMessage(exception)
                ));
                continue;
            }

            int totalPages = Math.max(firstPage.path("pageInfo").path("totalPages").asInt(1), 1);
            int nextRankPosition = 1;
            nextRankPosition += mergeTitleList(
                firstPage,
                firstPageUrl,
                genrePlan.dbGenreCode(),
                null,
                POPULARITY_GENRE_TYPE,
                genrePlan.dbGenreCode(),
                nextRankPosition,
                webtoonsByExternalId,
                failures
            );

            for (int page = 2; page <= totalPages; page++) {
                String pageUrl = NAVER_GENRE_API_URL_TEMPLATE.formatted(genrePlan.apiGenreCode(), page);
                try {
                    JsonNode pageResponse = fetchJson(pageUrl);
                    nextRankPosition += mergeTitleList(
                        pageResponse,
                        pageUrl,
                        genrePlan.dbGenreCode(),
                        null,
                        POPULARITY_GENRE_TYPE,
                        genrePlan.dbGenreCode(),
                        nextRankPosition,
                        webtoonsByExternalId,
                        failures
                    );
                } catch (Exception exception) {
                    failures.add(new CrawlFailureInput(
                        pageUrl,
                        null,
                        null,
                        exception.getClass().getSimpleName(),
                        trimErrorMessage(exception)
                    ));
                }
            }
        }
    }

    private void collectWeekdayMappings(
        Map<String, CrawlWebtoon> webtoonsByExternalId,
        List<CrawlFailureInput> failures
    ) {
        JsonNode response = fetchJson(NAVER_WEEKDAY_API_URL);
        JsonNode titleListMap = response.path("titleListMap");
        if (titleListMap.isMissingNode() || !titleListMap.isObject()) {
            throw new IllegalStateException("weekday API 응답 형식이 올바르지 않습니다.");
        }

        for (String weekdayCode : NAVER_WEEKDAY_CODES) {
            JsonNode list = titleListMap.path(weekdayCode);
            if (!list.isArray()) {
                continue;
            }

            int rankPosition = 1;
            for (JsonNode node : list) {
                String externalId = getExternalId(node);
                String title = trimToNull(node.path("titleName").asText(null));

                if (externalId == null || title == null) {
                    failures.add(new CrawlFailureInput(
                        NAVER_WEEKDAY_API_URL,
                        externalId,
                        title,
                        "INVALID_PAYLOAD",
                        "weekday 작품의 titleId/titleName이 비어 있습니다."
                    ));
                    continue;
                }

                CrawlWebtoon webtoon = webtoonsByExternalId.computeIfAbsent(externalId, CrawlWebtoon::new);
                webtoon.mergeNaverBaseInfo(node, buildNaverWebtoonUrl(externalId));
                webtoon.weekdayCodes().add(weekdayCode);
                webtoon.addPopularityRanking(POPULARITY_WEEKDAY_TYPE, weekdayCode, rankPosition);
                rankPosition++;
            }
        }
    }

    private int mergeTitleList(
        JsonNode response,
        String targetUrl,
        String genreCode,
        String weekdayCode,
        String rankingType,
        String rankingKey,
        int rankStart,
        Map<String, CrawlWebtoon> webtoonsByExternalId,
        List<CrawlFailureInput> failures
    ) {
        JsonNode titleList = response.path("titleList");
        if (!titleList.isArray()) {
            failures.add(new CrawlFailureInput(
                targetUrl,
                null,
                null,
                "INVALID_PAYLOAD",
                "titleList 배열을 찾지 못했습니다."
            ));
            return 0;
        }

        int processedCount = 0;
        for (JsonNode node : titleList) {
            int rankPosition = rankStart + processedCount;
            processedCount++;
            String externalId = getExternalId(node);
            String title = trimToNull(node.path("titleName").asText(null));

            if (externalId == null || title == null) {
                failures.add(new CrawlFailureInput(
                    targetUrl,
                    externalId,
                    title,
                    "INVALID_PAYLOAD",
                    "titleId/titleName이 비어 있어 스킵했습니다."
                ));
                continue;
            }

            CrawlWebtoon webtoon = webtoonsByExternalId.computeIfAbsent(externalId, CrawlWebtoon::new);
            webtoon.mergeNaverBaseInfo(node, buildNaverWebtoonUrl(externalId));
            if (genreCode != null) {
                webtoon.genreCodes().add(genreCode);
            }
            if (weekdayCode != null) {
                webtoon.weekdayCodes().add(weekdayCode);
            }
            if (rankingType != null && rankingKey != null) {
                webtoon.addPopularityRanking(rankingType, rankingKey, rankPosition);
            }
        }
        return processedCount;
    }

    private CrawlCollectionResult collectKakaoWebtoons() {
        Map<String, CrawlWebtoon> webtoonsByExternalId = new LinkedHashMap<>();
        List<CrawlFailureInput> collectionFailures = new ArrayList<>();

        for (KakaoTimetableFetchPlan plan : KAKAO_TIMETABLE_FETCH_PLANS) {
            String targetUrl = KAKAO_TIMETABLE_API_URL_TEMPLATE.formatted(plan.placement());
            try {
                JsonNode response = fetchJson(targetUrl);
                mergeKakaoTimetable(response, targetUrl, plan, webtoonsByExternalId, collectionFailures);
            } catch (Exception exception) {
                collectionFailures.add(new CrawlFailureInput(
                    targetUrl,
                    null,
                    null,
                    exception.getClass().getSimpleName(),
                    trimErrorMessage(exception)
                ));
            }
        }

        assignGlobalPopularityRankings(webtoonsByExternalId.values());
        applyDerivedWeekdayMappings(webtoonsByExternalId.values());
        return new CrawlCollectionResult(webtoonsByExternalId, collectionFailures);
    }

    private void mergeKakaoTimetable(
        JsonNode response,
        String targetUrl,
        KakaoTimetableFetchPlan plan,
        Map<String, CrawlWebtoon> webtoonsByExternalId,
        List<CrawlFailureInput> failures
    ) {
        JsonNode sections = response.path("data");
        if (!sections.isArray()) {
            failures.add(new CrawlFailureInput(
                targetUrl,
                null,
                null,
                "INVALID_PAYLOAD",
                "data 배열을 찾지 못했습니다."
            ));
            return;
        }

        int rankPosition = 1;
        for (JsonNode section : sections) {
            JsonNode cardGroups = section.path("cardGroups");
            if (!cardGroups.isArray()) {
                continue;
            }

            for (JsonNode cardGroup : cardGroups) {
                JsonNode cards = cardGroup.path("cards");
                if (!cards.isArray()) {
                    continue;
                }

                for (JsonNode card : cards) {
                    mergeKakaoCard(card, targetUrl, plan, rankPosition, webtoonsByExternalId, failures);
                    rankPosition++;
                }
            }
        }
    }

    private void mergeKakaoCard(
        JsonNode card,
        String targetUrl,
        KakaoTimetableFetchPlan plan,
        int rankPosition,
        Map<String, CrawlWebtoon> webtoonsByExternalId,
        List<CrawlFailureInput> failures
    ) {
        JsonNode content = card.path("content");
        String externalId = trimToNull(content.path("id").asText(null));
        String title = trimToNull(content.path("title").asText(null));

        if (externalId == null || title == null) {
            failures.add(new CrawlFailureInput(
                targetUrl,
                externalId,
                title,
                "INVALID_PAYLOAD",
                "카카오 timetable 작품의 id/title이 비어 있어 스킵했습니다."
            ));
            return;
        }

        CrawlWebtoon webtoon = webtoonsByExternalId.computeIfAbsent(externalId, CrawlWebtoon::new);
        webtoon.mergeKakaoBaseInfo(
            title,
            joinKakaoAuthors(content.path("authors")),
            trimToNull(content.path("catchphraseTwoLines").asText(null)),
            normalizeKakaoImageUrl(firstNonBlank(
                content.path("featuredCharacterImageA").asText(null),
                content.path("featuredCharacterImageB").asText(null),
                content.path("backgroundImage").asText(null),
                content.path("titleImageA").asText(null)
            )),
            buildKakaoWebtoonUrl(externalId, content.path("seoId").asText(null), title),
            content.path("adult").asBoolean(false) || card.path("additional").path("adult").asBoolean(false)
        );

        if (plan.completed()) {
            webtoon.markCompleted();
            webtoon.addPopularityRanking(POPULARITY_WEEKDAY_TYPE, "COMPLETED", rankPosition);
            webtoon.addPopularityRanking(POPULARITY_STATUS_TYPE, "COMPLETED", rankPosition);
        } else if (plan.weekdayCode() != null) {
            webtoon.weekdayCodes().add(plan.weekdayCode());
            webtoon.addPopularityRanking(POPULARITY_WEEKDAY_TYPE, plan.weekdayCode(), rankPosition);
        } else {
            webtoon.addPopularityRanking(POPULARITY_SECTION_TYPE, kakaoSectionKey(plan.placement()), rankPosition);
        }

        JsonNode genreFilters = card.path("genreFilters");
        if (genreFilters.isArray()) {
            for (JsonNode genreFilter : genreFilters) {
                for (String dbGenreCode : mapKakaoGenreCodes(genreFilter.asText(null))) {
                    webtoon.genreCodes().add(dbGenreCode);
                }
            }
        }
    }

    private void applyDerivedWeekdayMappings(Collection<CrawlWebtoon> webtoons) {
        for (CrawlWebtoon webtoon : webtoons) {
            if (webtoon.isCompleted()) {
                webtoon.weekdayCodes().clear();
                webtoon.weekdayCodes().add("COMPLETED");
                continue;
            }

            if (webtoon.weekdayCodes().isEmpty()) {
                webtoon.weekdayCodes().add("DAILY_PLUS");
            }
        }
    }

    private void assignGlobalPopularityRankings(Collection<CrawlWebtoon> webtoons) {
        int rankPosition = 1;
        for (CrawlWebtoon webtoon : webtoons) {
            webtoon.addPopularityRanking(POPULARITY_GLOBAL_TYPE, POPULARITY_GLOBAL_KEY, rankPosition);
            rankPosition++;
        }
    }

    private PersistResult persistWebtoons(
        Long platformId,
        long crawlHistoryId,
        Collection<CrawlWebtoon> webtoons,
        List<CrawlFailureInput> collectionFailures
    ) {
        int totalCount = webtoons.size();
        int insertedCount = 0;
        int updatedCount = 0;
        int successCount = 0;
        int failCount = 0;

        Map<String, Long> genreIdByCode = loadCodeIdMap("genres");
        Map<String, Long> weekdayIdByCode = loadCodeIdMap("weekdays");

        for (CrawlFailureInput failure : collectionFailures) {
            saveCrawlFailure(crawlHistoryId, failure);
            failCount++;
        }

        Set<String> crawledExternalIds = new LinkedHashSet<>();
        for (CrawlWebtoon webtoon : webtoons) {
            crawledExternalIds.add(webtoon.externalId());

            try {
                Long webtoonId = findWebtoonIdByExternalId(platformId, webtoon.externalId());
                if (webtoonId == null) {
                    webtoonId = insertWebtoon(platformId, webtoon);
                    insertedCount++;
                } else {
                    updateWebtoon(webtoonId, webtoon);
                    updatedCount++;
                }

                List<Long> genreIds = webtoon.genreCodes().stream()
                    .map(genreIdByCode::get)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

                List<Long> weekdayIds = webtoon.weekdayCodes().stream()
                    .map(weekdayIdByCode::get)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

                replaceGenreMappings(webtoonId, genreIds);
                replaceWeekdayMappings(webtoonId, weekdayIds);
                replacePopularityRankings(webtoonId, webtoon.popularityRankings());
                upsertThumbnail(webtoonId, webtoon.thumbnailUrl());

                successCount++;
            } catch (Exception exception) {
                failCount++;
                saveCrawlFailure(crawlHistoryId, new CrawlFailureInput(
                    webtoon.originalUrl(),
                    webtoon.externalId(),
                    webtoon.title(),
                    exception.getClass().getSimpleName(),
                    trimErrorMessage(exception)
                ));
            }
        }

        if (collectionFailures.isEmpty()) {
            deactivateMissingWebtoons(platformId, crawledExternalIds);
        }
        return new PersistResult(totalCount, insertedCount, updatedCount, successCount, failCount);
    }

    private void deactivateMissingWebtoons(Long platformId, Set<String> crawledExternalIds) {
        if (crawledExternalIds.isEmpty()) {
            return;
        }

        String sql = """
            UPDATE webtoons
            SET is_active = FALSE,
                updated_at = CURRENT_TIMESTAMP
            WHERE platform_id = :platformId
              AND is_active = TRUE
              AND external_id IS NOT NULL
              AND external_id NOT IN (:externalIds)
            """;

        jdbc.update(sql, new MapSqlParameterSource()
            .addValue("platformId", platformId)
            .addValue("externalIds", crawledExternalIds));
    }

    private Long insertWebtoon(Long platformId, CrawlWebtoon webtoon) {
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
              TRUE,
              CURRENT_TIMESTAMP,
              CURRENT_TIMESTAMP,
              CURRENT_TIMESTAMP
            )
            """;

        jdbc.update(sql, new MapSqlParameterSource()
            .addValue("platformId", platformId)
            .addValue("externalId", webtoon.externalId())
            .addValue("title", webtoon.title())
            .addValue("author", webtoon.author())
            .addValue("description", webtoon.description())
            .addValue("originalUrl", webtoon.originalUrl())
            .addValue("status", webtoon.status())
            .addValue("isAdult", webtoon.adult()), keyHolder, new String[]{"id"});

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("webtoon insert key를 가져오지 못했습니다.");
        }
        return key.longValue();
    }

    private void updateWebtoon(long webtoonId, CrawlWebtoon webtoon) {
        String sql = """
            UPDATE webtoons
            SET
              title = :title,
              author = COALESCE(:author, author),
              description = COALESCE(:description, description),
              original_url = :originalUrl,
              status = :status,
              is_adult = :isAdult,
              last_crawled_at = CURRENT_TIMESTAMP,
              updated_at = CURRENT_TIMESTAMP
            WHERE id = :webtoonId
            """;

        jdbc.update(sql, new MapSqlParameterSource()
            .addValue("webtoonId", webtoonId)
            .addValue("title", webtoon.title())
            .addValue("author", webtoon.author())
            .addValue("description", webtoon.description())
            .addValue("originalUrl", webtoon.originalUrl())
            .addValue("status", webtoon.status())
            .addValue("isAdult", webtoon.adult()));
    }

    private void replaceGenreMappings(long webtoonId, List<Long> genreIds) {
        jdbc.update("DELETE FROM webtoon_genres WHERE webtoon_id = :webtoonId",
            new MapSqlParameterSource("webtoonId", webtoonId));

        if (genreIds.isEmpty()) {
            return;
        }

        String insertSql = """
            INSERT INTO webtoon_genres (webtoon_id, genre_id, created_at)
            VALUES (:webtoonId, :genreId, CURRENT_TIMESTAMP)
            """;

        List<MapSqlParameterSource> params = new ArrayList<>();
        for (Long genreId : genreIds) {
            params.add(new MapSqlParameterSource()
                .addValue("webtoonId", webtoonId)
                .addValue("genreId", genreId));
        }
        jdbc.batchUpdate(insertSql, params.toArray(MapSqlParameterSource[]::new));
    }

    private void replaceWeekdayMappings(long webtoonId, List<Long> weekdayIds) {
        jdbc.update("DELETE FROM webtoon_weekdays WHERE webtoon_id = :webtoonId",
            new MapSqlParameterSource("webtoonId", webtoonId));

        if (weekdayIds.isEmpty()) {
            return;
        }

        String insertSql = """
            INSERT INTO webtoon_weekdays (webtoon_id, weekday_id, created_at)
            VALUES (:webtoonId, :weekdayId, CURRENT_TIMESTAMP)
            """;

        List<MapSqlParameterSource> params = new ArrayList<>();
        for (Long weekdayId : weekdayIds) {
            params.add(new MapSqlParameterSource()
                .addValue("webtoonId", webtoonId)
                .addValue("weekdayId", weekdayId));
        }
        jdbc.batchUpdate(insertSql, params.toArray(MapSqlParameterSource[]::new));
    }

    private void replacePopularityRankings(long webtoonId, Collection<PopularityRanking> rankings) {
        jdbc.update("DELETE FROM webtoon_popularity_rankings WHERE webtoon_id = :webtoonId",
            new MapSqlParameterSource("webtoonId", webtoonId));

        if (rankings.isEmpty()) {
            return;
        }

        String insertSql = """
            INSERT INTO webtoon_popularity_rankings (
              webtoon_id,
              ranking_type,
              ranking_key,
              rank_position,
              collected_at,
              created_at,
              updated_at
            )
            VALUES (
              :webtoonId,
              :rankingType,
              :rankingKey,
              :rankPosition,
              CURRENT_TIMESTAMP,
              CURRENT_TIMESTAMP,
              CURRENT_TIMESTAMP
            )
            """;

        List<MapSqlParameterSource> params = new ArrayList<>();
        for (PopularityRanking ranking : rankings) {
            params.add(new MapSqlParameterSource()
                .addValue("webtoonId", webtoonId)
                .addValue("rankingType", ranking.rankingType())
                .addValue("rankingKey", ranking.rankingKey())
                .addValue("rankPosition", ranking.rankPosition()));
        }
        jdbc.batchUpdate(insertSql, params.toArray(MapSqlParameterSource[]::new));
    }

    private void upsertThumbnail(long webtoonId, String sourceUrl) {
        String normalized = trimToNull(sourceUrl);
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
        if (count == null || count == 0L) {
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

    private Long findWebtoonIdByExternalId(Long platformId, String externalId) {
        String sql = """
            SELECT id
            FROM webtoons
            WHERE platform_id = :platformId
              AND external_id = :externalId
            """;

        List<Long> ids = jdbc.query(sql, new MapSqlParameterSource()
            .addValue("platformId", platformId)
            .addValue("externalId", externalId), (rs, rowNum) -> rs.getLong("id"));

        return ids.isEmpty() ? null : ids.get(0);
    }

    private long createCrawlHistory(Long platformId, String crawlType) {
        String sql = """
            INSERT INTO crawl_histories (
              platform_id,
              crawl_type,
              status,
              total_count,
              success_count,
              fail_count,
              started_at,
              ended_at,
              message,
              created_at
            )
            VALUES (
              :platformId,
              :crawlType,
              :status,
              0,
              0,
              0,
              CURRENT_TIMESTAMP,
              NULL,
              :message,
              CURRENT_TIMESTAMP
            )
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, new MapSqlParameterSource()
            .addValue("platformId", platformId)
            .addValue("crawlType", crawlType)
            .addValue("status", STATUS_RUNNING)
            .addValue("message", "크롤링을 시작했습니다."), keyHolder, new String[]{"id"});

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("crawl_history key를 생성하지 못했습니다.");
        }
        return key.longValue();
    }

    private void updateCrawlHistory(
        long crawlHistoryId,
        String status,
        int totalCount,
        int successCount,
        int failCount,
        String message
    ) {
        String sql = """
            UPDATE crawl_histories
            SET
              status = :status,
              total_count = :totalCount,
              success_count = :successCount,
              fail_count = :failCount,
              ended_at = CURRENT_TIMESTAMP,
              message = :message
            WHERE id = :crawlHistoryId
            """;

        jdbc.update(sql, new MapSqlParameterSource()
            .addValue("crawlHistoryId", crawlHistoryId)
            .addValue("status", status)
            .addValue("totalCount", totalCount)
            .addValue("successCount", successCount)
            .addValue("failCount", failCount)
            .addValue("message", message));
    }

    private void saveCrawlFailure(long crawlHistoryId, CrawlFailureInput failure) {
        String sql = """
            INSERT INTO crawl_failures (
              crawl_history_id,
              target_url,
              external_id,
              title,
              error_type,
              error_message,
              retry_count,
              created_at
            )
            VALUES (
              :crawlHistoryId,
              :targetUrl,
              :externalId,
              :title,
              :errorType,
              :errorMessage,
              0,
              CURRENT_TIMESTAMP
            )
            """;

        jdbc.update(sql, new MapSqlParameterSource()
            .addValue("crawlHistoryId", crawlHistoryId)
            .addValue("targetUrl", trimToNull(failure.targetUrl()))
            .addValue("externalId", trimToNull(failure.externalId()))
            .addValue("title", trimToNull(failure.title()))
            .addValue("errorType", trimToNull(failure.errorType()))
            .addValue("errorMessage", trimToNull(failure.errorMessage())));
    }

    private void ensureReferenceData() {
        ensurePlatformReferences();
        ensureGenreReferences();
        ensureWeekdayReferences();
    }

    private void ensurePlatformReferences() {
        Map<String, Long> existing = loadCodeIdMap("platforms");
        String sql = """
            INSERT INTO platforms (code, name, base_url, is_active, created_at, updated_at)
            VALUES (:code, :name, :baseUrl, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

        for (PlatformReference reference : PLATFORM_REFERENCES) {
            if (existing.containsKey(reference.code())) {
                continue;
            }
            jdbc.update(sql, new MapSqlParameterSource()
                .addValue("code", reference.code())
                .addValue("name", reference.name())
                .addValue("baseUrl", reference.baseUrl()));
        }
    }

    private void ensureGenreReferences() {
        Map<String, Long> existing = loadCodeIdMap("genres");
        String sql = """
            INSERT INTO genres (code, name, sort_order, is_active, created_at, updated_at)
            VALUES (:code, :name, :sortOrder, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

        for (GenreReference reference : GENRE_REFERENCES) {
            if (existing.containsKey(reference.code())) {
                continue;
            }
            jdbc.update(sql, new MapSqlParameterSource()
                .addValue("code", reference.code())
                .addValue("name", reference.name())
                .addValue("sortOrder", reference.sortOrder()));
        }
    }

    private void ensureWeekdayReferences() {
        Map<String, Long> existing = loadCodeIdMap("weekdays");
        String sql = """
            INSERT INTO weekdays (code, name, sort_order)
            VALUES (:code, :name, :sortOrder)
            """;

        for (WeekdayReference reference : WEEKDAY_REFERENCES) {
            if (existing.containsKey(reference.code())) {
                continue;
            }
            jdbc.update(sql, new MapSqlParameterSource()
                .addValue("code", reference.code())
                .addValue("name", reference.name())
                .addValue("sortOrder", reference.sortOrder()));
        }
    }

    private Map<String, Long> loadCodeIdMap(String tableName) {
        String sql = "SELECT code, id FROM " + tableName;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, new MapSqlParameterSource());

        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            result.put((String) row.get("code"), ((Number) row.get("id")).longValue());
        }
        return result;
    }

    private Long getPlatformId(String platformCode) {
        String sql = "SELECT id FROM platforms WHERE code = :platformCode";
        List<Long> ids = jdbc.query(sql,
            new MapSqlParameterSource("platformCode", platformCode),
            (rs, rowNum) -> rs.getLong("id"));

        if (ids.isEmpty()) {
            throw new IllegalStateException(platformCode + " 플랫폼이 없습니다.");
        }
        return ids.get(0);
    }

    private JsonNode fetchJson(String url) {
        JsonNode body = restClient.get()
            .uri(url)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(JsonNode.class);

        if (body == null) {
            throw new IllegalStateException("빈 응답을 받았습니다: " + url);
        }
        return body;
    }

    private String getExternalId(JsonNode node) {
        JsonNode idNode = node.path("titleId");
        if (idNode.isMissingNode() || idNode.isNull()) {
            return null;
        }
        String text = trimToNull(idNode.asText(null));
        return text;
    }

    private List<String> mapKakaoGenreCodes(String kakaoGenreCode) {
        String normalized = trimToNull(kakaoGenreCode);
        if (normalized == null || "all".equalsIgnoreCase(normalized)) {
            return List.of();
        }
        return KAKAO_GENRE_CODE_MAP.getOrDefault(normalized, List.of());
    }

    private String kakaoSectionKey(String placement) {
        String normalized = trimToNull(placement);
        if (normalized == null) {
            return "UNKNOWN";
        }
        return normalized.replace("timetable_", "").toUpperCase(Locale.ROOT);
    }

    private String joinKakaoAuthors(JsonNode authors) {
        if (!authors.isArray()) {
            return null;
        }

        List<String> names = collectKakaoAuthorNames(authors, false);
        if (names.isEmpty()) {
            names = collectKakaoAuthorNames(authors, true);
        }
        return names.isEmpty() ? null : String.join(", ", names);
    }

    private List<String> collectKakaoAuthorNames(JsonNode authors, boolean includePublishers) {
        List<String> names = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (JsonNode author : authors) {
            String type = trimToNull(author.path("type").asText(null));
            if (!includePublishers && "PUBLISHER".equals(type)) {
                continue;
            }

            String name = trimToNull(author.path("name").asText(null));
            if (name != null && seen.add(name)) {
                names.add(name);
            }
        }
        return names;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String normalizeKakaoImageUrl(String imageUrl) {
        String normalized = trimToNull(imageUrl);
        if (normalized == null) {
            return null;
        }

        int queryIndex = normalized.indexOf('?');
        String path = queryIndex >= 0 ? normalized.substring(0, queryIndex) : normalized;
        String lowerPath = path.toLowerCase(Locale.ROOT);
        if (lowerPath.endsWith(".webp")
            || lowerPath.endsWith(".png")
            || lowerPath.endsWith(".jpg")
            || lowerPath.endsWith(".jpeg")
            || lowerPath.endsWith(".gif")) {
            return normalized;
        }

        String suffix = queryIndex >= 0 ? normalized.substring(queryIndex) : "";
        return path + ".webp" + suffix;
    }

    private String buildNaverWebtoonUrl(String externalId) {
        return NAVER_BASE_URL + "/webtoon/list?titleId=" + externalId;
    }

    private String buildKakaoWebtoonUrl(String externalId, String seoId, String title) {
        String pathTitle = firstNonBlank(seoId, title);
        if (pathTitle == null) {
            return KAKAO_BASE_URL + "/content/" + externalId;
        }
        return KAKAO_BASE_URL + "/content/" + pathTitle.replace(' ', '-') + "/" + externalId;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimErrorMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() > 400 ? normalized.substring(0, 400) : normalized;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static final class CrawlWebtoon {

        private final String externalId;
        private String title;
        private String author;
        private String description;
        private String originalUrl;
        private String thumbnailUrl;
        private boolean adult;
        private boolean finished;
        private boolean rest;
        private final Set<String> genreCodes = new LinkedHashSet<>();
        private final Set<String> weekdayCodes = new LinkedHashSet<>();
        private final Map<PopularityRankingKey, Integer> popularityRankings = new LinkedHashMap<>();

        private CrawlWebtoon(String externalId) {
            this.externalId = externalId;
        }

        private void mergeNaverBaseInfo(JsonNode node, String originalUrl) {
            String parsedTitle = trimStatic(node.path("titleName").asText(null));
            String parsedAuthor = trimStatic(node.path("author").asText(null));
            String parsedThumbnail = trimStatic(node.path("thumbnailUrl").asText(null));

            mergeBaseInfo(parsedTitle, parsedAuthor, null, parsedThumbnail, originalUrl, node.path("adult").asBoolean(false));
            this.finished = this.finished || node.path("finish").asBoolean(false);
            this.rest = this.rest || node.path("rest").asBoolean(false);
        }

        private void mergeKakaoBaseInfo(
            String title,
            String author,
            String description,
            String thumbnailUrl,
            String originalUrl,
            boolean adult
        ) {
            mergeBaseInfo(title, author, description, thumbnailUrl, originalUrl, adult);
        }

        private void mergeBaseInfo(
            String parsedTitle,
            String parsedAuthor,
            String parsedDescription,
            String parsedThumbnail,
            String parsedOriginalUrl,
            boolean parsedAdult
        ) {
            if (parsedTitle != null) {
                this.title = parsedTitle;
            }
            if (parsedAuthor != null) {
                this.author = parsedAuthor;
            }
            if (parsedDescription != null) {
                this.description = parsedDescription;
            }
            if (parsedThumbnail != null) {
                this.thumbnailUrl = parsedThumbnail;
            }
            if (parsedOriginalUrl != null) {
                this.originalUrl = parsedOriginalUrl;
            }

            this.adult = this.adult || parsedAdult;
        }

        private void markCompleted() {
            this.finished = true;
        }

        private void addPopularityRanking(String rankingType, String rankingKey, int rankPosition) {
            String normalizedType = trimStatic(rankingType);
            String normalizedKey = trimStatic(rankingKey);
            if (normalizedType == null || normalizedKey == null || rankPosition <= 0) {
                return;
            }

            popularityRankings.merge(
                new PopularityRankingKey(normalizedType, normalizedKey),
                rankPosition,
                Math::min
            );
        }

        private static String trimStatic(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }

        private String status() {
            if (finished) {
                return "COMPLETED";
            }
            if (rest) {
                return "HIATUS";
            }
            return "ONGOING";
        }

        private boolean isCompleted() {
            return finished;
        }

        private String externalId() {
            return externalId;
        }

        private String title() {
            return title;
        }

        private String author() {
            return author;
        }

        private String description() {
            return description;
        }

        private String originalUrl() {
            return originalUrl;
        }

        private String thumbnailUrl() {
            return thumbnailUrl;
        }

        private boolean adult() {
            return adult;
        }

        private Set<String> genreCodes() {
            return genreCodes;
        }

        private Set<String> weekdayCodes() {
            return weekdayCodes;
        }

        private Collection<PopularityRanking> popularityRankings() {
            return popularityRankings.entrySet().stream()
                .map(entry -> new PopularityRanking(
                    entry.getKey().rankingType(),
                    entry.getKey().rankingKey(),
                    entry.getValue()
                ))
                .toList();
        }
    }

    private record CrawlFailureInput(
        String targetUrl,
        String externalId,
        String title,
        String errorType,
        String errorMessage
    ) {
    }

    private record CrawlCollectionResult(
        Map<String, CrawlWebtoon> webtoonsByExternalId,
        List<CrawlFailureInput> collectionFailures
    ) {
    }

    private record PersistResult(
        int totalCount,
        int insertedCount,
        int updatedCount,
        int successCount,
        int failCount
    ) {
    }

    private record PopularityRankingKey(String rankingType, String rankingKey) {
    }

    private record PopularityRanking(String rankingType, String rankingKey, int rankPosition) {
    }

    private record GenreFetchPlan(String apiGenreCode, String dbGenreCode) {
    }

    private record KakaoTimetableFetchPlan(String placement, String weekdayCode, boolean completed) {
    }

    private record PlatformReference(String code, String name, String baseUrl) {
    }

    private record GenreReference(String code, String name, int sortOrder) {
    }

    private record WeekdayReference(String code, String name, int sortOrder) {
    }

    private enum CrawlerTarget {
        NAVER(NAVER_PLATFORM_CODE, "네이버 웹툰"),
        KAKAO(KAKAO_PLATFORM_CODE, "카카오 웹툰");

        private final String platformCode;
        private final String displayName;

        CrawlerTarget(String platformCode, String displayName) {
            this.platformCode = platformCode;
            this.displayName = displayName;
        }

        private String platformCode() {
            return platformCode;
        }

        private String displayName() {
            return displayName;
        }
    }
}
