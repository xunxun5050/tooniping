import Link from "next/link";
import { redirect } from "next/navigation";
import { WebtoonCard } from "@/components/WebtoonCard";
import { fetchApi } from "@/lib/api";
import { PagedResult, WebtoonCard as WebtoonCardType, WebtoonFilters } from "@/lib/types";

const DEFAULT_PAGE_SIZE = 20;
const MAX_PAGE_SIZE = 100;
const SORT_OPTIONS = [
  { code: "popular", name: "인기순" },
  { code: "latest", name: "최신순" },
  { code: "title", name: "제목순" },
  { code: "weekday", name: "요일순" }
];
const SORT_OPTION_CODES = new Set(SORT_OPTIONS.map((option) => option.code));
const WEEKDAY_BY_ENGLISH_NAME: Record<string, string> = {
  Monday: "MONDAY",
  Tuesday: "TUESDAY",
  Wednesday: "WEDNESDAY",
  Thursday: "THURSDAY",
  Friday: "FRIDAY",
  Saturday: "SATURDAY",
  Sunday: "SUNDAY"
};

type SearchParams = { [key: string]: string | string[] | undefined };

type Props = {
  searchParams: Promise<SearchParams>;
};

function pick(searchParams: SearchParams, key: string): string {
  const value = searchParams[key];
  if (Array.isArray(value)) {
    return value[0] ?? "";
  }
  return value ?? "";
}

function normalizePage(rawPage: string): number {
  const parsed = Number(rawPage);
  if (!Number.isInteger(parsed) || parsed < 0) {
    return 0;
  }
  return parsed;
}

function normalizeSize(rawSize: string): number {
  const parsed = Number(rawSize);
  if (!Number.isInteger(parsed) || parsed < DEFAULT_PAGE_SIZE || parsed > MAX_PAGE_SIZE) {
    return DEFAULT_PAGE_SIZE;
  }
  return parsed;
}

function normalizeSort(rawSort: string): string {
  if (!rawSort || !SORT_OPTION_CODES.has(rawSort)) {
    return "popular";
  }
  return rawSort;
}

function getTodayWeekdayCodeInSeoul(): string {
  const todayName = new Intl.DateTimeFormat("en-US", {
    weekday: "long",
    timeZone: "Asia/Seoul"
  }).format(new Date());

  return WEEKDAY_BY_ENGLISH_NAME[todayName] ?? "MONDAY";
}

function toQueryParams(searchParams: SearchParams): URLSearchParams {
  const params = new URLSearchParams();

  for (const [key, value] of Object.entries(searchParams)) {
    if (Array.isArray(value)) {
      if (value[0]) params.set(key, value[0]);
    } else if (value) {
      params.set(key, value);
    }
  }

  return params;
}

function buildQuery(searchParams: SearchParams, updates: Record<string, string>) {
  const params = toQueryParams(searchParams);

  for (const [key, value] of Object.entries(updates)) {
    if (!value) {
      params.delete(key);
    } else {
      params.set(key, value);
    }
  }

  if (!updates.page) {
    params.delete("page");
  }

  const query = params.toString();
  return query ? `/webtoons?${query}` : "/webtoons";
}

