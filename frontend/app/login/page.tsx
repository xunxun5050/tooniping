"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { FormEvent, Suspense, useEffect, useMemo, useState } from "react";
import { getAuthSession, saveAuthSession } from "@/lib/auth-client";
import { ApiResponse, AuthLoginResponse } from "@/lib/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

function LoginPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const nextPath = useMemo(() => {
    const raw = searchParams.get("next") || "/webtoons";
    return raw.startsWith("/") ? raw : "/webtoons";
  }, [searchParams]);
  const oauthErrorMessage = searchParams.get("oauthError");
  const kakaoLoginUrl = `${API_BASE_URL}/api/auth/oauth/kakao/start?next=${encodeURIComponent(nextPath)}`;

  useEffect(() => {
    let active = true;

    async function redirectIfLoggedIn() {
      const session = await getAuthSession();
      if (active && session) {
        router.replace(nextPath);
      }
    }

    redirectIfLoggedIn();

    return () => {
      active = false;
    };
  }, [nextPath, router]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setError(null);

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        credentials: "include",
        body: JSON.stringify({
          email,
          password
        })
      });

      const payload = (await response.json()) as ApiResponse<AuthLoginResponse>;
      if (!response.ok || !payload.success || !payload.data) {
        setError(payload.message ?? "로그인에 실패했습니다.");
        return;
      }

      saveAuthSession(payload.data);
      router.push(nextPath);
      router.refresh();
    } catch {
      setError("로그인 요청 중 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-card reveal">
        <h1>로그인</h1>
        {oauthErrorMessage ? <p className="auth-error">{oauthErrorMessage}</p> : null}

        <form className="auth-form" onSubmit={handleSubmit}>
          <label htmlFor="email">이메일</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="you@example.com"
            autoComplete="email"
            maxLength={100}
            required
          />

          <label htmlFor="password">비밀번호</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="비밀번호"
            autoComplete="current-password"
            required
          />

          {error ? <p className="auth-error">{error}</p> : null}

          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? "로그인 중..." : "로그인"}
          </button>
        </form>

        <div className="auth-signup">
          <p>처음이신가요?</p>
          <Link className="auth-secondary-link" href={`/signup?next=${encodeURIComponent(nextPath)}`}>
            회원가입
          </Link>
        </div>

        <div className="auth-social-section" aria-label="간편 로그인">
          <div className="auth-divider">
            <span>간편 로그인</span>
          </div>
          <div className="auth-social-grid">
            <a className="auth-social-button kakao" href={kakaoLoginUrl} aria-label="카카오로 로그인">
              <span className="kakao-bubble" aria-hidden="true" />
            </a>
          </div>
        </div>

        <div className="auth-help">
          <Link href="/">메인으로 돌아가기</Link>
        </div>
      </div>
    </section>
  );
}

export default function LoginPage() {
  return (
    <Suspense
      fallback={
        <section className="auth-page">
          <div className="auth-card reveal">
            <h1>로그인</h1>
            <p className="auth-subtitle">페이지를 준비 중입니다...</p>
          </div>
        </section>
      }
    >
      <LoginPageContent />
    </Suspense>
  );
}
