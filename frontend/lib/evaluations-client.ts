"use client";

import { AuthSession } from "@/lib/auth-client";
import { ApiResponse, WebtoonEvaluation, WebtoonRating } from "@/lib/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export const WEBTOON_RATINGS = ["SSS", "S", "A", "B", "C", "D", "F"] as const;
export const WEBTOON_EMOTION_TAGS = ["웃김", "울림", "설렘", "소름", "충격", "힐링", "도파민", "여운"] as const;

export const WEBTOON_RATING_META: Record<WebtoonRating, { icon: string; label: string; tone: string }> = {
  SSS: { icon: "👑", label: "최상위", tone: "legendary" },
  S: { icon: "💎", label: "명작", tone: "diamond" },
  A: { icon: "⭐", label: "강추", tone: "star" },
  B: { icon: "🔥", label: "재밌음", tone: "fire" },
  C: { icon: "🌿", label: "무난함", tone: "leaf" },
  D: { icon: "☁️", label: "아쉬움", tone: "cloud" },
  F: { icon: "🧊", label: "보류", tone: "ice" }
};

export const WEBTOON_EMOTION_TAG_META: Record<string, { emoji: string; label: string }> = {
  웃김: { emoji: "😂", label: "웃김" },
  울림: { emoji: "🥹", label: "울림" },
  설렘: { emoji: "💗", label: "설렘" },
  소름: { emoji: "⚡", label: "소름" },
  충격: { emoji: "💥", label: "충격" },
  힐링: { emoji: "🌿", label: "힐링" },
  도파민: { emoji: "🚀", label: "도파민" },
  여운: { emoji: "🌙", label: "여운" }
};

export class EvaluationApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "EvaluationApiError";
    this.status = status;
  }
}

function toAuthHeader(session: AuthSession): string {
  return `${session.tokenType} ${session.token}`;
}

async function requestEvaluationApi<T>(session: AuthSession, path: string, init?: RequestInit): Promise<T> {
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
    throw new EvaluationApiError(response.status, payload?.message ?? "웹툰 평가 요청에 실패했습니다.");
  }

  return payload.data;
}

export function getWebtoonEvaluation(session: AuthSession, webtoonId: number): Promise<WebtoonEvaluation | null> {
  return requestEvaluationApi<WebtoonEvaluation | null>(session, `/api/me/evaluations/${webtoonId}`);
}

export function getWebtoonEvaluations(session: AuthSession): Promise<WebtoonEvaluation[]> {
  return requestEvaluationApi<WebtoonEvaluation[]>(session, "/api/me/evaluations");
}

export function saveWebtoonEvaluation(
  session: AuthSession,
  webtoonId: number,
  rating: WebtoonRating,
  emotionTags: string[]
): Promise<WebtoonEvaluation> {
  return requestEvaluationApi<WebtoonEvaluation>(session, `/api/me/evaluations/${webtoonId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ rating, emotionTags })
  });
}

export function deleteWebtoonEvaluation(session: AuthSession, webtoonId: number): Promise<void> {
  return requestEvaluationApi<void>(session, `/api/me/evaluations/${webtoonId}`, {
    method: "DELETE"
  });
}
