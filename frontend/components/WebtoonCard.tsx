"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { type MouseEvent, useEffect, useState } from "react";
import { AUTH_SESSION_CHANGED_EVENT, AuthSession, clearAuthSession, readAuthSession } from "@/lib/auth-client";
import {
  FavoriteApiError,
  FAVORITES_CHANGED_EVENT,
  isFavoriteWebtoonById,
  toggleFavoriteWebtoonRemote
} from "@/lib/favorites-client";
import { toPlatformStyleClass } from "@/lib/platform-style";
import { WebtoonCard as WebtoonCardType } from "@/lib/types";

type Props = {
  webtoon: WebtoonCardType;
  showSourceButton?: boolean;
  compact?: boolean;
  className?: string;
};

export function WebtoonCard({
  webtoon,
  showSourceButton = true,
  compact = false,
  className
}: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const [session, setSession] = useState<AuthSession | null>(null);
  const [isFavorite, setIsFavorite] = useState(false);
  const [isSavingFavorite, setIsSavingFavorite] = useState(false);

  useEffect(() => {
    function syncFavoriteAndAuthState() {
      setSession(readAuthSession());
      setIsFavorite(isFavoriteWebtoonById(webtoon.id));
    }

    syncFavoriteAndAuthState();
    window.addEventListener("storage", syncFavoriteAndAuthState);
    window.addEventListener(AUTH_SESSION_CHANGED_EVENT, syncFavoriteAndAuthState as EventListener);
    window.addEventListener(FAVORITES_CHANGED_EVENT, syncFavoriteAndAuthState as EventListener);

    return () => {
      window.removeEventListener("storage", syncFavoriteAndAuthState);
      window.removeEventListener(AUTH_SESSION_CHANGED_EVENT, syncFavoriteAndAuthState as EventListener);
      window.removeEventListener(FAVORITES_CHANGED_EVENT, syncFavoriteAndAuthState as EventListener);
    };
  }, [webtoon.id]);

  const genresText = webtoon.genres.map((genre) => genre.name).join(" · ");
  const weekdaysText = webtoon.weekdays.map((day) => day.name).join(" · ");
  const compactMeta = weekdaysText || genresText || webtoon.statusName;
  const platformStyleClass = toPlatformStyleClass(webtoon.platform.code);
  const cardClassName = ["webtoon-card", "reveal", platformStyleClass, compact ? "compact" : "", className ?? ""]
    .filter(Boolean)
    .join(" ");

  async function handleFavoriteClick(event: MouseEvent<HTMLButtonElement>) {
    event.preventDefault();
    event.stopPropagation();

    if (isSavingFavorite) {
      return;
    }

    if (!session) {
      const nextPath = encodeURIComponent(pathname || "/webtoons");
      router.push(`/login?next=${nextPath}`);
      return;
    }

    setIsSavingFavorite(true);
    try {
      const result = await toggleFavoriteWebtoonRemote(session, webtoon);
      setIsFavorite(result.favorited);
    } catch (error) {
      if (error instanceof FavoriteApiError && error.status === 401) {
        clearAuthSession();
        const nextPath = encodeURIComponent(pathname || "/webtoons");
        router.push(`/login?next=${nextPath}`);
      }
    } finally {
      setIsSavingFavorite(false);
    }
  }

  return (
    <article className={cardClassName}>
      <button
        type="button"
        className={`favorite-heart ${isFavorite && session ? "active" : ""}`}
        onClick={handleFavoriteClick}
        disabled={isSavingFavorite}
        aria-label={isFavorite ? "즐겨찾기 해제" : "즐겨찾기 추가"}
        title={session ? "즐겨찾기" : "로그인 후 즐겨찾기 가능"}
      >
        ♥
      </button>
      <Link href={`/webtoons/${webtoon.id}`} className="webtoon-card-link">
        <div className="webtoon-thumb-wrap">
          {webtoon.thumbnailUrl ? (
            <img className="webtoon-thumb" src={webtoon.thumbnailUrl} alt={webtoon.title} />
          ) : (
            <div className="webtoon-thumb placeholder">NO IMAGE</div>
          )}
        </div>
        <div className="webtoon-meta">
          <h3>{webtoon.title}</h3>
          <p className="author">{webtoon.author || "작가 미상"}</p>
          {compact ? (
            <p className="compact-meta">{compactMeta}</p>
          ) : (
            <>
              <p className="chips">
                {genresText}
                {genresText && weekdaysText ? " | " : ""}
                {weekdaysText}
              </p>
              <p className="status">{webtoon.statusName}</p>
            </>
          )}
        </div>
      </Link>
      {showSourceButton ? (
        <a className={`source-btn webtoon-source-btn ${platformStyleClass}`} href={webtoon.originalUrl} target="_blank" rel="noreferrer">
          {webtoon.platform.name} 바로가기
        </a>
      ) : null}
    </article>
  );
}
