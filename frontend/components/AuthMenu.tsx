"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useMemo, useRef, useState } from "react";
import {
  AUTH_SESSION_CHANGED_EVENT,
  AuthSession,
  clearAuthSession,
  readAuthSession
} from "@/lib/auth-client";
import {
  FAVORITES_CHANGED_EVENT,
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
  { code: "COMPLETED", name: "완결" },
  { code: "UNKNOWN", name: "요일 미정" }
];

type FavoriteWeekdayGroup = {
  code: string;
  name: string;
  items: FavoriteWebtoon[];
};

function toNextPath(pathname: string): string {
  if (!pathname || pathname === "/login") {
    return "/mypage";
  }
  return pathname;
}

function groupFavoritesByWeekday(favorites: FavoriteWebtoon[]): FavoriteWeekdayGroup[] {
  const groups = new Map<string, FavoriteWeekdayGroup>(
    WEEKDAY_GROUPS.map((group) => [group.code, { ...group, items: [] }])
  );

  for (const favorite of favorites) {
    const weekdays = favorite.weekdays.length > 0 ? favorite.weekdays : [{ code: "UNKNOWN", name: "요일 미정" }];
    for (const weekday of weekdays) {
      if (!groups.has(weekday.code)) {
        groups.set(weekday.code, { code: weekday.code, name: weekday.name, items: [] });
      }
      groups.get(weekday.code)?.items.push(favorite);
    }
  }

  return Array.from(groups.values()).filter((group) => group.items.length > 0);
}

export function AuthMenu() {
  const router = useRouter();
  const pathname = usePathname() || "/webtoons";
  const profileRef = useRef<HTMLDivElement | null>(null);
  const [session, setSession] = useState<AuthSession | null>(null);
  const [favoriteWebtoons, setFavoriteWebtoons] = useState<FavoriteWebtoon[]>([]);
  const [isFavoritePanelOpen, setIsFavoritePanelOpen] = useState(false);
  const favoriteGroups = useMemo(() => groupFavoritesByWeekday(favoriteWebtoons), [favoriteWebtoons]);

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
    function syncFavoriteState() {
      setFavoriteWebtoons(readFavoriteWebtoons());
    }

    syncFavoriteState();
    window.addEventListener("storage", syncFavoriteState);
    window.addEventListener(FAVORITES_CHANGED_EVENT, syncFavoriteState as EventListener);

    return () => {
      window.removeEventListener("storage", syncFavoriteState);
      window.removeEventListener(FAVORITES_CHANGED_EVENT, syncFavoriteState as EventListener);
    };
  }, []);

  useEffect(() => {
    let active = true;

    async function syncFavorites() {
      if (!session) {
        clearFavoriteWebtoons();
        setFavoriteWebtoons([]);
        setIsFavoritePanelOpen(false);
        return;
      }

      try {
        const favorites = await syncFavoriteWebtoonsFromServer(session);
        if (active) {
          setFavoriteWebtoons(favorites);
        }
      } catch (error) {
        if (!active) {
          return;
        }

        if (error instanceof FavoriteApiError && error.status === 401) {
          clearAuthSession();
          clearFavoriteWebtoons();
          setSession(null);
          return;
        }
      }
    }

    syncFavorites();

    return () => {
      active = false;
    };
  }, [session]);

  useEffect(() => {
    if (!isFavoritePanelOpen) {
      return;
    }

    function handleMouseDown(event: MouseEvent) {
      if (profileRef.current && !profileRef.current.contains(event.target as Node)) {
        setIsFavoritePanelOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setIsFavoritePanelOpen(false);
      }
    }

    document.addEventListener("mousedown", handleMouseDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("mousedown", handleMouseDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isFavoritePanelOpen]);

  function logout() {
    clearAuthSession();
    clearFavoriteWebtoons();
    setSession(null);
    setFavoriteWebtoons([]);
    setIsFavoritePanelOpen(false);
    router.push("/");
    router.refresh();
  }

  if (!session) {
    const nextPath = encodeURIComponent(toNextPath(pathname));
    return (
      <Link className="auth-link" href={`/login?next=${nextPath}`}>
        로그인
      </Link>
    );
  }

  const displayName = session.nickname || session.username;
  const initial = displayName.slice(0, 1);

  return (
    <div className="auth-nav">
      <div className="auth-profile" ref={profileRef}>
        <button
          className="auth-user-button"
          type="button"
          onClick={() => setIsFavoritePanelOpen((current) => !current)}
          aria-expanded={isFavoritePanelOpen}
          aria-label="요일별 즐겨찾기 보기"
        >
          <span className="auth-user-avatar">{initial}</span>
          <span className="auth-user-copy">
            <span className="auth-user-label">내 서재</span>
            <span className="auth-user-name">{displayName}</span>
          </span>
          <span className="auth-user-count">{favoriteWebtoons.length}</span>
        </button>

        {isFavoritePanelOpen ? (
          <div className="favorite-weekday-popover">
            <div className="favorite-popover-head">
              <strong>요일별 즐겨찾기</strong>
              <Link href="/mypage" onClick={() => setIsFavoritePanelOpen(false)}>
                전체 보기
              </Link>
            </div>

            {favoriteGroups.length === 0 ? (
              <p className="favorite-popover-empty">웹툰 카드의 하트를 눌러 내 서재를 채워보세요.</p>
            ) : (
              <div className="favorite-weekday-groups">
                {favoriteGroups.map((group) => (
                  <section className="favorite-weekday-group" key={group.code}>
                    <div className="favorite-weekday-head">
                      <span>{group.name}</span>
                      <strong>{group.items.length}</strong>
                    </div>
                    <ul>
                      {group.items.slice(0, 5).map((item) => (
                        <li key={`${group.code}-${item.id}`}>
                          <Link href={`/webtoons/${item.id}`} onClick={() => setIsFavoritePanelOpen(false)}>
                            <span>{item.title}</span>
                            <small>{item.statusName}</small>
                          </Link>
                        </li>
                      ))}
                    </ul>
                  </section>
                ))}
              </div>
            )}
          </div>
        ) : null}
      </div>
      <Link className="auth-link" href="/mypage">
        마이페이지
      </Link>
      <button className="auth-link-button" type="button" onClick={logout}>
        로그아웃
      </button>
    </div>
  );
}
