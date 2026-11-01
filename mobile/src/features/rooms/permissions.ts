import type { RoomRole, RoomStatus } from '@/api/types';

export type RoomAction =
  | 'share'
  | 'settings'
  | 'geofence'
  | 'alerts'
  | 'members'
  | 'leave'
  | 'close'
  | 'delete';

export function availableActions(role: RoomRole, status: RoomStatus): RoomAction[] {
  const running = status !== 'ENDED';
  const guardian = role !== 'MEMBER';
  const owner = role === 'OWNER';
  const actions: [RoomAction, boolean][] = [
    ['share', running],
    ['settings', guardian && running],
    ['geofence', guardian && running],
    ['alerts', guardian],
    ['members', guardian],
    ['leave', !owner],
    ['close', owner && running],
    ['delete', owner],
  ];
  return actions.filter(([, allowed]) => allowed).map(([action]) => action);
}
