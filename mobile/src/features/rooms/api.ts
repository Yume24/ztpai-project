import { api, unwrap } from '@/api/client';
import type {
  CreateRoomRequest,
  JoinRoomRequest,
  PageResponseRoomSummaryResponse,
  RoomDetailsResponse,
  RoomStatus,
  UpdateRoomRequest,
} from '@/api/types';

export const ROOMS_PAGE_SIZE = 20;

export async function listMyRooms(status: RoomStatus, page: number): Promise<PageResponseRoomSummaryResponse> {
  return unwrap(await api.GET('/api/me/rooms', { params: { query: { status, page, size: ROOMS_PAGE_SIZE } } }));
}

export async function getRoom(id: string): Promise<RoomDetailsResponse> {
  return unwrap(await api.GET('/api/rooms/{id}', { params: { path: { id } } }));
}

export async function createRoom(body: CreateRoomRequest): Promise<RoomDetailsResponse> {
  return unwrap(await api.POST('/api/rooms', { body }));
}

export async function joinRoom(body: JoinRoomRequest): Promise<RoomDetailsResponse> {
  return unwrap(await api.POST('/api/rooms/join', { body }));
}

export async function updateRoom(id: string, body: UpdateRoomRequest): Promise<RoomDetailsResponse> {
  return unwrap(await api.PATCH('/api/rooms/{id}', { params: { path: { id } }, body }));
}

export async function closeRoom(id: string): Promise<void> {
  await unwrap(await api.POST('/api/rooms/{id}/close', { params: { path: { id } } }));
}

export async function deleteRoom(id: string): Promise<void> {
  await unwrap(await api.DELETE('/api/rooms/{id}', { params: { path: { id } } }));
}

export async function leaveRoom(id: string): Promise<void> {
  await unwrap(await api.POST('/api/rooms/{id}/leave', { params: { path: { id } } }));
}
