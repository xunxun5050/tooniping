import Link from "next/link";
import { WebtoonCard } from "@/components/WebtoonCard";
import { fetchApi } from "@/lib/api";
import { HomeData, PagedResult, WebtoonCard as WebtoonCardType } from "@/lib/types";

export default async function HomePage() {
  const [homeData, latestList] = await Promise.all([
    fetchApi<HomeData>("/api/home"),
    fetchApi<PagedResult<WebtoonCardType>>("/api/webtoons?page=0&size=18&sort=latest")
  ]);

  return (
    <section className="home-naver">
      <section className="naver-home-head reveal">
        <div className="naver-title-row">
          <div>
            <p className="eyebrow">WEBTOON HUB</p>
            <h1>요일별 웹툰</h1>
          </div>
          <Link className="more-link" href="/webtoons">
            전체 목록 보기
          </Link>
        </div>

        <form className="search-form naver-search" action="/webtoons" method="get">
          <input type="text" name="keyword" placeholder="제목/작가를 검색해보세요" aria-label="검색어" />
          <button type="submit">검색</button>
        </form>

        <div className="weekday-strip">
          {homeData.weekdayMenus.map((menu, index) => (
            <Link
              key={menu.code}
              className={`weekday-tab ${index === 0 ? "active" : ""}`}
              href={`/webtoons?weekday=${menu.code}`}
            >
              <span>{menu.name}</span>
              <strong>{menu.count}</strong>
            </Link>
          ))}
        </div>

        <div className="genre-strip">
          {homeData.genreMenus.map((menu) => (
            <Link key={menu.code} className="chip" href={`/webtoons?genre=${menu.code}`}>
              {menu.name}
            </Link>
          ))}
        </div>
      </section>

      <section className="naver-grid-panel reveal">
        <div className="naver-section-head">
          <h2>인기/신규 웹툰</h2>
          <Link className="more-link" href="/webtoons?sort=latest">
            더보기
          </Link>
        </div>
        <div className="card-grid home-grid">
          {latestList.content.map((item) => (
            <WebtoonCard key={item.id} webtoon={item} compact showSourceButton={false} />
          ))}
        </div>
      </section>
    </section>
  );
}
