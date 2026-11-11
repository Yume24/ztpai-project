import type { RoomRole } from '@/api/types';

export type MemberAction = 'promote' | 'demote' | 'remove';

export function memberActions(myRole: RoomRole, target: { role: RoomRole; isMe: boolean }): MemberAction[] {
  if (target.isMe || target.role === 'OWNER') {
    return [];
  }
  const actions: MemberAction[] = [];
  if (myRole === 'OWNER') {
    actions.push(target.role === 'MEMBER' ? 'promote' : 'demote');
  }
  if (myRole === 'OWNER' || (myRole === 'ADMIN' && target.role === 'MEMBER')) {
    actions.push('remove');
  }
  return actions;
}
