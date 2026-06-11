"use client";

import { AuthLoginResponse } from "@/lib/types";

export type AuthSession = AuthLoginResponse;

const AUTH_STORAGE_KEY = "webtoon_hub_auth_session";
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
export const AUTH_SESSION_CHANGED_EVENT = "auth-session-changed";
const DEFAULT_AVATAR_PALETTE = "MINT";
const WEEKDAY_BY_ENGLISH_NAME: Record<string, string> = {
  Monday: "MONDAY",
  Tuesday: "TUESDAY",
  Wednesday: "WEDNESDAY",
  Thursday: "THURSDAY",
  Friday: "FRIDAY",
  Saturday: "SATURDAY",
  Sunday: "SUNDAY"
};

export function getTodayWeekdayCodeInSeoul(): string {
  const todayName = new Intl.DateTimeFormat("en-US", {
    weekday: "long",
    timeZone: "Asia/Seoul"
  }).format(new Date());

  return WEEKDAY_BY_ENGLISH_NAME[todayName] ?? "MONDAY";
}

function normalizeWeekdayCode(value: unknown): string {
  if (typeof value !== "string" || !value.trim()) {
    return getTodayWeekdayCodeInSeoul();
  }
  return value;
}

function normalizeAvatarSeed(value: unknown, username: string): string {
  if (typeof value === "string" && value.trim()) {
    return value.trim();
  }
  return `avatar-${username}`;
}

function normalizeAvatarPalette(value: unknown): string {
  if (typeof value === "string" && value.trim()) {
    return value.trim();
  }
  return DEFAULT_AVATAR_PALETTE;
}

function emitAuthSessionChanged() {
  if (typeof window === "undefined") {
    return;
  }
  window.dispatchEvent(new Event(AUTH_SESSION_CHANGED_EVENT));
}

export function readAuthSession(): AuthSession | null {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as Partial<AuthSession>;
    if (!parsed.token || !parsed.tokenType || !parsed.username || !parsed.expiresAt) {
      clearAuthSession();
      return null;
    }

    const expiresAt = Date.parse(parsed.expiresAt);
    if (Number.isNaN(expiresAt) || expiresAt <= Date.now()) {
      clearAuthSession();
      return null;
    }

    return {
      token: parsed.token,
      tokenType: parsed.tokenType,
      username: parsed.username,
      nickname: typeof parsed.nickname === "string" && parsed.nickname.trim() ? parsed.nickname : parsed.username,
      avatarSeed: normalizeAvatarSeed(parsed.avatarSeed, parsed.username),
      avatarPalette: normalizeAvatarPalette(parsed.avatarPalette),
      expiresAt: parsed.expiresAt,
      loginWeekday: normalizeWeekdayCode(parsed.loginWeekday)
    };
  } catch {
    clearAuthSession();
    return null;
  }
}

export function saveAuthSession(data: AuthLoginResponse) {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.setItem(
    AUTH_STORAGE_KEY,
    JSON.stringify({
      ...data,
      avatarSeed: normalizeAvatarSeed(data.avatarSeed, data.username),
      avatarPalette: normalizeAvatarPalette(data.avatarPalette),
      loginWeekday: normalizeWeekdayCode(data.loginWeekday)
    })
  );
  emitAuthSessionChanged();
}

export function clearAuthSession() {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.removeItem(AUTH_STORAGE_KEY);
  emitAuthSessionChanged();
}

export async function refreshAuthSession(): Promise<AuthSession | null> {
  if (typeof window === "undefined") {
    return null;
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
      method: "POST",
      credentials: "include"
    });
    const payload = (await response.json()) as { success: boolean; data: AuthLoginResponse | null; message: string | null };
    if (!response.ok || !payload.success || !payload.data) {
      clearAuthSession();
      return null;
    }
    saveAuthSession(payload.data);
    return readAuthSession();
  } catch {
    clearAuthSession();
    return null;
  }
}

export async function getAuthSession(): Promise<AuthSession | null> {
  const session = readAuthSession();
  if (session) {
    return session;
  }
  return refreshAuthSession();
}

export async function logoutAuthSession() {
  try {
    await fetch(`${API_BASE_URL}/api/auth/logout`, {
      method: "POST",
      credentials: "include"
    });
  } catch {
    // Local cleanup still happens even if the network request fails.
  } finally {
    clearAuthSession();
  }
}
