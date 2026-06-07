export function toPlatformStyleClass(platformCode: string): string {
  if (platformCode === "KAKAO_WEBTOON") {
    return "platform-kakao";
  }
  if (platformCode === "NAVER_WEBTOON") {
    return "platform-naver";
  }
  return "platform-default";
}
