import { api, unwrap } from '@/api/client';
import type { PositionResponse, TrailPointResponse } from '@/api/types';

export async function getLatestPositions(id: string): Promise<PositionResponse[]> {
  return unwrap(await api.GET('/api/rooms/{id}/locations/latest', { params: { path: { id } } }));
}

export async function getTrail(id: string, userId: string): Promise<TrailPointResponse[]> {
  return unwrap(await api.GET('/api/rooms/{id}/members/{userId}/trail', { params: { path: { id, userId } } }));
}
