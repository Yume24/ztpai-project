import { api, unwrap } from '@/api/client';
import type { GeofenceRequest } from '@/api/types';

export async function saveGeofence(id: string, body: GeofenceRequest): Promise<void> {
  await unwrap(await api.PUT('/api/rooms/{id}/geofence', { params: { path: { id } }, body }));
}

export async function deleteGeofence(id: string): Promise<void> {
  await unwrap(await api.DELETE('/api/rooms/{id}/geofence', { params: { path: { id } } }));
}
