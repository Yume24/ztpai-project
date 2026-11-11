import type { QueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/api/queryKeys';
import { appendToTrail } from '@/features/live/positions';
import type { Position, TrailPoint } from '@/realtime/types';

export function appendLivePosition(queryClient: QueryClient, roomId: string, position: Position): void {
  queryClient.setQueryData<TrailPoint[]>(queryKeys.trail(roomId, position.userId), (old) =>
    old && appendToTrail(old, position),
  );
}
