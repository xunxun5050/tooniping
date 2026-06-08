"use client";

import Link from "next/link";
import { type FormEvent, useEffect, useState } from "react";
import {
  AUTH_SESSION_CHANGED_EVENT,
  AuthSession,
  clearAuthSession,
  readAuthSession,
  saveAuthSession
} from "@/lib/auth-client";
import {
  FAVORITES_CHANGED_EVENT,
  FavoriteApiError,
  clearFavoriteWebtoons,
  readFavoriteWebtoons,
  syncFavoriteWebtoonsFromServer
} from "@/lib/favorites-client";
import {
  EvaluationApiError,
  WEBTOON_EMOTION_TAG_META,
  WEBTOON_RATING_META,
  getWebtoonEvaluations
} from "@/lib/evaluations-client";
import { ApiResponse, AuthMeResponse, FavoriteWebtoon, WebtoonEvaluation } from "@/lib/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

type UserProfile = {
  username: string;
  nickname: string;
  provider: string | null;
  createdAt: string;
  updatedAt: string;
};

const MY_ACTIVITY_ITEMS = [
  {
    title: "최근 본 웹툰",
    description: "최근 본 작품 목록과 이어보기를 한눈에 확인해요.",
    status: "준비중"
  },
  {
    title: "관심 작품(북마크)",
    description: "찜한 작품을 모아보고 업데이트를 빠르게 확인해요.",
    status: "준비중"
  },
  {
    title: "알림 설정",
    description: "신작/업데이트 알림, 관심 장르 알림을 설정해요.",
    status: "준비중"
  },
  {
    title: "내 취향 장르",
    description: "선호 장르를 관리하고 맞춤 추천 정확도를 높여요.",
    status: "준비중"
  }
];

function formatDateTime(value: string): string {
  const timestamp = Date.parse(value);
  if (Number.isNaN(timestamp)) {
    return "-";
  }
  return new Date(timestamp).toLocaleString("ko-KR", { hour12: false });
}

