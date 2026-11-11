import { api, unwrap } from '@/api/client';
import type { ChangeRoleRequest, MemberResponse } from '@/api/types';

export async function listMembers(id: string): Promise<MemberResponse[]> {
  return unwrap(await api.GET('/api/rooms/{id}/members', { params: { path: { id } } }));
}

export async function changeRole(id: string, userId: string, body: ChangeRoleRequest): Promise<void> {
  await unwrap(await api.PATCH('/api/rooms/{id}/members/{userId}', { params: { path: { id, userId } }, body }));
}

export async function removeMember(id: string, userId: string): Promise<void> {
  await unwrap(await api.DELETE('/api/rooms/{id}/members/{userId}', { params: { path: { id, userId } } }));
}
