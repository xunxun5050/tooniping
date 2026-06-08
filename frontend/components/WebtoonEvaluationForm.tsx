"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { type FormEvent, useEffect, useState } from "react";
import { AUTH_SESSION_CHANGED_EVENT, AuthSession, clearAuthSession, readAuthSession } from "@/lib/auth-client";
import {
  EvaluationApiError,
  WEBTOON_EMOTION_TAG_META,
  WEBTOON_EMOTION_TAGS,
  WEBTOON_RATING_META,
  WEBTOON_RATINGS,
  deleteWebtoonEvaluation,
  getWebtoonEvaluation,
  saveWebtoonEvaluation
} from "@/lib/evaluations-client";
import { WebtoonCard, WebtoonEvaluation, WebtoonRating } from "@/lib/types";

type Props = {
  webtoon: WebtoonCard;
};

export function WebtoonEvaluationForm({ webtoon }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const [session, setSession] = useState<AuthSession | null>(null);
  const [evaluation, setEvaluation] = useState<WebtoonEvaluation | null>(null);
  const [rating, setRating] = useState<WebtoonRating | null>(null);
  const [emotionTags, setEmotionTags] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

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
    if (!session) {
      setEvaluation(null);
      setRating(null);
      setEmotionTags([]);
      setIsLoading(false);
      return;
    }

    const currentSession = session;
    let active = true;
    setIsLoading(true);
    setError(null);
    setMessage(null);

    async function loadEvaluation() {
      try {
        const saved = await getWebtoonEvaluation(currentSession, webtoon.id);
        if (!active) {
          return;
        }
        setEvaluation(saved);
        setRating(saved?.rating ?? null);
        setEmotionTags(saved?.emotionTags ?? []);
      } catch (error) {
        if (!active) {
          return;
        }
        if (error instanceof EvaluationApiError && error.status === 401) {
          clearAuthSession();
          setError("로그인이 만료되었습니다. 다시 로그인해 주세요.");
          return;
        }
        setError("저장된 평가를 불러오지 못했습니다.");
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    loadEvaluation();

    return () => {
      active = false;
    };
  }, [session, webtoon.id]);

  function buildLoginHref(): string {
    const nextPath = encodeURIComponent(pathname || `/webtoons/${webtoon.id}`);
    return `/login?next=${nextPath}`;
  }

  function toggleEmotionTag(tag: string) {
    setMessage(null);
    setError(null);
    setEmotionTags((current) => (current.includes(tag) ? current.filter((item) => item !== tag) : [...current, tag]));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isSaving) {
      return;
    }
    if (!session) {
      router.push(buildLoginHref());
      return;
    }
    if (!rating) {
      setError("레이팅을 선택해 주세요.");
      return;
    }
    if (emotionTags.length === 0) {
      setError("감정 태그를 하나 이상 선택해 주세요.");
      return;
    }

    setIsSaving(true);
    setError(null);
    setMessage(null);
    try {
      const saved = await saveWebtoonEvaluation(session, webtoon.id, rating, emotionTags);
      setEvaluation(saved);
      setRating(saved.rating);
      setEmotionTags(saved.emotionTags);
      setMessage("평가를 저장했습니다.");
    } catch (error) {
      if (error instanceof EvaluationApiError && error.status === 401) {
        clearAuthSession();
        router.push(buildLoginHref());
        return;
      }
      setError(error instanceof EvaluationApiError ? error.message : "평가 저장 중 오류가 발생했습니다.");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDelete() {
    if (!session || !evaluation || isSaving) {
      return;
    }

    setIsSaving(true);
    setError(null);
    setMessage(null);
    try {
      await deleteWebtoonEvaluation(session, webtoon.id);
      setEvaluation(null);
      setRating(null);
      setEmotionTags([]);
      setMessage("평가를 삭제했습니다.");
    } catch (error) {
      if (error instanceof EvaluationApiError && error.status === 401) {
        clearAuthSession();
        router.push(buildLoginHref());
        return;
      }
      setError(error instanceof EvaluationApiError ? error.message : "평가 삭제 중 오류가 발생했습니다.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <section className="webtoon-evaluation-panel quick-nav reveal" aria-labelledby="webtoon-evaluation-title">
      <div className="evaluation-head">
        <div>
          <p className="section-kicker">내 감상 기록</p>
          <h2 id="webtoon-evaluation-title">웹툰 평가</h2>
        </div>
        {evaluation ? <span className="evaluation-saved-badge">저장됨</span> : null}
      </div>

      {!session ? (
        <div className="evaluation-login">
          <p className="description">로그인하면 이 작품의 레이팅과 감정 태그를 저장할 수 있어요.</p>
          <Link className="more-link" href={buildLoginHref()}>
            로그인하고 평가하기
          </Link>
        </div>
      ) : (
        <form className="evaluation-form" onSubmit={handleSubmit}>
          <fieldset disabled={isLoading || isSaving}>
            <legend>레이팅</legend>
            <div className="rating-options">
              {WEBTOON_RATINGS.map((item) => {
                const meta = WEBTOON_RATING_META[item];
                return (
                  <button
                    key={item}
                    className={`rating-choice rating-${meta.tone} ${rating === item ? "active" : ""}`}
                    type="button"
                    onClick={() => {
                      setRating(item);
                      setMessage(null);
                      setError(null);
                    }}
                    aria-pressed={rating === item}
                    aria-label={`${item} 등급, ${meta.label}`}
                  >
                    <span className="rating-icon" aria-hidden="true">{meta.icon}</span>
                    <strong>{item}</strong>
                    <small>{meta.label}</small>
                  </button>
                );
              })}
            </div>
          </fieldset>

          <fieldset disabled={isLoading || isSaving}>
            <legend>이 웹툰은 나에게..</legend>
            <div className="emotion-tag-grid">
              {WEBTOON_EMOTION_TAGS.map((tag) => {
                const selected = emotionTags.includes(tag);
                const meta = WEBTOON_EMOTION_TAG_META[tag];
                return (
                  <button
                    key={tag}
                    className={selected ? "active" : ""}
                    type="button"
                    onClick={() => toggleEmotionTag(tag)}
                    aria-pressed={selected}
                  >
                    <span aria-hidden="true">{meta.emoji}</span>
                    {meta.label}
                  </button>
                );
              })}
            </div>
          </fieldset>

          <div className="evaluation-actions">
            <button className="evaluation-save-btn" type="submit" disabled={isLoading || isSaving}>
              {isSaving ? "저장 중..." : "평가 저장"}
            </button>
            {evaluation ? (
              <button className="evaluation-delete-btn" type="button" onClick={handleDelete} disabled={isLoading || isSaving}>
                삭제
              </button>
            ) : null}
          </div>

          {message ? <p className="evaluation-message">{message}</p> : null}
          {error ? <p className="evaluation-error">{error}</p> : null}
        </form>
      )}
    </section>
  );
}
