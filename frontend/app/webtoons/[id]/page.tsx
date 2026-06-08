import Link from "next/link";
import { notFound } from "next/navigation";
import { DetailFavoriteButton } from "@/components/DetailFavoriteButton";
import { WebtoonEvaluationForm } from "@/components/WebtoonEvaluationForm";
import { WebtoonCard } from "@/components/WebtoonCard";
import { fetchApi } from "@/lib/api";
import { toPlatformStyleClass } from "@/lib/platform-style";
import { WebtoonCard as WebtoonCardType, WebtoonDetail } from "@/lib/types";

type Props = {
  params: Promise<{ id: string }>;
};

export default async function WebtoonDetailPage({ params }: Props) {
  const { id } = await params;
  const webtoonId = Number(id);
  if (Number.isNaN(webtoonId)) {
    notFound();
  }

  let detail: WebtoonDetail;
  try {
    detail = await fetchApi<WebtoonDetail>(`/api/webtoons/${webtoonId}`);
  } catch {
    notFound();
  }

  let similar: WebtoonCardType[] = [];
  try {
    similar = await fetchApi<WebtoonCardType[]>(`/api/webtoons/${webtoonId}/similar?size=6`);
  } catch {
    similar = [];
  }

  const thumbnail = detail.thumbnail.storedUrl || detail.thumbnail.sourceUrl;
  const platformStyleClass = toPlatformStyleClass(detail.platform.code);
  const detailFavoriteWebtoon: WebtoonCardType = {
    id: detail.id,
    title: detail.title,
    author: detail.author,
    description: detail.description ?? "",
    platform: detail.platform,
    genres: detail.genres,
    weekdays: detail.weekdays,
    status: detail.status,
    statusName: detail.statusName,
    thumbnailUrl: thumbnail,
    originalUrl: detail.originalUrl
  };

  return (
    <section className="detail-page">
      <Link href="/webtoons" className="back-link">
        목록으로 돌아가기
      </Link>

      <article className={`detail-main reveal ${platformStyleClass}`}>
        <div className="detail-thumb-wrap">
          {thumbnail ? <img src={thumbnail} alt={detail.title} className="detail-thumb" /> : <div className="detail-thumb placeholder">NO IMAGE</div>}
        </div>
        <div className="detail-meta">
          <p className={`platform-tag ${platformStyleClass}`}>{detail.platform.name}</p>
          <h1>{detail.title}</h1>
          <p className="author">{detail.author || "작가 미상"}</p>
          <p className="description">{detail.description}</p>

          <dl>
            <div>
              <dt>장르</dt>
              <dd>{detail.genres.map((g) => g.name).join(" · ") || "-"}</dd>
            </div>
            <div>
              <dt>연재 요일</dt>
              <dd>{detail.weekdays.map((w) => w.name).join(" · ") || "-"}</dd>
            </div>
            <div>
              <dt>연재 상태</dt>
              <dd>{detail.statusName}</dd>
            </div>
            <div>
              <dt>원본 출처</dt>
              <dd>{detail.platform.name}</dd>
            </div>
          </dl>

          <div className="detail-actions">
            <DetailFavoriteButton webtoon={detailFavoriteWebtoon} />
            <a className={`source-btn large ${platformStyleClass}`} href={detail.originalUrl} target="_blank" rel="noreferrer">
              {detail.platform.name} 바로가기
            </a>
          </div>
        </div>
      </article>

      <WebtoonEvaluationForm webtoon={detailFavoriteWebtoon} />

      {similar.length > 0 ? (
        <section className="quick-nav reveal">
          <h2>비슷한 장르 추천</h2>
          <div className="card-grid">
            {similar.map((item) => (
              <WebtoonCard key={item.id} webtoon={item} />
            ))}
          </div>
        </section>
      ) : null}
    </section>
  );
}