export default async function WebtoonsPage({ searchParams }: Props) {
  const resolvedSearchParams = await searchParams;
  const keyword = pick(resolvedSearchParams, "keyword");
  const rawWeekday = pick(resolvedSearchParams, "weekday");
  const shouldConvertCompletedWeekday = rawWeekday === "COMPLETED";
  const weekday = rawWeekday === "ALL" || shouldConvertCompletedWeekday ? "" : rawWeekday;
  const genre = pick(resolvedSearchParams, "genre");
  const rawStatus = pick(resolvedSearchParams, "status");
  const status = shouldConvertCompletedWeekday && !rawStatus ? "COMPLETED" : rawStatus;
  const rawSort = pick(resolvedSearchParams, "sort");
  const sort = normalizeSort(rawSort);
  const rawPage = pick(resolvedSearchParams, "page");
  const rawSize = pick(resolvedSearchParams, "size");
  const page = normalizePage(rawPage || "0");
  const size = normalizeSize(rawSize || String(DEFAULT_PAGE_SIZE));
  const shouldUseTodayWeekdayDefault = !weekday && !keyword && !genre && !status && page === 0;
  const effectiveWeekday = shouldUseTodayWeekdayDefault ? getTodayWeekdayCodeInSeoul() : weekday;

  const canonicalParams = toQueryParams(resolvedSearchParams);
  let shouldRedirect = false;

  if (rawPage && rawPage !== String(page)) {
    canonicalParams.set("page", String(page));
    shouldRedirect = true;
  }

  if (!rawSize || rawSize !== String(size)) {
    canonicalParams.set("size", String(size));
    shouldRedirect = true;
  }

  if (!rawSort || rawSort !== sort) {
    canonicalParams.set("sort", sort);
    shouldRedirect = true;
  }

  if (rawWeekday === "ALL" || shouldConvertCompletedWeekday) {
    canonicalParams.delete("weekday");
    shouldRedirect = true;
  }

  if (shouldConvertCompletedWeekday && !rawStatus) {
    canonicalParams.set("status", "COMPLETED");
    shouldRedirect = true;
  }

  if (shouldUseTodayWeekdayDefault && rawWeekday !== effectiveWeekday) {
    canonicalParams.set("weekday", effectiveWeekday);
    shouldRedirect = true;
  }

  if (shouldRedirect) {
    redirect(`/webtoons?${canonicalParams.toString()}`);
  }

  const query = new URLSearchParams();
  if (keyword) query.set("keyword", keyword);
  if (effectiveWeekday) query.set("weekday", effectiveWeekday);
  if (genre) query.set("genre", genre);
  if (status) query.set("status", status);
  if (sort) query.set("sort", sort);
  query.set("page", String(page));
  query.set("size", String(size));

  const [filters, list] = await Promise.all([
    fetchApi<WebtoonFilters>("/api/webtoon-filters"),
    fetchApi<PagedResult<WebtoonCardType>>(`/api/webtoons?${query.toString()}`)
  ]);

  const hasPrev = list.page > 0;
  const weekdayOptions = filters.weekdays.filter((option) => option.code !== "COMPLETED");

  return (
    <section className="list-page">
      <div className="list-layout">
        <div>
          <p className="result-count reveal">총 {list.totalElements.toLocaleString()}개</p>

          <div className="card-grid">
            {list.content.map((item) => (
              <WebtoonCard key={item.id} webtoon={item} />
            ))}
          </div>

          <div className="pagination reveal">
            <Link
              className={`page-btn ${!hasPrev ? "disabled" : ""}`}
              href={hasPrev ? buildQuery(resolvedSearchParams, { page: String(list.page - 1) }) : "#"}
            >
              이전
            </Link>
            <span>
              {list.page + 1} / {Math.max(list.totalPages, 1)}
            </span>
            <Link
              className={`page-btn ${!list.hasNext ? "disabled" : ""}`}
              href={list.hasNext ? buildQuery(resolvedSearchParams, { page: String(list.page + 1) }) : "#"}
            >
              다음
            </Link>
          </div>
        </div>

        <aside className="filters-remote reveal">
          <p className="remote-title">필터 리모컨</p>
          <FilterGroup
            title="정렬"
            options={SORT_OPTIONS}
            selectedCode={sort}
            showAll={false}
            makeHref={(code) => buildQuery(resolvedSearchParams, { sort: code, page: "" })}
            clearHref={buildQuery(resolvedSearchParams, { sort: "popular", page: "" })}
          />
          <FilterGroup
            title="요일"
            options={weekdayOptions}
            selectedCode={effectiveWeekday}
            showAll={false}
            makeHref={(code) => buildQuery(resolvedSearchParams, { weekday: code, page: "" })}
            clearHref={buildQuery(resolvedSearchParams, { weekday: "ALL", page: "" })}
          />
          <FilterGroup
            title="장르"
            options={filters.genres}
            selectedCode={genre}
            makeHref={(code) => buildQuery(resolvedSearchParams, { genre: code, page: "" })}
            clearHref={buildQuery(resolvedSearchParams, { genre: "", page: "" })}
          />
          <FilterGroup
            title="연재 상태"
            options={filters.statuses}
            selectedCode={status}
            makeHref={(code) => buildQuery(resolvedSearchParams, { status: code, page: "" })}
            clearHref={buildQuery(resolvedSearchParams, { status: "", page: "" })}
          />
        </aside>
      </div>
    </section>
  );
}

function FilterGroup({
  title,
  options,
  selectedCode,
  showAll,
  makeHref,
  clearHref
}: {
  title: string;
  options: { code: string; name: string }[];
  selectedCode: string;
  showAll?: boolean;
  makeHref: (code: string) => string;
  clearHref: string;
}) {
  const shouldShowAll = showAll ?? true;

  return (
    <div className="filter-group">
      <strong>{title}</strong>
      <div className="chips-row">
        {shouldShowAll ? (
          <Link href={clearHref} className={`chip ${!selectedCode ? "active" : ""}`}>
            전체
          </Link>
        ) : null}
        {options.map((option) => (
          <Link
            key={option.code}
            href={makeHref(option.code)}
            className={`chip ${selectedCode === option.code ? "active" : ""}`}
          >
            {option.name}
          </Link>
        ))}
      </div>
    </div>
  );
}
