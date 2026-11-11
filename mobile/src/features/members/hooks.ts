import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/api/queryKeys';
import type { ChangeRoleRequest } from '@/api/types';
import { changeRole, listMembers, removeMember } from './api';

export function useMembers(roomId: string) {
  return useQuery({ queryKey: queryKeys.members(roomId), queryFn: () => listMembers(roomId) });
}

function useInvalidateMembers(roomId: string) {
  const queryClient = useQueryClient();
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.members(roomId) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.room(roomId) }),
    ]);
}

export function useChangeRole(roomId: string) {
  const invalidate = useInvalidateMembers(roomId);
  return useMutation({
    mutationFn: ({ userId, body }: { userId: string; body: ChangeRoleRequest }) =>
      changeRole(roomId, userId, body),
    onSuccess: invalidate,
  });
}

export function useRemoveMember(roomId: string) {
  const invalidate = useInvalidateMembers(roomId);
  return useMutation({
    mutationFn: (userId: string) => removeMember(roomId, userId),
    onSuccess: invalidate,
  });
}
