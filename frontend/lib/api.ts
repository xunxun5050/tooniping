import { ApiResponse } from "@/lib/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

function makeUrl(path: string): string {
  return `${API_BASE_URL}${path}`;
}

export async function fetchApi<T>(path: string): Promise<T> {
  const res = await fetch(makeUrl(path), {
    next: { revalidate: 0 }
  });

  if (!res.ok) {
    throw new Error(`API 요청 실패: ${path}`);
  }

  const json = (await res.json()) as ApiResponse<T>;
  if (!json.success) {
    throw new Error(json.message ?? "API 오류가 발생했습니다.");
  }

  return json.data;
}
