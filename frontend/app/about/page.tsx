import Link from "next/link";

const features = [
  {
    title: "여러 플랫폼을 한 화면에서",
    description: "네이버 웹툰과 카카오 웹툰의 작품 정보를 모아 플랫폼을 오가며 찾는 시간을 줄입니다."
  },
  {
    title: "요일, 장르, 상태로 빠르게",
    description: "오늘 볼 작품부터 완결작까지 요일별, 장르별, 연재 상태별로 바로 좁혀볼 수 있습니다."
  },
  {
    title: "인기순으로 먼저 보기",
    description: "플랫폼별 인기 흐름을 반영해 지금 많이 보는 작품을 목록 상단에서 확인할 수 있습니다."
  },
  {
    title: "내 서재에 담아두기",
    description: "마음에 드는 작품은 하트로 저장하고, 우측 상단 닉네임에서 연재중 즐겨찾기를 요일별로 다시 볼 수 있습니다."
  }
];

export default function AboutPage() {
  return (
    <section className="about-page reveal">
      <section className="about-hero">
        <p className="eyebrow">WEBTOON HUB</p>
        <h1>흩어진 웹툰을 한곳에서, 내 취향대로</h1>
        <p className="about-lead">
          웹툰 허브는 네이버 웹툰과 카카오 웹툰의 작품 정보를 모아 요일, 장르, 인기순으로 탐색할 수 있게
          돕는 웹툰 링크 허브입니다. 보고 싶은 작품을 발견하면 공식 페이지로 이동하고, 마음에 드는 작품은
          내 서재에 담아 다시 찾아볼 수 있습니다.
        </p>
        <div className="about-actions">
          <Link className="source-btn" href="/webtoons?sort=popular">
            인기 웹툰 보기
          </Link>
          <Link className="secondary-btn" href="/favorites">
            내 서재 가기
          </Link>
        </div>
      </section>

      <section className="about-band">
        <div className="about-section-head">
          <p className="about-kicker">둘러보기</p>
          <h2>찾고, 고르고, 저장하는 흐름을 짧게</h2>
        </div>
        <div className="about-feature-list">
          {features.map((feature, index) => (
            <article className="about-feature" key={feature.title}>
              <span>{String(index + 1).padStart(2, "0")}</span>
              <strong>{feature.title}</strong>
              <p>{feature.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="about-band">
        <div className="about-section-head">
          <p className="about-kicker">운영 원칙</p>
          <h2>원작자가 만든 공식 공간으로 연결합니다</h2>
        </div>
        <ul className="about-principles">
          <li>웹툰 본문, 회차 이미지, 유료 콘텐츠를 복제하여 제공하지 않습니다.</li>
          <li>작품 제목, 작가, 장르, 요일, 썸네일과 같은 탐색용 정보만 제공합니다.</li>
          <li>작품 상세 화면에서 출처를 표시하고, 원본 보러가기 버튼으로 공식 페이지에 연결합니다.</li>
        </ul>
      </section>

      <section className="about-band about-source-band">
        <div>
          <p className="about-kicker">출처</p>
          <h2>네이버 웹툰 · 카카오 웹툰</h2>
        </div>
        <p>
          각 작품의 권리와 감상 경험은 원 플랫폼에 있습니다. 웹툰 허브는 더 쉽게 발견하고 정리할 수 있도록
          돕는 탐색 서비스로 운영됩니다.
        </p>
      </section>
    </section>
  );
}
