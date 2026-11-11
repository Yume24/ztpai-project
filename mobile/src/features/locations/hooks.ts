import { skipToken, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo } from 'react';
import { ApiError } from '@/api/errors';
import { queryKeys } from '@/api/queryKeys';
import { useLiveLocationsStore } from '@/features/live/liveLocationsStore';
import { mergePositions } from '@/features/live/positions';
import type { Position, TrailPoint } from '@/realtime/types';
import { getLatestPositions, getTrail } from './api';
import { appendLivePosition } from './trailCache';

const TRAIL_FORBIDDEN_MESSAGE = 'Brak dostępu do trasy tej osoby.';

export function useLatestPositions(roomId: string): { positions: Position[]; error: unknown } {
  const latest = useQuery({ queryKey: queryKeys.latest(roomId), queryFn: () => getLatestPositions(roomId) });
  const live = useLiveLocationsStore((state) => state.byRoom[roomId]);
  const positions = useMemo(() => mergePositions(latest.data ?? [], live), [latest.data, live]);
  return { positions, error: latest.error };
}

type TrailResult = { trail: TrailPoint[] | null; error: unknown; forbiddenMessage: string | null };

export function useTrail(roomId: string, userId: string | null): TrailResult {
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: queryKeys.trail(roomId, userId ?? 'none'),
    queryFn: userId === null ? skipToken : () => getTrail(roomId, userId),
  });
  const livePosition = useLiveLocationsStore((state) =>
    userId === null ? undefined : state.byRoom[roomId]?.[userId],
  );

  useEffect(() => {
    if (livePosition) {
      appendLivePosition(queryClient, roomId, livePosition);
    }
  }, [queryClient, roomId, livePosition]);

  if (userId === null) {
    return { trail: null, error: null, forbiddenMessage: null };
  }
  if (query.error instanceof ApiError && query.error.status === 403) {
    return { trail: null, error: null, forbiddenMessage: TRAIL_FORBIDDEN_MESSAGE };
  }
  return { trail: query.data ?? null, error: query.error, forbiddenMessage: null };
}
