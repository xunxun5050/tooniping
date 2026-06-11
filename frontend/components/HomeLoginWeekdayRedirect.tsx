"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { getAuthSession, getTodayWeekdayCodeInSeoul } from "@/lib/auth-client";

export function HomeLoginWeekdayRedirect() {
  const router = useRouter();

  useEffect(() => {
    let active = true;

    async function redirectLoggedInUser() {
      const session = await getAuthSession();
      if (!active || !session) {
        return;
      }

      const weekday = getTodayWeekdayCodeInSeoul();
      router.replace(`/webtoons?size=20&page=0&sort=popular&weekday=${weekday}`);
    }

    redirectLoggedInUser();

    return () => {
      active = false;
    };
  }, [router]);

  return null;
}
