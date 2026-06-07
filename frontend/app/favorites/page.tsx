"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { AUTH_SESSION_CHANGED_EVENT, AuthSession, clearAuthSession, readAuthSession } from "@/lib/auth-client";
import {
  FavoriteApiError,
  clearFavoriteWebtoons,
  replaceFavoriteWebtoons,
  readFavoriteWebtoons,
  syncFavoriteWebtoonsFromServer
} from "@/lib/favorites-client";
import { ApiResponse, CodeName, FavoriteWebtoon, WebtoonDetail } from "@/lib/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
const CALENDAR_WEEKDAYS = [
  { code: "MONDAY", name: "월요일" },
  { code: "TUESDAY", name: "화요일" },
  { code: "WEDNESDAY", name: "수요일" },
  { code: "THURSDAY", name: "목요일" },
  { code: "FRIDAY", name: "금요일" },
  { code: "SATURDAY", name: "토요일" },
  { code: "SUNDAY", name: "일요일" }
];

type FavoriteViewMode = "weekday" | "genre";

type FavoriteGroup = {
  code: string;
  name: string;
  items: FavoriteWebtoon[];
};

function isOngoingFavorite(item: FavoriteWebtoon): boolean {
  return item.status === "ONGOING" || item.statusName === "연재중";
}

async function fetchFavoriteGenres(webtoonId: number): Promise<CodeName[]> {
  const response = await fetch(`${API_BASE_URL}/api/webtoons/${webtoonId}`);
  if (!response.ok) {
    return [];
  }

  const payload = (await response.json()) as ApiResponse<WebtoonDetail>;
  return payload.success ? payload.data.genres : [];
}

async function enrichFavoritesWithGenres(favorites: FavoriteWebtoon[]): Promise<FavoriteWebtoon[]> {
  const ongoingFavorites = favorites.filter(isOngoingFavorite);
  const favoritesMissingGenres = ongoingFavorites.filter((favorite) => !favorite.genres || favorite.genres.length === 0);

  if (favoritesMissingGenres.length === 0) {
    return favorites;
  }

  const genreEntries = await Promise.all(
    favoritesMissingGenres.map(async (favorite) => {
      try {
        return [favorite.id, await fetchFavoriteGenres(favorite.id)] as const;
      } catch {
        return [favorite.id, [] as CodeName[]] as const;
      }
    })
  );
  const genresByWebtoonId = new Map(genreEntries);

  return favorites.map((favorite) => {
    const genres = genresByWebtoonId.get(favorite.id);
    return genres ? { ...favorite, genres } : favorite;
  });
}

function groupOngoingFavoritesByWeekday(favorites: FavoriteWebtoon[]): FavoriteGroup[] {
  const groups = new Map<string, FavoriteGroup>(
    CALENDAR_WEEKDAYS.map((group) => [group.code, { ...group, items: [] }])
  );

  for (const favorite of favorites.filter(isOngoingFavorite)) {
    const weekdayCodes = favorite.weekdays.map((weekday) => weekday.code);
    const targetWeekdays = weekdayCodes.includes("DAILY_PLUS")
      ? CALENDAR_WEEKDAYS
      : CALENDAR_WEEKDAYS.filter((weekday) => weekdayCodes.includes(weekday.code));

    for (const weekday of targetWeekdays) {
      groups.get(weekday.code)?.items.push(favorite);
    }
  }

  return Array.from(groups.values());
}

function groupOngoingFavoritesByGenre(favorites: FavoriteWebtoon[]): FavoriteGroup[] {
  const groups = new Map<string, FavoriteGroup>();

  for (const favorite of favorites.filter(isOngoingFavorite)) {
    const genres = favorite.genres && favorite.genres.length > 0 ? favorite.genres : [{ code: "UNKNOWN", name: "장르 미정" }];

    for (const genre of genres) {
      if (!groups.has(genre.code)) {
        groups.set(genre.code, { code: genre.code, name: genre.name, items: [] });
      }
      groups.get(genre.code)?.items.push(favorite);
    }
  }

  return Array.from(groups.values()).sort((a, b) => b.items.length - a.items.length || a.name.localeCompare(b.name, "ko"));
}

