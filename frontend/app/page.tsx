import Link from "next/link";
import { HomeLoginWeekdayRedirect } from "@/components/HomeLoginWeekdayRedirect";
import { WebtoonCard } from "@/components/WebtoonCard";
import { fetchApi } from "@/lib/api";
import { HomeData, PagedResult, WebtoonCard as WebtoonCardType } from "@/lib/types";

const WEEKDAY_BY_ENGLISH_NAME: Record<string, string> = {
  Monday: "MONDAY",
  Tuesday: "TUESDAY",
  Wednesday: "WEDNESDAY",
  Thursday: "THURSDAY",
  Friday: "FRIDAY",
  Saturday: "SATURDAY",
  Sunday: "SUNDAY"
};

function getTodayWeekdayCodeInSeoul(): string {
  const todayName = new Intl.DateTimeFormat("en-US", {
    weekday: "long",
    timeZone: "Asia/Seoul"
  }).format(new Date());

  return WEEKDAY_BY_ENGLISH_NAME[todayName] ?? "MONDAY";
}

export default async function HomePage() {
  const todayWeekdayCode = getTodayWeekdayCodeInSeoul();
  const [homeData, popularList] = await Promise.all([
    fetchApi<HomeData>("/api/home"),
    fetchApi<PagedResult<WebtoonCardType>>(
      `/api/webtoons?page=0&size=18&sort=popular&weekday=${todayWeekdayCode}`
    )
  ]);
  const todayWeekdayName =
    homeData.weekdayMenus.find((menu) => menu.code === todayWeekdayCode)?.name ?? "오늘";

  return (
    <section className="home-naver">
      <HomeLoginWeekdayRedirect />
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

        <div className="weekday-strip">
          {homeData.weekdayMenus.map((menu) => (
            <Link
              key={menu.code}
              className={`weekday-tab ${menu.code === todayWeekdayCode ? "active" : ""}`}
              href={`/webtoons?sort=popular&weekday=${menu.code}`}
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
          <h2>{todayWeekdayName} 인기 웹툰</h2>
          <Link className="more-link" href={`/webtoons?sort=popular&weekday=${todayWeekdayCode}`}>
            더보기
          </Link>
        </div>
        <div className="card-grid home-grid">
          {popularList.content.map((item) => (
            <WebtoonCard key={item.id} webtoon={item} compact showSourceButton={false} />
          ))}
        </div>
      </section>
    </section>
  );
}
