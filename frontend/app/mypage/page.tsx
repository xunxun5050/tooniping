"use client";

import Link from "next/link";
import { type FormEvent, useEffect, useMemo, useState } from "react";
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
import { ApiResponse, AuthMeResponse, FavoriteWebtoon, PagedResult, WebtoonCard } from "@/lib/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

type DashboardSnapshot = {
  username: string;
  nickname: string;
  totalWebtoons: number;
};

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

const ACCOUNT_ITEMS = [
  {
    title: "보안 설정",
    description: "비밀번호 변경, 세션 관리 등 보안 기능을 제공할 예정이에요.",
    status: "준비중"
  },
  {
    title: "로그인 기록",
    description: "최근 로그인 이력을 확인하고 비정상 접근을 점검해요.",
    status: "준비중"
  }
];

function formatFavoriteAddedAt(value: string): string {
  const timestamp = Date.parse(value);
  if (Number.isNaN(timestamp)) {
    return "-";
  }
  return new Date(timestamp).toLocaleString("ko-KR", { hour12: false });
}

function formatDateTime(value: string): string {
  const timestamp = Date.parse(value);
  if (Number.isNaN(timestamp)) {
    return "-";
  }
  return new Date(timestamp).toLocaleString("ko-KR", { hour12: false });
}

function formatRemainingMinutes(value: string): string {
  const timestamp = Date.parse(value);
  if (Number.isNaN(timestamp)) {
    return "-";
  }

  const diffMs = timestamp - Date.now();
  if (diffMs <= 0) {
    return "만료됨";
  }

  const remainingMinutes = Math.floor(diffMs / 1000 / 60);
  if (remainingMinutes < 60) {
    return `${remainingMinutes}분`;
  }

  const hours = Math.floor(remainingMinutes / 60);
  const minutes = remainingMinutes % 60;
  return `${hours}시간 ${minutes}분`;
}

export default function MyPage() {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [snapshot, setSnapshot] = useState<DashboardSnapshot | null>(null);
  const [favoriteWebtoons, setFavoriteWebtoons] = useState<FavoriteWebtoon[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [nicknameInput, setNicknameInput] = useState("");
  const [nicknameMessage, setNicknameMessage] = useState<string | null>(null);
  const [savingNickname, setSavingNickname] = useState(false);
  const [loading, setLoading] = useState(false);

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
      setSnapshot(null);
      return;
    }

    const currentSession = session;
    let active = true;
    async function fetchDashboard() {
      setLoading(true);
      setError(null);

      try {
        const authHeader = `${currentSession.tokenType} ${currentSession.token}`;
        const [meRes, webtoonRes] = await Promise.all([
          fetch(`${API_BASE_URL}/api/auth/me`, {
            headers: {
              Authorization: authHeader
            }
          }),
          fetch(`${API_BASE_URL}/api/admin/webtoons?page=0&size=1`, {
            headers: {
              Authorization: authHeader
            }
          })
        ]);

        const meJson = (await meRes.json()) as ApiResponse<AuthMeResponse>;
        const webtoonJson = (await webtoonRes.json()) as ApiResponse<PagedResult<WebtoonCard>>;

        if (!meRes.ok || !webtoonRes.ok || !meJson.success || !webtoonJson.success || !meJson.data || !webtoonJson.data) {
          if (active) {
            setError(meJson.message ?? webtoonJson.message ?? "대시보드 정보를 불러오지 못했습니다.");
          }
          return;
        }

        if (active) {
          setSnapshot({
            username: meJson.data.username,
            nickname: meJson.data.nickname,
            totalWebtoons: webtoonJson.data.totalElements
          });
          setNicknameInput(meJson.data.nickname);
        }
      } catch {
        if (active) {
          setError("대시보드 조회 중 오류가 발생했습니다.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    fetchDashboard();

    return () => {
      active = false;
    };
  }, [session]);

  const loginInfoRows = useMemo(() => {
    if (!session) {
      return [];
    }

    return [
      { label: "닉네임", value: snapshot?.nickname ?? session.nickname ?? session.username },
      { label: "로그인 계정", value: snapshot?.username ?? session.username },
      { label: "토큰 타입", value: session.tokenType },
      { label: "세션 만료 시각", value: formatDateTime(session.expiresAt) },
      { label: "남은 세션 시간", value: formatRemainingMinutes(session.expiresAt) },
      {
        label: "관리자 API 접근",
        value: snapshot ? "인증 완료" : loading ? "확인 중..." : "확인 필요"
      }
    ];
  }, [loading, session, snapshot]);

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
      setSnapshot((current) =>
        current
          ? {
              ...current,
              nickname: json.data.nickname
            }
          : current
      );
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
        <p className="description">계정 정보와 내 활동을 한곳에서 관리하세요.</p>
        <div className="mypage-actions">
          <Link className="more-link" href="/webtoons">
            웹툰 목록으로 이동
          </Link>
          <button
            className="auth-link-button"
            type="button"
            onClick={() => {
              clearAuthSession();
              setSnapshot(null);
            }}
          >
            로그아웃
          </button>
        </div>
      </div>

      {error ? <p className="auth-error">{error}</p> : null}

      <div className="mypage-grid">
        <section className="mypage-card reveal">
          <h2>로그인 정보</h2>
          <dl className="mypage-info-list">
            {loginInfoRows.map((row) => (
              <div key={row.label}>
                <dt>{row.label}</dt>
                <dd>{row.value}</dd>
              </div>
            ))}
          </dl>
          {snapshot ? <p className="description">전체 웹툰 데이터: {snapshot.totalWebtoons.toLocaleString()}개</p> : null}
        </section>

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
                      <p>추가일: {formatFavoriteAddedAt(item.addedAt)}</p>
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
          <ul className="mypage-list">
            {ACCOUNT_ITEMS.map((item) => (
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
      </div>
    </section>
  );
}
