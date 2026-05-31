export type ApiResponse<T> = {
  success: boolean;
  data: T;
  message: string | null;
};

export type CodeName = {
  code: string;
  name: string;
};

export type Platform = {
  code: string;
  name: string;
  baseUrl?: string | null;
};

export type WebtoonCard = {
  id: number;
  title: string;
  author: string;
  description: string;
  platform: Platform;
  genres: CodeName[];
  weekdays: CodeName[];
  status: string;
  statusName: string;
  thumbnailUrl: string | null;
  originalUrl: string;
};

export type PagedResult<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

export type WebtoonDetail = {
  id: number;
  title: string;
  author: string;
  description: string;
  platform: Platform;
  genres: CodeName[];
  weekdays: CodeName[];
  status: string;
  statusName: string;
  thumbnail: {
    sourceUrl: string | null;
    storedUrl: string | null;
  };
  originalUrl: string;
  lastCrawledAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type HomeData = {
  recentWebtoons: { id: number; title: string; author: string; thumbnailUrl: string | null }[];
  weekdayMenus: { code: string; name: string; count: number }[];
  genreMenus: { code: string; name: string; count: number }[];
};

export type WebtoonFilters = {
  platforms: CodeName[];
  genres: CodeName[];
  weekdays: CodeName[];
  statuses: CodeName[];
};
