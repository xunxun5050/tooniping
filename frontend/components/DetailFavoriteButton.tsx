"use client";

import { usePathname, useRouter } from "next/navigation";
import { type MouseEvent, useEffect, useState } from "react";
import { AUTH_SESSION_CHANGED_EVENT, AuthSession, clearAuthSession, getAuthSession, readAuthSession } from "@/lib/auth-client";
import {
  FAVORITES_CHANGED_EVENT,
  FavoriteApiError,
  isFavoriteWebtoonById,
  toggleFavoriteWebtoonRemote
} from "@/lib/favorites-client";
import { WebtoonCard } from "@/lib/types";

type Props = {
  webtoon: WebtoonCard;
};

export function DetailFavoriteButton({ webtoon }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const [session, setSession] = useState<AuthSession | null>(null);
  const [isFavorite, setIsFavorite] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    let active = true;

    function syncFavoriteAndAuthState() {
      setSession(readAuthSession());
      setIsFavorite(isFavoriteWebtoonById(webtoon.id));
    }

    syncFavoriteAndAuthState();
    getAuthSession().then((nextSession) => {
      if (active) {
        setSession(nextSession);
        setIsFavorite(isFavoriteWebtoonById(webtoon.id));
      }
    });
    window.addEventListener("storage", syncFavoriteAndAuthState);
    window.addEventListener(AUTH_SESSION_CHANGED_EVENT, syncFavoriteAndAuthState as EventListener);
    window.addEventListener(FAVORITES_CHANGED_EVENT, syncFavoriteAndAuthState as EventListener);

    return () => {
      active = false;
      window.removeEventListener("storage", syncFavoriteAndAuthState);
      window.removeEventListener(AUTH_SESSION_CHANGED_EVENT, syncFavoriteAndAuthState as EventListener);
      window.removeEventListener(FAVORITES_CHANGED_EVENT, syncFavoriteAndAuthState as EventListener);
    };
  }, [webtoon.id]);

  async function handleClick(event: MouseEvent<HTMLButtonElement>) {
    event.preventDefault();

    if (isSaving) {
      return;
    }

    const activeSession = session ?? await getAuthSession();
    if (!activeSession) {
      const nextPath = encodeURIComponent(pathname || `/webtoons/${webtoon.id}`);
      router.push(`/login?next=${nextPath}`);
      return;
    }
    setSession(activeSession);

    setIsSaving(true);
    try {
      const result = await toggleFavoriteWebtoonRemote(activeSession, webtoon);
      setIsFavorite(result.favorited);
    } catch (error) {
      if (error instanceof FavoriteApiError && error.status === 401) {
        clearAuthSession();
        const nextPath = encodeURIComponent(pathname || `/webtoons/${webtoon.id}`);
        router.push(`/login?next=${nextPath}`);
      }
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <button
      className={`detail-favorite-btn ${isFavorite && session ? "active" : ""}`}
      type="button"
      onClick={handleClick}
      disabled={isSaving}
      aria-pressed={isFavorite && Boolean(session)}
    >
      <span aria-hidden="true">♥</span>
      {isFavorite && session ? "즐겨찾기 해제" : "즐겨찾기"}
    </button>
  );
}
