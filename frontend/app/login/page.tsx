"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { FormEvent, Suspense, useEffect, useMemo, useState } from "react";
import { saveAuthSession, readAuthSession } from "@/lib/auth-client";
import { ApiResponse, AuthLoginResponse } from "@/lib/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

function LoginPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const nextPath = useMemo(() => {
    const raw = searchParams.get("next") || "/webtoons";
    return raw.startsWith("/") ? raw : "/webtoons";
  }, [searchParams]);
  const oauthErrorMessage = searchParams.get("oauthError");
  const kakaoSignupUrl = `${API_BASE_URL}/api/auth/oauth/kakao/start?next=${encodeURIComponent(nextPath)}`;
  const naverSignupUrl = `${API_BASE_URL}/api/auth/oauth/naver/start?next=${encodeURIComponent(nextPath)}`;

  useEffect(() => {
    const session = readAuthSession();
    if (session) {
      router.replace(nextPath);
    }
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
        body: JSON.stringify({
          username,
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
        <p className="auth-subtitle">관리자 API를 사용하려면 로그인해 주세요.</p>
        {oauthErrorMessage ? <p className="auth-error">{oauthErrorMessage}</p> : null}

        <form className="auth-form" onSubmit={handleSubmit}>
          <label htmlFor="username">아이디</label>
          <input
            id="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            placeholder="admin"
            autoComplete="username"
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
          <p>처음이신가요? 소셜 계정으로 회원가입/로그인</p>
          <a className="social-signup kakao" href={kakaoSignupUrl}>
            카카오로 시작하기
          </a>
          <a className="social-signup naver" href={naverSignupUrl}>
            네이버로 시작하기
          </a>
        </div>

        <div className="auth-help">
          <p>
            기본 계정: <code>admin</code> / <code>admin1234</code>
          </p>
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
