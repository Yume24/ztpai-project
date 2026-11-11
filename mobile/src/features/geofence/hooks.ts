import { useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/api/queryKeys';
import type { GeofenceRequest } from '@/api/types';
import { deleteGeofence, saveGeofence } from './api';

function useInvalidateRoom(roomId: string) {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: queryKeys.room(roomId) });
}

export function useSaveGeofence(roomId: string) {
  const invalidate = useInvalidateRoom(roomId);
  return useMutation({
    mutationFn: (body: GeofenceRequest) => saveGeofence(roomId, body),
    onSuccess: invalidate,
  });
}

export function useDeleteGeofence(roomId: string) {
  const invalidate = useInvalidateRoom(roomId);
  return useMutation({ mutationFn: () => deleteGeofence(roomId), onSuccess: invalidate });
}
