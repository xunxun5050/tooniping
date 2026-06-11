import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "개인정보 처리방침 | Webtoon Hub",
  description: "웹툰 허브의 개인정보 수집 항목, 이용 목적, 보유 기간, 거부 방법 안내"
};

const privacyRows = [
  {
    title: "회원가입 및 로그인",
    items: "이메일, 비밀번호 해시, 닉네임, 이메일 인증번호 발송·검증 기록",
    purpose: "회원 식별, 이메일 소유 확인, 로그인 유지, 닉네임 표시, 계정 관리",
    retention: "회원 정보는 회원 탈퇴 시까지, 이메일 인증번호 기록은 인증 완료 또는 만료 후 삭제·갱신됩니다. 단, 관계 법령에 따라 보관이 필요한 경우 해당 기간 동안 보관합니다.",
    refusal: "회원가입을 하지 않을 수 있습니다. 이 경우 즐겨찾기, 평가 저장 등 회원 기능 이용이 제한됩니다."
  },
  {
    title: "내 서재 및 평가",
    items: "즐겨찾기한 웹툰, 웹툰 평가 등급, 감정 태그, 생성·수정 시각",
    purpose: "내 서재 제공, 마이페이지 활동 기록 표시, 개인화된 감상 기록 관리",
    retention: "회원 탈퇴 또는 사용자가 해당 기록을 삭제할 때까지 보관합니다.",
    refusal: "즐겨찾기 또는 평가 기능을 사용하지 않을 수 있습니다. 저장한 기록은 마이페이지와 상세 페이지에서 직접 삭제할 수 있습니다."
  },
  {
    title: "로그인 유지",
    items: "브라우저에 저장되는 로그인 유지 쿠키, access token, refresh token",
    purpose: "브라우저 재방문 시 로그인 상태 복원, 보안 인증 처리",
    retention: "access token은 짧은 기간 동안, refresh token은 설정된 보유 기간 또는 로그아웃·회원 탈퇴 시까지 보관합니다.",
    refusal: "브라우저 쿠키를 차단하거나 로그아웃하면 로그인 유지 기능을 사용할 수 없습니다."
  },
  {
    title: "추후 광고 및 분석 도구",
    items: "쿠키, 광고 식별자, IP 주소, 브라우저·기기 정보, 방문·검색·클릭 등 서비스 이용 기록",
    purpose: "광고 노출 및 빈도 제한, 광고 성과 측정, 맞춤형 광고 제공, 서비스 품질 개선",
    retention: "도구 제공 사업자의 정책 또는 이용자의 동의 철회·거부 설정 시까지 보관될 수 있습니다. 실제 도입 시 구체적인 사업자와 기간을 별도로 안내합니다.",
    refusal: "브라우저 쿠키 차단, 모바일 광고 ID 재설정 또는 맞춤형 광고 제한, 각 광고 사업자가 제공하는 수신 거부 기능을 통해 거부할 수 있습니다."
  }
];

export default function PrivacyPage() {
  return (
    <section className="privacy-page reveal">
      <section className="about-hero">
        <p className="eyebrow">PRIVACY</p>
        <h1>개인정보 처리방침</h1>
        <p className="about-lead">
          웹툰 허브는 서비스 제공에 필요한 정보만 처리하고, 추후 광고 또는 분석 도구를 도입하는 경우
          수집 항목과 거부 방법을 사용자가 이해하기 쉽게 안내하겠습니다.
        </p>
        <p className="privacy-updated">시행일: 2026.06.09</p>
      </section>

      <section className="about-band">
        <div className="about-section-head">
          <p className="about-kicker">처리 항목</p>
          <h2>수집 항목, 이용 목적, 보유 기간, 거부 방법</h2>
        </div>
        <div className="privacy-table-wrap">
          <table className="privacy-table">
            <thead>
              <tr>
                <th>구분</th>
                <th>수집 항목</th>
                <th>이용 목적</th>
                <th>보유 기간</th>
                <th>거부 방법</th>
              </tr>
            </thead>
            <tbody>
              {privacyRows.map((row) => (
                <tr key={row.title}>
                  <th scope="row">{row.title}</th>
                  <td>{row.items}</td>
                  <td>{row.purpose}</td>
                  <td>{row.retention}</td>
                  <td>{row.refusal}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="about-band">
        <div className="about-section-head">
          <p className="about-kicker">광고 안내</p>
          <h2>맞춤형 광고 도입 전 고지</h2>
        </div>
        <ul className="about-principles">
          <li>현재 광고 SDK 또는 맞춤형 광고 도구를 운영하지 않습니다.</li>
          <li>광고성 콘텐츠는 일반 웹툰 정렬과 분리하고, 사용자가 광고임을 알 수 있도록 표시합니다.</li>
          <li>광고 또는 분석 도구를 도입하기 전에 처리방침에 사업자, 수집 항목, 보유 기간, 거부 방법을 업데이트합니다.</li>
        </ul>
      </section>

      <section className="about-band about-source-band">
        <div>
          <p className="about-kicker">돌아가기</p>
          <h2>서비스 소개</h2>
        </div>
        <p>
          웹툰 허브의 운영 원칙과 출처 안내는 소개 페이지에서 함께 확인할 수 있습니다.{" "}
          <Link className="inline-link" href="/about">
            소개 페이지로 이동
          </Link>
        </p>
      </section>
    </section>
  );
}
