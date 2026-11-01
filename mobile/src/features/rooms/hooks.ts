import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { nextPageParam } from '@/api/paging';
import { queryKeys } from '@/api/queryKeys';
import type { RoomStatus, UpdateRoomRequest } from '@/api/types';
import { useLiveLocationsStore } from '@/features/live/liveLocationsStore';
import {
  closeRoom,
  createRoom,
  deleteRoom,
  getRoom,
  joinRoom,
  leaveRoom,
  listMyRooms,
  updateRoom,
} from './api';

let onMembershipChanged: () => void = () => {};

export function setRoomMembershipChangedHandler(handler: () => void): void {
  onMembershipChanged = handler;
}

export function useMyRooms(status: RoomStatus) {
  return useInfiniteQuery({
    queryKey: queryKeys.myRooms(status),
    queryFn: ({ pageParam }) => listMyRooms(status, pageParam),
    initialPageParam: 0,
    getNextPageParam: nextPageParam,
  });
}

export function useRoom(id: string) {
  return useQuery({ queryKey: queryKeys.room(id), queryFn: () => getRoom(id) });
}

function useInvalidateRooms() {
  const queryClient = useQueryClient();
  return (id: string) =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.myRoomsPrefix }),
      queryClient.invalidateQueries({ queryKey: queryKeys.room(id) }),
    ]);
}

function useForgetRoom() {
  const queryClient = useQueryClient();
  const clearLivePositions = useLiveLocationsStore((state) => state.clearRoom);
  return (id: string) => {
    queryClient.removeQueries({ queryKey: queryKeys.room(id) });
    clearLivePositions(id);
    return queryClient.invalidateQueries({ queryKey: queryKeys.myRoomsPrefix });
  };
}

export function useCreateRoom() {
  const invalidate = useInvalidateRooms();
  return useMutation({
    mutationFn: createRoom,
    onSuccess: async (room) => {
      await invalidate(room.id);
      onMembershipChanged();
    },
  });
}

export function useJoinRoom() {
  const invalidate = useInvalidateRooms();
  return useMutation({
    mutationFn: joinRoom,
    onSuccess: async (room) => {
      await invalidate(room.id);
      onMembershipChanged();
    },
  });
}

export function useUpdateRoom(id: string) {
  const invalidate = useInvalidateRooms();
  return useMutation({
    mutationFn: (body: UpdateRoomRequest) => updateRoom(id, body),
    onSuccess: () => invalidate(id),
  });
}

export function useCloseRoom(id: string) {
  const invalidate = useInvalidateRooms();
  return useMutation({
    mutationFn: () => closeRoom(id),
    onSuccess: async () => {
      await invalidate(id);
      onMembershipChanged();
    },
  });
}

export function useDeleteRoom(id: string) {
  const forget = useForgetRoom();
  return useMutation({
    mutationFn: () => deleteRoom(id),
    onSuccess: async () => {
      await forget(id);
      onMembershipChanged();
    },
  });
}

export function useLeaveRoom(id: string) {
  const forget = useForgetRoom();
  return useMutation({
    mutationFn: () => leaveRoom(id),
    onSuccess: async () => {
      await forget(id);
      onMembershipChanged();
    },
  });
}
