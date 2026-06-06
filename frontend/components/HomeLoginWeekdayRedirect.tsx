"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { getTodayWeekdayCodeInSeoul, readAuthSession } from "@/lib/auth-client";

export function HomeLoginWeekdayRedirect() {
  const router = useRouter();

  useEffect(() => {
    const session = readAuthSession();
    if (!session) {
      return;
    }

    const weekday = getTodayWeekdayCodeInSeoul();
    router.replace(`/webtoons?size=20&page=0&sort=popular&weekday=${weekday}`);
  }, [router]);

  return null;
}
