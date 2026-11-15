import { api, unwrap } from '@/api/client';
import type { PageResponseAdminRoomResponse, PageResponseAdminUserResponse, RoomStatus } from '@/api/types';

export const ADMIN_PAGE_SIZE = 20;

export async function listAdminUsers(
  query: string,
  blocked: boolean | undefined,
  page: number,
): Promise<PageResponseAdminUserResponse> {
  return unwrap(
    await api.GET('/api/admin/users', {
      params: { query: { query: query || undefined, blocked, page, size: ADMIN_PAGE_SIZE } },
    }),
  );
}

export async function blockUser(id: string): Promise<void> {
  await unwrap(await api.PUT('/api/admin/users/{id}/block', { params: { path: { id } } }));
}

export async function unblockUser(id: string): Promise<void> {
  await unwrap(await api.DELETE('/api/admin/users/{id}/block', { params: { path: { id } } }));
}

export type AdminRoomFilters = { status?: RoomStatus; from?: string; to?: string };

export async function listAdminRooms(
  filters: AdminRoomFilters,
  page: number,
): Promise<PageResponseAdminRoomResponse> {
  return unwrap(
    await api.GET('/api/admin/rooms', {
      params: { query: { ...filters, page, size: ADMIN_PAGE_SIZE } },
    }),
  );
}

export async function forceCloseRoom(id: string): Promise<void> {
  await unwrap(await api.POST('/api/admin/rooms/{id}/close', { params: { path: { id } } }));
}
