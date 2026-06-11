"use client";

import Link from "next/link";
import { type FormEvent, useEffect, useState } from "react";
import {
  AUTH_SESSION_CHANGED_EVENT,
  AuthSession,
  clearAuthSession,
  getAuthSession,
  readAuthSession
} from "@/lib/auth-client";
import { clearFavoriteWebtoons } from "@/lib/favorites-client";
import { ApiResponse } from "@/lib/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export default function WithdrawalPage() {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [confirmInput, setConfirmInput] = useState("");
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isComplete, setIsComplete] = useState(false);

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

  async function handleDeleteAccountSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session || isDeleting || confirmInput !== "탈퇴") {
      return;
    }

    setIsDeleting(true);
    setError(null);

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
        method: "DELETE",
        credentials: "include",
        headers: {
          Authorization: `${session.tokenType} ${session.token}`
        }
      });
      const json = (await response.json()) as ApiResponse<void>;

      if (!response.ok || !json.success) {
        if (response.status === 401) {
          clearAuthSession();
          clearFavoriteWebtoons();
          setSession(null);
          setError("로그인이 만료되었습니다. 다시 로그인해 주세요.");
          return;
        }
        setError(json.message ?? "회원 탈퇴를 처리하지 못했습니다.");
        return;
      }

      clearFavoriteWebtoons();
      clearAuthSession();
      setSession(null);
      setConfirmInput("");
      setIsComplete(true);
    } catch {
      setError("회원 탈퇴 처리 중 오류가 발생했습니다.");
    } finally {
      setIsDeleting(false);
    }
  }

  if (isComplete) {
    return (
      <section className="withdrawal-page">
        <div className="withdrawal-panel reveal">
          <p className="eyebrow">ACCOUNT</p>
          <h1>회원 탈퇴 완료</h1>
          <p className="description">계정 정보와 저장한 즐겨찾기, 웹툰 평가를 정리했습니다.</p>
          <Link className="source-btn" href="/">
            메인으로 이동
          </Link>
        </div>
      </section>
    );
  }

  if (!session) {
    return (
      <section className="withdrawal-page">
        <div className="withdrawal-panel reveal">
          <p className="eyebrow">ACCOUNT</p>
          <h1>회원 탈퇴</h1>
          {error ? <p className="auth-error">{error}</p> : null}
          <p className="description">로그인 후 회원 탈퇴를 진행할 수 있습니다.</p>
          <Link className="source-btn" href="/login?next=%2Fmypage%2Fwithdrawal">
            로그인하기
          </Link>
        </div>
      </section>
    );
  }

  return (
    <section className="withdrawal-page">
      <div className="withdrawal-panel reveal">
        <Link className="back-link" href="/mypage">
          마이페이지로 돌아가기
        </Link>
        <p className="eyebrow">ACCOUNT</p>
        <h1>회원 탈퇴</h1>
        <p className="description">탈퇴하면 프로필, 즐겨찾기, 웹툰 평가가 삭제되고 현재 세션에서 로그아웃됩니다.</p>

        <ul className="withdrawal-warning-list">
          <li>즐겨찾기한 웹툰 목록이 삭제됩니다.</li>
          <li>상세 페이지에서 남긴 웹툰 평가가 삭제됩니다.</li>
          <li>자동 생성 또는 수정한 닉네임 정보가 삭제됩니다.</li>
          <li>탈퇴 후 같은 소셜 계정으로 다시 로그인하면 새 프로필이 생성됩니다.</li>
        </ul>

        <form className="withdrawal-form" onSubmit={handleDeleteAccountSubmit}>
          <label htmlFor="withdrawal-confirm">계속하려면 아래에 탈퇴라고 입력하세요.</label>
          <div className="withdrawal-row">
            <input
              id="withdrawal-confirm"
              type="text"
              value={confirmInput}
              onChange={(event) => setConfirmInput(event.target.value.trim())}
              placeholder="탈퇴"
              autoComplete="off"
            />
            <button type="submit" disabled={isDeleting || confirmInput !== "탈퇴"}>
              {isDeleting ? "처리 중..." : "회원 탈퇴"}
            </button>
          </div>
          {error ? <p className="auth-error">{error}</p> : null}
        </form>
      </div>
    </section>
  );
}
