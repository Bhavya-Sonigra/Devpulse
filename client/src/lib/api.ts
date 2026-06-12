const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ??
  process.env.BACKEND_URL ??
  "http://localhost:8080";

export function apiUrl(path: string): string {
  const normalizedBase = API_BASE_URL.replace(/\/+$/, "");
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${normalizedBase}${normalizedPath}`;
}

