"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ProfileAvatar } from "@/components/ProfileAvatar";
import {
  AUTH_SESSION_CHANGED_EVENT,
  AuthSession,
  clearAuthSession,
  getAuthSession,
  logoutAuthSession,
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

function toNextPath(pathname: string): string {
  if (!pathname || pathname === "/login") {
    return "/mypage";
  }
  return pathname;
}

export function AuthMenu() {
  const router = useRouter();
  const pathname = usePathname() || "/webtoons";
  const [session, setSession] = useState<AuthSession | null>(null);
  const [favoriteWebtoons, setFavoriteWebtoons] = useState<FavoriteWebtoon[]>([]);

  useEffect(() => {
    let active = true;

    function syncSession() {
      setSession(readAuthSession());
    }

    syncSession();
    getAuthSession().then((nextSession) => {
      if (active) {
        setSession(nextSession);
      }
    });
    window.addEventListener("storage", syncSession);
    window.addEventListener(AUTH_SESSION_CHANGED_EVENT, syncSession as EventListener);

    return () => {
      active = false;
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

  async function logout() {
    await logoutAuthSession();
    clearFavoriteWebtoons();
    setSession(null);
    setFavoriteWebtoons([]);
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

  return (
    <div className="auth-nav">
      <Link className="auth-user-button" href="/favorites" aria-label="연재중 즐겨찾기 요일별 보기">
        <ProfileAvatar seed={session.avatarSeed} palette={session.avatarPalette} label={displayName} size="sm" />
        <span className="auth-user-copy">
          <span className="auth-user-label">내 서재</span>
          <span className="auth-user-name">{displayName}</span>
        </span>
        <span className="auth-user-count">{favoriteWebtoons.length}</span>
      </Link>
      <Link className="auth-link" href="/mypage">
        마이페이지
      </Link>
      <button className="auth-link-button" type="button" onClick={logout}>
        로그아웃
      </button>
    </div>
  );
}
