import Link from "next/link";
import { WebtoonCard as WebtoonCardType } from "@/lib/types";

type Props = {
  webtoon: WebtoonCardType;
  showSourceButton?: boolean;
  compact?: boolean;
  className?: string;
};

export function WebtoonCard({
  webtoon,
  showSourceButton = true,
  compact = false,
  className
}: Props) {
  const genresText = webtoon.genres.map((genre) => genre.name).join(" · ");
  const weekdaysText = webtoon.weekdays.map((day) => day.name).join(" · ");
  const compactMeta = weekdaysText || genresText || webtoon.statusName;
  const cardClassName = ["webtoon-card", "reveal", compact ? "compact" : "", className ?? ""]
    .filter(Boolean)
    .join(" ");

  return (
    <article className={cardClassName}>
      <Link href={`/webtoons/${webtoon.id}`} className="webtoon-card-link">
        <div className="webtoon-thumb-wrap">
          {webtoon.thumbnailUrl ? (
            <img className="webtoon-thumb" src={webtoon.thumbnailUrl} alt={webtoon.title} />
          ) : (
            <div className="webtoon-thumb placeholder">NO IMAGE</div>
          )}
        </div>
        <div className="webtoon-meta">
          <h3>{webtoon.title}</h3>
          <p className="author">{webtoon.author || "작가 미상"}</p>
          {compact ? (
            <p className="compact-meta">{compactMeta}</p>
          ) : (
            <>
              <p className="chips">
                {genresText}
                {genresText && weekdaysText ? " | " : ""}
                {weekdaysText}
              </p>
              <p className="status">{webtoon.statusName}</p>
            </>
          )}
        </div>
      </Link>
      {showSourceButton ? (
        <a className="source-btn" href={webtoon.originalUrl} target="_blank" rel="noreferrer">
          네이버 웹툰 바로가기
        </a>
      ) : null}
    </article>
  );
}