export default function FavoritesPage() {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [favoriteWebtoons, setFavoriteWebtoons] = useState<FavoriteWebtoon[]>([]);
  const [viewMode, setViewMode] = useState<FavoriteViewMode>("weekday");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const weekdayGroups = useMemo(() => groupOngoingFavoritesByWeekday(favoriteWebtoons), [favoriteWebtoons]);
  const genreGroups = useMemo(() => groupOngoingFavoritesByGenre(favoriteWebtoons), [favoriteWebtoons]);
  const ongoingCount = useMemo(() => favoriteWebtoons.filter(isOngoingFavorite).length, [favoriteWebtoons]);

  useEffect(() => {
    function syncSession() {
      setSession(readAuthSession());
    }

    syncSession();
    window.addEventListener("storage", syncSession);
    window.addEventListener(AUTH_SESSION_CHANGED_EVENT, syncSession as EventListener);

    return () => {
      window.removeEventListener("storage", syncSession);
      window.removeEventListener(AUTH_SESSION_CHANGED_EVENT, syncSession as EventListener);
    };
  }, []);

  useEffect(() => {
    let active = true;

    async function loadFavorites() {
      setError(null);
      if (!session) {
        clearFavoriteWebtoons();
        setFavoriteWebtoons([]);
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      try {
        const favorites = await syncFavoriteWebtoonsFromServer(session);
        const enrichedFavorites = await enrichFavoritesWithGenres(favorites);
        if (active) {
          setFavoriteWebtoons(enrichedFavorites);
          replaceFavoriteWebtoons(enrichedFavorites);
        }
      } catch (loadError) {
        if (!active) {
          return;
        }
        if (loadError instanceof FavoriteApiError && loadError.status === 401) {
          clearAuthSession();
          clearFavoriteWebtoons();
          setSession(null);
          setFavoriteWebtoons([]);
          setError("로그인이 만료되었습니다. 다시 로그인해 주세요.");
        } else {
          const cachedFavorites = readFavoriteWebtoons();
          setFavoriteWebtoons(cachedFavorites);
          setError("즐겨찾기 목록을 불러오지 못했습니다.");
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    loadFavorites();

    return () => {
      active = false;
    };
  }, [session]);

  if (!session) {
    return (
      <section className="favorites-page">
        <div className="favorites-head reveal">
          <p className="eyebrow">MY LIBRARY</p>
          <h1>연재중 즐겨찾기</h1>
          <p className="description">로그인하면 저장한 연재중 웹툰을 요일별로 볼 수 있습니다.</p>
          <Link className="source-btn" href="/login?next=%2Ffavorites">
            로그인하기
          </Link>
        </div>
      </section>
    );
  }

  return (
    <section className="favorites-page">
      <div className="favorites-head reveal">
        <div>
          <p className="eyebrow">MY LIBRARY</p>
          <h1>연재중 즐겨찾기</h1>
          <p className="description">즐겨찾기한 작품 중 현재 연재중인 웹툰을 요일별, 장르별로 모았습니다.</p>
        </div>
        <strong>{ongoingCount}개</strong>
      </div>

      <div className="favorites-view-toggle reveal" role="group" aria-label="내 서재 보기 방식">
        <button
          className={viewMode === "weekday" ? "active" : ""}
          type="button"
          onClick={() => setViewMode("weekday")}
        >
          요일별
        </button>
        <button
          className={viewMode === "genre" ? "active" : ""}
          type="button"
          onClick={() => setViewMode("genre")}
        >
          장르별
        </button>
      </div>

      {error ? <p className="favorites-message">{error}</p> : null}
      {isLoading ? <p className="favorites-message">불러오는 중...</p> : null}

      {!isLoading && ongoingCount === 0 ? (
        <div className="favorites-empty reveal">
          <p>연재중 즐겨찾기 웹툰이 아직 없습니다.</p>
          <Link className="source-btn" href="/webtoons?sort=popular">
            웹툰 둘러보기
          </Link>
        </div>
      ) : null}

      {!isLoading && ongoingCount > 0 && viewMode === "weekday" ? <FavoriteWeekdayCalendar groups={weekdayGroups} /> : null}
      {!isLoading && ongoingCount > 0 && viewMode === "genre" ? <FavoriteGenreGroups groups={genreGroups} /> : null}
    </section>
  );
}

function FavoriteWeekdayCalendar({ groups }: { groups: FavoriteGroup[] }) {
  return (
    <div className="favorite-calendar-grid">
      {groups.map((group) => (
        <section className="favorite-calendar-column reveal" key={group.code}>
          <div className="favorite-calendar-head">
            <h2>{group.name.replace("요일", "")}</h2>
            <span>{group.items.length}</span>
          </div>
          <div className="favorite-calendar-list">
            {group.items.length === 0 ? <p className="favorite-calendar-empty">비어 있음</p> : null}
            {group.items.map((item) => (
              <FavoriteListItem item={item} key={`${group.code}-${item.id}`} />
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}

function FavoriteGenreGroups({ groups }: { groups: FavoriteGroup[] }) {
  return (
    <div className="favorite-genre-grid">
      {groups.map((group) => (
        <section className="favorite-genre-section reveal" key={group.code}>
          <div className="favorite-day-head">
            <h2>{group.name}</h2>
            <span>{group.items.length}</span>
          </div>
          <div className="favorite-day-list">
            {group.items.map((item) => (
              <FavoriteListItem item={item} key={`${group.code}-${item.id}`} />
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}

function FavoriteListItem({ item }: { item: FavoriteWebtoon }) {
  return (
    <Link className="favorite-day-item" href={`/webtoons/${item.id}`}>
      <div className="favorite-day-thumb-wrap">
        {item.thumbnailUrl ? (
          <img className="favorite-day-thumb" src={item.thumbnailUrl} alt={item.title} />
        ) : (
          <div className="favorite-day-thumb placeholder">NO IMAGE</div>
        )}
      </div>
      <div>
        <strong>{item.title}</strong>
        <p>{item.author || "작가 미상"}</p>
      </div>
    </Link>
  );
}
