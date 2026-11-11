import type { RoomRole, VisibilityMode } from '@/api/types';

const isGuardian = (role: RoomRole) => role !== 'MEMBER';

export function canSeePosition(
  mode: VisibilityMode,
  myRole: RoomRole,
  member: { role: RoomRole; isMe: boolean },
): boolean {
  return member.isMe || isGuardian(myRole) || mode === 'ALL' || isGuardian(member.role);
}