export default function MyPage() {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [favoriteWebtoons, setFavoriteWebtoons] = useState<FavoriteWebtoon[]>([]);
  const [evaluations, setEvaluations] = useState<WebtoonEvaluation[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [nicknameInput, setNicknameInput] = useState("");
  const [nicknameMessage, setNicknameMessage] = useState<string | null>(null);
  const [savingNickname, setSavingNickname] = useState(false);

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
    function syncFavorites() {
      setFavoriteWebtoons(readFavoriteWebtoons());
    }

    syncFavorites();
    window.addEventListener("storage", syncFavorites);
    window.addEventListener(FAVORITES_CHANGED_EVENT, syncFavorites as EventListener);

    return () => {
      window.removeEventListener("storage", syncFavorites);
      window.removeEventListener(FAVORITES_CHANGED_EVENT, syncFavorites as EventListener);
    };
  }, []);

  useEffect(() => {
    if (!session) {
      setFavoriteWebtoons([]);
      setEvaluations([]);
      clearFavoriteWebtoons();
      return;
    }

    const currentSession = session;
    let active = true;

    async function loadFavoritesFromServer() {
      try {
        const favorites = await syncFavoriteWebtoonsFromServer(currentSession);
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
          setError("로그인이 만료되었습니다. 다시 로그인해 주세요.");
          return;
        }

        setError("즐겨찾기 목록을 불러오지 못했습니다.");
      }
    }

    loadFavoritesFromServer();

    return () => {
      active = false;
    };
  }, [session]);

  useEffect(() => {
    if (!session) {
      setEvaluations([]);
      return;
    }

    const currentSession = session;
    let active = true;

    async function loadEvaluationsFromServer() {
      try {
        const savedEvaluations = await getWebtoonEvaluations(currentSession);
        if (active) {
          setEvaluations(savedEvaluations);
        }
      } catch (error) {
        if (!active) {
          return;
        }

        if (error instanceof EvaluationApiError && error.status === 401) {
          clearAuthSession();
          clearFavoriteWebtoons();
          setEvaluations([]);
          setError("로그인이 만료되었습니다. 다시 로그인해 주세요.");
          return;
        }

        setError("웹툰 평가 목록을 불러오지 못했습니다.");
      }
    }

    loadEvaluationsFromServer();

    return () => {
      active = false;
    };
  }, [session]);

  useEffect(() => {
    if (!session) {
      setNicknameInput("");
      return;
    }

    const currentSession = session;
    let active = true;
    async function fetchDashboard() {
      setError(null);

      try {
        const authHeader = `${currentSession.tokenType} ${currentSession.token}`;
        const meRes = await fetch(`${API_BASE_URL}/api/auth/me`, {
          headers: {
            Authorization: authHeader
          }
        });

        const meJson = (await meRes.json()) as ApiResponse<AuthMeResponse>;

        if (!meRes.ok || !meJson.success || !meJson.data) {
          if (active) {
            setError(meJson.message ?? "프로필 정보를 불러오지 못했습니다.");
          }
          return;
        }

        if (active) {
          setNicknameInput(meJson.data.nickname);
        }
      } catch {
        if (active) {
          setError("프로필 조회 중 오류가 발생했습니다.");
        }
      }
    }

    fetchDashboard();

    return () => {
      active = false;
    };
  }, [session]);

  async function handleNicknameSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session || savingNickname) {
      return;
    }

    setSavingNickname(true);
    setNicknameMessage(null);
    setError(null);

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/me/nickname`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: `${session.tokenType} ${session.token}`
        },
        body: JSON.stringify({ nickname: nicknameInput })
      });
      const json = (await response.json()) as ApiResponse<UserProfile>;

      if (!response.ok || !json.success || !json.data) {
        if (response.status === 401) {
          clearAuthSession();
          clearFavoriteWebtoons();
          setSession(null);
          setError("로그인이 만료되었습니다. 다시 로그인해 주세요.");
          return;
        }
        setError(json.message ?? "닉네임을 저장하지 못했습니다.");
        return;
      }

      const nextSession = {
        ...session,
        nickname: json.data.nickname
      };
      saveAuthSession(nextSession);
      setSession(nextSession);
      setNicknameInput(json.data.nickname);
      setNicknameMessage("닉네임을 저장했습니다.");
    } catch {
      setError("닉네임 저장 중 오류가 발생했습니다.");
    } finally {
      setSavingNickname(false);
    }
  }

  if (!session) {
    return (
      <section className="quick-nav reveal">
        <h1>마이페이지</h1>
        <p className="description">로그인 후 대시보드를 확인할 수 있습니다.</p>
        <Link className="more-link" href="/login?next=%2Fmypage">
          로그인하러 가기
        </Link>
      </section>
    );
  }

  return (
    <section className="mypage">
      <div className="mypage-header quick-nav reveal">
        <h1>마이페이지</h1>
        <p className="description">내 활동과 즐겨찾기 작품을 한곳에서 관리하세요.</p>
        <div className="mypage-actions">
          <Link className="more-link" href="/webtoons">
            웹툰 목록으로 이동
          </Link>
          <button
            className="auth-link-button"
            type="button"
            onClick={() => {
              clearAuthSession();
            }}
          >
            로그아웃
          </button>
        </div>
      </div>

      {error ? <p className="auth-error">{error}</p> : null}

      <div className="mypage-grid">
        <section className="mypage-card reveal">
          <h2>프로필 관리</h2>
          <form className="nickname-form" onSubmit={handleNicknameSubmit}>
            <label htmlFor="nickname">닉네임</label>
            <div className="nickname-row">
              <input
                id="nickname"
                type="text"
                value={nicknameInput}
                onChange={(event) => {
                  setNicknameInput(event.target.value);
                  setNicknameMessage(null);
                }}
                minLength={2}
                maxLength={24}
                autoComplete="nickname"
              />
              <button type="submit" disabled={savingNickname}>
                {savingNickname ? "저장 중..." : "저장"}
              </button>
            </div>
            <p className="description">처음 가입하면 단어 조합 닉네임이 자동으로 만들어져요.</p>
            {nicknameMessage ? <p className="nickname-message">{nicknameMessage}</p> : null}
          </form>
        </section>

        <section className="mypage-card reveal">
          <div className="mypage-card-head">
            <h2>내 즐겨찾기 작품</h2>
            <strong>{favoriteWebtoons.length}개</strong>
          </div>
          {favoriteWebtoons.length === 0 ? (
            <p className="description">웹툰 카드의 하트 버튼을 눌러 즐겨찾기를 추가해 보세요.</p>
          ) : (
            <ul className="mypage-favorite-list">
              {favoriteWebtoons.map((item) => (
                <li key={item.id}>
                  <Link href={`/webtoons/${item.id}`} className="mypage-favorite-link">
                    <div className="mypage-favorite-thumb-wrap">
                      {item.thumbnailUrl ? (
                        <img className="mypage-favorite-thumb" src={item.thumbnailUrl} alt={item.title} />
                      ) : (
                        <div className="mypage-favorite-thumb placeholder">NO IMAGE</div>
                      )}
                    </div>
                    <div className="mypage-favorite-meta">
                      <strong>{item.title}</strong>
                      <p>{item.author}</p>
                      <p>{item.statusName}</p>
                      <p>추가일: {formatDateTime(item.addedAt)}</p>
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="mypage-card reveal">
          <div className="mypage-card-head">
            <h2>내 웹툰 평가</h2>
            <strong>{evaluations.length}개</strong>
          </div>
          {evaluations.length === 0 ? (
            <p className="description">상세 페이지에서 레이팅과 감정 태그를 남겨 보세요.</p>
          ) : (
            <ul className="mypage-evaluation-list">
              {evaluations.map((item) => (
                <li key={item.webtoonId}>
                  <Link href={`/webtoons/${item.webtoonId}`} className="mypage-evaluation-link">
                    <div className="mypage-favorite-thumb-wrap">
                      {item.thumbnailUrl ? (
                        <img className="mypage-favorite-thumb" src={item.thumbnailUrl} alt={item.title} />
                      ) : (
                        <div className="mypage-favorite-thumb placeholder">NO IMAGE</div>
                      )}
                    </div>
                    <div className="mypage-evaluation-meta">
                      <div className="mypage-evaluation-title-row">
                        <strong>{item.title}</strong>
                        <span className={`mypage-rating-badge rating-${WEBTOON_RATING_META[item.rating].tone}`}>
                          <span aria-hidden="true">{WEBTOON_RATING_META[item.rating].icon}</span>
                          {item.rating}
                        </span>
                      </div>
                      <p>{item.author}</p>
                      <div className="mypage-evaluation-tags" aria-label={`${item.title} 감정 태그`}>
                        {item.emotionTags.map((tag) => {
                          const meta = WEBTOON_EMOTION_TAG_META[tag] ?? { emoji: "•", label: tag };
                          return (
                            <span key={tag}>
                              <span aria-hidden="true">{meta.emoji}</span>
                              {meta.label}
                            </span>
                          );
                        })}
                      </div>
                      <p>최근 수정: {formatDateTime(item.updatedAt)}</p>
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="mypage-card reveal">
          <h2>내 활동</h2>
          <ul className="mypage-list">
            {MY_ACTIVITY_ITEMS.map((item) => (
              <li key={item.title}>
                <div>
                  <strong>{item.title}</strong>
                  <p>{item.description}</p>
                </div>
                <span>{item.status}</span>
              </li>
            ))}
          </ul>
        </section>

        <section className="mypage-card reveal">
          <h2>계정 관리</h2>
          <div className="withdrawal-entry">
            <div>
              <strong>회원 탈퇴</strong>
              <p className="description">프로필, 즐겨찾기, 웹툰 평가는 별도 확인 페이지에서 삭제합니다.</p>
            </div>
            <Link className="withdrawal-link" href="/mypage/withdrawal">
              탈퇴 페이지로 이동
            </Link>
          </div>
        </section>
      </div>
    </section>
  );
}
