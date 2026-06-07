"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { AUTH_SESSION_CHANGED_EVENT, AuthSession, clearAuthSession, readAuthSession } from "@/lib/auth-client";
import {
  FavoriteApiError,
  clearFavoriteWebtoons,
  readFavoriteWebtoons,
  syncFavoriteWebtoonsFromServer
} from "@/lib/favorites-client";
import { FavoriteWebtoon } from "@/lib/types";

const WEEKDAY_GROUPS = [
  { code: "MONDAY", name: "월요일" },
  { code: "TUESDAY", name: "화요일" },
  { code: "WEDNESDAY", name: "수요일" },
  { code: "THURSDAY", name: "목요일" },
  { code: "FRIDAY", name: "금요일" },
  { code: "SATURDAY", name: "토요일" },
  { code: "SUNDAY", name: "일요일" },
  { code: "DAILY_PLUS", name: "매일+" },
  { code: "UNKNOWN", name: "요일 미정" }
];

type FavoriteWeekdayGroup = {
  code: string;
  name: string;
  items: FavoriteWebtoon[];
};

function isOngoingFavorite(item: FavoriteWebtoon): boolean {
  return item.status === "ONGOING" || item.statusName === "연재중";
}

function groupOngoingFavorites(favorites: FavoriteWebtoon[]): FavoriteWeekdayGroup[] {
  const groups = new Map<string, FavoriteWeekdayGroup>(
    WEEKDAY_GROUPS.map((group) => [group.code, { ...group, items: [] }])
  );

  for (const favorite of favorites.filter(isOngoingFavorite)) {
    const visibleWeekdays = favorite.weekdays.filter((weekday) => weekday.code !== "COMPLETED");
    const weekdays = visibleWeekdays.length > 0 ? visibleWeekdays : [{ code: "UNKNOWN", name: "요일 미정" }];

    for (const weekday of weekdays) {
      if (!groups.has(weekday.code)) {
        groups.set(weekday.code, { code: weekday.code, name: weekday.name, items: [] });
      }
      groups.get(weekday.code)?.items.push(favorite);
    }
  }

  return Array.from(groups.values()).filter((group) => group.items.length > 0);
}

export default function FavoritesPage() {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [favoriteWebtoons, setFavoriteWebtoons] = useState<FavoriteWebtoon[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const favoriteGroups = useMemo(() => groupOngoingFavorites(favoriteWebtoons), [favoriteWebtoons]);
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
        if (active) {
          setFavoriteWebtoons(favorites);
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
          setFavoriteWebtoons(readFavoriteWebtoons());
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
          <p className="description">즐겨찾기한 작품 중 현재 연재중인 웹툰만 요일별로 모았습니다.</p>
        </div>
        <strong>{ongoingCount}개</strong>
      </div>

      {error ? <p className="favorites-message">{error}</p> : null}
      {isLoading ? <p className="favorites-message">불러오는 중...</p> : null}

      {!isLoading && favoriteGroups.length === 0 ? (
        <div className="favorites-empty reveal">
          <p>연재중 즐겨찾기 웹툰이 아직 없습니다.</p>
          <Link className="source-btn" href="/webtoons?sort=popular">
            웹툰 둘러보기
          </Link>
        </div>
      ) : null}

      <div className="favorite-day-grid">
        {favoriteGroups.map((group) => (
          <section className="favorite-day-section reveal" key={group.code}>
            <div className="favorite-day-head">
              <h2>{group.name}</h2>
              <span>{group.items.length}</span>
            </div>
            <div className="favorite-day-list">
              {group.items.map((item) => (
                <Link className="favorite-day-item" href={`/webtoons/${item.id}`} key={`${group.code}-${item.id}`}>
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
              ))}
            </div>
          </section>
        ))}
      </div>
    </section>
  );
}
