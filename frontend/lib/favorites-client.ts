"use client";

import { AuthSession } from "@/lib/auth-client";
import { ApiResponse, CodeName, FavoriteWebtoon, WebtoonCard } from "@/lib/types";

const FAVORITES_STORAGE_KEY = "webtoon_hub_favorite_webtoons";
export const FAVORITES_CHANGED_EVENT = "favorite-webtoons-changed";
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class FavoriteApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "FavoriteApiError";
    this.status = status;
  }
}

function emitFavoritesChanged() {
  if (typeof window === "undefined") {
    return;
  }
  window.dispatchEvent(new Event(FAVORITES_CHANGED_EVENT));
}

function isFavoriteWebtoon(value: unknown): value is FavoriteWebtoon {
  if (!value || typeof value !== "object") {
    return false;
  }

  const favorite = value as Partial<FavoriteWebtoon>;
  return (
    typeof favorite.id === "number" &&
    typeof favorite.title === "string" &&
    typeof favorite.author === "string" &&
    (typeof favorite.thumbnailUrl === "string" || favorite.thumbnailUrl === null) &&
    (typeof favorite.status === "string" || typeof favorite.status === "undefined") &&
    typeof favorite.statusName === "string" &&
    typeof favorite.originalUrl === "string" &&
    (Array.isArray(favorite.genres) || typeof favorite.genres === "undefined") &&
    typeof favorite.addedAt === "string"
  );
}

function isCodeName(value: unknown): value is CodeName {
  if (!value || typeof value !== "object") {
    return false;
  }
  const item = value as Partial<CodeName>;
  return typeof item.code === "string" && typeof item.name === "string";
}

function normalizeFavoriteWebtoon(item: FavoriteWebtoon): FavoriteWebtoon {
  return {
    ...item,
    genres: Array.isArray(item.genres) ? item.genres.filter(isCodeName) : [],
    weekdays: Array.isArray(item.weekdays) ? item.weekdays.filter(isCodeName) : []
  };
}

function normalizeFavorites(items: FavoriteWebtoon[]): FavoriteWebtoon[] {
  return items
    .filter(isFavoriteWebtoon)
    .map(normalizeFavoriteWebtoon)
    .sort((a, b) => Date.parse(b.addedAt) - Date.parse(a.addedAt));
}

export function readFavoriteWebtoons(): FavoriteWebtoon[] {
  if (typeof window === "undefined") {
    return [];
  }

  const raw = window.localStorage.getItem(FAVORITES_STORAGE_KEY);
  if (!raw) {
    return [];
  }

  try {
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) {
      return [];
    }

    return normalizeFavorites(parsed.filter(isFavoriteWebtoon));
  } catch {
    return [];
  }
}

export function isFavoriteWebtoonById(webtoonId: number): boolean {
  return readFavoriteWebtoons().some((item) => item.id === webtoonId);
}

function saveFavoriteWebtoons(items: FavoriteWebtoon[]) {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(FAVORITES_STORAGE_KEY, JSON.stringify(normalizeFavorites(items)));
  emitFavoritesChanged();
}

export function clearFavoriteWebtoons() {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.removeItem(FAVORITES_STORAGE_KEY);
  emitFavoritesChanged();
}

export function replaceFavoriteWebtoons(items: FavoriteWebtoon[]) {
  saveFavoriteWebtoons(items);
}

function toFavoriteWebtoon(webtoon: WebtoonCard): FavoriteWebtoon {
  return {
    id: webtoon.id,
    title: webtoon.title,
    author: webtoon.author || "작가 미상",
    thumbnailUrl: webtoon.thumbnailUrl,
    status: webtoon.status,
    statusName: webtoon.statusName,
    originalUrl: webtoon.originalUrl,
    genres: webtoon.genres,
    weekdays: webtoon.weekdays,
    addedAt: new Date().toISOString()
  };
}

function toAuthHeader(session: AuthSession): string {
  return `${session.tokenType} ${session.token}`;
}

async function requestFavoriteApi<T>(session: AuthSession, path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  headers.set("Authorization", toAuthHeader(session));

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers
  });

  let payload: ApiResponse<T> | null = null;
  try {
    payload = (await response.json()) as ApiResponse<T>;
  } catch {
    payload = null;
  }

  if (!response.ok || !payload?.success) {
    throw new FavoriteApiError(response.status, payload?.message ?? "즐겨찾기 요청에 실패했습니다.");
  }

  return payload.data;
}

export async function syncFavoriteWebtoonsFromServer(session: AuthSession): Promise<FavoriteWebtoon[]> {
  const favorites = await requestFavoriteApi<FavoriteWebtoon[]>(session, "/api/me/favorites");
  saveFavoriteWebtoons(favorites);
  return readFavoriteWebtoons();
}

export async function toggleFavoriteWebtoonRemote(session: AuthSession, webtoon: WebtoonCard) {
  const current = readFavoriteWebtoons();
  const alreadyFavorite = current.some((item) => item.id === webtoon.id);

  if (alreadyFavorite) {
    const next = current.filter((item) => item.id !== webtoon.id);
    saveFavoriteWebtoons(next);

    try {
      await requestFavoriteApi<void>(session, `/api/me/favorites/${webtoon.id}`, {
        method: "DELETE"
      });
      return { favorited: false, favorites: next };
    } catch (error) {
      saveFavoriteWebtoons(current);
      throw error;
    }
  }

  const optimistic = toFavoriteWebtoon(webtoon);
  const next = [optimistic, ...current];
  saveFavoriteWebtoons(next);

  try {
    const saved = await requestFavoriteApi<FavoriteWebtoon>(session, `/api/me/favorites/${webtoon.id}`, {
      method: "PUT"
    });
    const merged = [saved, ...next.filter((item) => item.id !== saved.id)];
    saveFavoriteWebtoons(merged);
    return { favorited: true, favorites: merged };
  } catch (error) {
    saveFavoriteWebtoons(current);
    throw error;
  }
}
