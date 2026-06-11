import type { Metadata } from "next";
import Link from "next/link";
import { AuthMenu } from "@/components/AuthMenu";
import { HeaderSearch } from "@/components/HeaderSearch";
import "./globals.css";

export const metadata: Metadata = {
  title: "Webtoon Hub",
  description: "네이버 웹툰과 카카오 웹툰을 함께 탐색하는 웹툰 링크 허브 서비스"
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>
        <div className="bg-orb orb-a" />
        <div className="bg-orb orb-b" />
        <header className="site-header">
          <Link className="logo" href="/">
            WEBTOON HUB
          </Link>
          <nav>
            <HeaderSearch />
            <Link href="/about">소개</Link>
            <AuthMenu />
          </nav>
        </header>
        <main className="container">{children}</main>
      </body>
    </html>
  );
}
