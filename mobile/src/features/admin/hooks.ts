import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { nextPageParam } from '@/api/paging';
import { queryKeys } from '@/api/queryKeys';
import type { RoomStatus } from '@/api/types';
import { blockUser, forceCloseRoom, listAdminRooms, listAdminUsers, unblockUser } from './api';

export function useAdminUsers(query: string, blocked?: boolean) {
  return useInfiniteQuery({
    queryKey: queryKeys.adminUsers(query, blocked),
    queryFn: ({ pageParam }) => listAdminUsers(query, blocked, pageParam),
    initialPageParam: 0,
    getNextPageParam: nextPageParam,
  });
}

function useInvalidateAdminUsers() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: queryKeys.adminUsersPrefix });
}

export function useBlockUser() {
  const queryClient = useQueryClient();
  const invalidateUsers = useInvalidateAdminUsers();
  return useMutation({
    mutationFn: blockUser,
    onSuccess: () =>
      Promise.all([invalidateUsers(), queryClient.invalidateQueries({ queryKey: queryKeys.adminRoomsPrefix })]),
  });
}

export function useUnblockUser() {
  const invalidate = useInvalidateAdminUsers();
  return useMutation({ mutationFn: unblockUser, onSuccess: invalidate });
}

export function useAdminRooms(filters: { status?: RoomStatus; from?: Date; to?: Date }) {
  const apiFilters = { status: filters.status, from: filters.from?.toISOString(), to: filters.to?.toISOString() };
  return useInfiniteQuery({
    queryKey: queryKeys.adminRooms(apiFilters),
    queryFn: ({ pageParam }) => listAdminRooms(apiFilters, pageParam),
    initialPageParam: 0,
    getNextPageParam: nextPageParam,
  });
}

export function useForceCloseRoom() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: forceCloseRoom,
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.adminRoomsPrefix }),
        queryClient.invalidateQueries({ queryKey: queryKeys.roomsPrefix }),
        queryClient.invalidateQueries({ queryKey: queryKeys.myRoomsPrefix }),
      ]),
  });
}
