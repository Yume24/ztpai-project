import { api, unwrap } from '@/api/client';
import type { LocationPointRequest } from '@/api/types';

const MAX_BATCH_SIZE = 50;

export async function sendLocations(points: LocationPointRequest[]): Promise<number> {
  if (points.length === 0) {
    return -1;
  }
  let activeRoomCount = -1;
  for (let start = 0; start < points.length; start += MAX_BATCH_SIZE) {
    const body = { points: points.slice(start, start + MAX_BATCH_SIZE) };
    activeRoomCount = (await unwrap(await api.POST('/api/me/locations', { body }))).activeRoomCount;
  }
  return activeRoomCount;
}
