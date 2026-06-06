"use client";

import { AuthLoginResponse } from "@/lib/types";

export type AuthSession = AuthLoginResponse;

const AUTH_STORAGE_KEY = "webtoon_hub_auth_session";
export const AUTH_SESSION_CHANGED_EVENT = "auth-session-changed";
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
