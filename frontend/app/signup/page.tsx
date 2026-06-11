"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { type FormEvent, Suspense, useEffect, useMemo, useState } from "react";
import { getAuthSession, saveAuthSession } from "@/lib/auth-client";
import { ApiResponse, AuthLoginResponse, EmailVerificationResponse } from "@/lib/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

function SignupPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");
  const [emailCode, setEmailCode] = useState("");
  const [emailVerificationSent, setEmailVerificationSent] = useState(false);
  const [emailVerified, setEmailVerified] = useState(false);
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [isVerifyingCode, setIsVerifyingCode] = useState(false);
  const [verificationMessage, setVerificationMessage] = useState<string | null>(null);
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const nextPath = useMemo(() => {
    const raw = searchParams.get("next") || "/webtoons";
    return raw.startsWith("/") ? raw : "/webtoons";
  }, [searchParams]);
  const kakaoSignupUrl = `${API_BASE_URL}/api/auth/oauth/kakao/start?next=${encodeURIComponent(nextPath)}`;

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

  function handleEmailChange(value: string) {
    setEmail(value);
    setEmailCode("");
    setEmailVerificationSent(false);
    setEmailVerified(false);
    setVerificationMessage(null);
  }

  async function handleSendEmailCode() {
    setError(null);
    setVerificationMessage(null);
    setEmailVerified(false);

    if (!email.trim()) {
      setError("이메일을 입력해 주세요.");
      return;
    }

    setIsSendingCode(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/signup/email-code`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ email })
      });
      const payload = (await response.json()) as ApiResponse<EmailVerificationResponse>;

      if (!response.ok || !payload.success || !payload.data) {
        setError(payload.message ?? "인증번호 발송에 실패했습니다.");
        return;
      }

      setEmailVerificationSent(true);
      setEmailCode("");
      setVerificationMessage("인증번호를 보냈습니다. 메일함에서 6자리 코드를 확인해 주세요.");
    } catch {
      setError("인증번호 발송 중 오류가 발생했습니다.");
    } finally {
      setIsSendingCode(false);
    }
  }

  async function handleVerifyEmailCode() {
    setError(null);
    setVerificationMessage(null);

    if (!emailVerificationSent) {
      setError("인증번호를 먼저 요청해 주세요.");
      return;
    }
    if (!/^[0-9]{6}$/.test(emailCode)) {
      setError("인증번호 6자리를 입력해 주세요.");
      return;
    }

    setIsVerifyingCode(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/signup/email-code/verify`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ email, code: emailCode })
      });
      const payload = (await response.json()) as ApiResponse<EmailVerificationResponse>;

      if (!response.ok || !payload.success || !payload.data?.verified) {
        setError(payload.message ?? "이메일 인증에 실패했습니다.");
        return;
      }

      setEmailVerified(true);
      setVerificationMessage("이메일 인증이 완료되었습니다.");
    } catch {
      setError("인증번호 확인 중 오류가 발생했습니다.");
    } finally {
      setIsVerifyingCode(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (!emailVerified) {
      setError("이메일 인증을 완료해 주세요.");
      return;
    }
    if (password !== passwordConfirm) {
      setError("비밀번호가 서로 일치하지 않습니다.");
      return;
    }
    if (!/[A-Za-z]/.test(password) || !/[0-9]/.test(password)) {
      setError("비밀번호는 영문과 숫자를 함께 입력해 주세요.");
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/signup`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        credentials: "include",
        body: JSON.stringify({
          email,
          password,
          nickname
        })
      });
      const payload = (await response.json()) as ApiResponse<AuthLoginResponse>;

      if (!response.ok || !payload.success || !payload.data) {
        setError(payload.message ?? "회원가입에 실패했습니다.");
        return;
      }

      saveAuthSession(payload.data);
      router.push(nextPath);
      router.refresh();
    } catch {
      setError("회원가입 요청 중 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-card reveal">
        <h1>회원가입</h1>
        <p className="auth-subtitle">좋아하는 웹툰을 저장할 계정을 만들어요.</p>

        <form className="auth-form" onSubmit={handleSubmit}>
          <label htmlFor="signup-email">이메일</label>
          <div className="auth-inline-field">
            <input
              id="signup-email"
              type="email"
              value={email}
              onChange={(event) => handleEmailChange(event.target.value)}
              placeholder="you@example.com"
              autoComplete="email"
              maxLength={100}
              disabled={isSendingCode || isVerifyingCode || emailVerified}
              required
            />
            <button
              className="auth-inline-button"
              type="button"
              onClick={handleSendEmailCode}
              disabled={isSendingCode || isVerifyingCode || emailVerified}
            >
              {isSendingCode ? "발송 중" : emailVerified ? "인증완료" : "인증번호 받기"}
            </button>
          </div>

          {emailVerificationSent ? (
            <>
              <label htmlFor="signup-email-code">인증번호</label>
              <div className="auth-inline-field code">
                <input
                  id="signup-email-code"
                  value={emailCode}
                  onChange={(event) => setEmailCode(event.target.value.replace(/\D/g, "").slice(0, 6))}
                  placeholder="6자리 코드"
                  inputMode="numeric"
                  pattern="[0-9]{6}"
                  maxLength={6}
                  disabled={isVerifyingCode || emailVerified}
                  required
                />
                <button
                  className="auth-inline-button"
                  type="button"
                  onClick={handleVerifyEmailCode}
                  disabled={isVerifyingCode || emailVerified}
                >
                  {isVerifyingCode ? "확인 중" : emailVerified ? "확인완료" : "인증번호 확인"}
                </button>
              </div>
            </>
          ) : null}

          {verificationMessage ? (
            <p className={`auth-info ${emailVerified ? "success" : ""}`}>{verificationMessage}</p>
          ) : null}

          <label htmlFor="signup-nickname">닉네임</label>
          <input
            id="signup-nickname"
            value={nickname}
            onChange={(event) => setNickname(event.target.value)}
            placeholder="2~20자"
            autoComplete="nickname"
            minLength={2}
            maxLength={20}
            required
          />

          <label htmlFor="signup-password">비밀번호</label>
          <input
            id="signup-password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="영문+숫자 8자 이상"
            autoComplete="new-password"
            minLength={8}
            maxLength={72}
            required
          />

          <label htmlFor="signup-password-confirm">비밀번호 확인</label>
          <input
            id="signup-password-confirm"
            type="password"
            value={passwordConfirm}
            onChange={(event) => setPasswordConfirm(event.target.value)}
            placeholder="비밀번호 다시 입력"
            autoComplete="new-password"
            minLength={8}
            maxLength={72}
            required
          />

          {error ? <p className="auth-error">{error}</p> : null}

          <button type="submit" disabled={isSubmitting || !emailVerified}>
            {isSubmitting ? "가입 중..." : "회원가입"}
          </button>
        </form>

        <div className="auth-signup">
          <p>이미 계정이 있으신가요?</p>
          <Link className="auth-secondary-link" href={`/login?next=${encodeURIComponent(nextPath)}`}>
            로그인하기
          </Link>
        </div>

        <div className="auth-social-section" aria-label="소셜 회원가입">
          <div className="auth-divider">
            <span>또는</span>
          </div>
          <div className="auth-social-grid">
            <a className="auth-social-button kakao" href={kakaoSignupUrl} aria-label="카카오로 회원가입">
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

export default function SignupPage() {
  return (
    <Suspense
      fallback={
        <section className="auth-page">
          <div className="auth-card reveal">
            <h1>회원가입</h1>
            <p className="auth-subtitle">페이지를 준비 중입니다...</p>
          </div>
        </section>
      }
    >
      <SignupPageContent />
    </Suspense>
  );
}
