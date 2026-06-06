"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect } from "react";
import { saveAuthSession } from "@/lib/auth-client";

function AuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const token = searchParams.get("token");
    const tokenType = searchParams.get("tokenType");
    const username = searchParams.get("username");
    const nickname = searchParams.get("nickname") || username || "";
    const expiresAt = searchParams.get("expiresAt");
    const loginWeekday = searchParams.get("loginWeekday") || "MONDAY";
    const nextPathRaw = searchParams.get("next") || "/webtoons";
    const nextPath = nextPathRaw.startsWith("/") ? nextPathRaw : "/webtoons";

    if (!token || !tokenType || !username || !expiresAt) {
      router.replace("/login?oauthError=소셜%20로그인%20정보를%20확인하지%20못했습니다.");
      return;
    }

    saveAuthSession({
      token,
      tokenType,
      username,
      nickname,
      expiresAt,
      loginWeekday
    });
    router.replace(nextPath);
    router.refresh();
  }, [router, searchParams]);

  return (
    <section className="auth-page">
      <div className="auth-card reveal">
        <h1>회원가입 처리중</h1>
        <p className="auth-subtitle">소셜 로그인 정보를 확인하고 있습니다...</p>
      </div>
    </section>
  );
}

export default function AuthCallbackPage() {
  return (
    <Suspense
      fallback={
        <section className="auth-page">
          <div className="auth-card reveal">
            <h1>회원가입 처리중</h1>
            <p className="auth-subtitle">페이지를 준비 중입니다...</p>
          </div>
        </section>
      }
    >
      <AuthCallbackContent />
    </Suspense>
  );
}
