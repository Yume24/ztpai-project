import { api, unwrap } from '@/api/client';
import type { AlertType, PageResponseAlertResponse } from '@/api/types';

export const ALERTS_PAGE_SIZE = 20;

export async function listAlerts(id: string, page: number, type?: AlertType): Promise<PageResponseAlertResponse> {
  return unwrap(
    await api.GET('/api/rooms/{id}/alerts', {
      params: { path: { id }, query: { type, page, size: ALERTS_PAGE_SIZE } },
    }),
  );
}
