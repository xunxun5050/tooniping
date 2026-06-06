export function HeaderSearch() {
  return (
    <form className="header-search" action="/webtoons" method="get">
      <input type="search" name="keyword" placeholder="웹툰 검색" aria-label="웹툰 검색" />
      <button type="submit">검색</button>
    </form>
  );
}
