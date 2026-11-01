export function nextPageParam(page: { page: number; totalPages: number }): number | undefined {
  const next = page.page + 1;
  return next < page.totalPages ? next : undefined;
}
