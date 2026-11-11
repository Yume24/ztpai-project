import { useInfiniteQuery } from '@tanstack/react-query';
import { nextPageParam } from '@/api/paging';
import { queryKeys } from '@/api/queryKeys';
import type { AlertType } from '@/api/types';
import { listAlerts } from './api';

export function useAlerts(roomId: string, type?: AlertType) {
  return useInfiniteQuery({
    queryKey: queryKeys.alerts(roomId, type),
    queryFn: ({ pageParam }) => listAlerts(roomId, pageParam, type),
    initialPageParam: 0,
    getNextPageParam: nextPageParam,
  });
}
