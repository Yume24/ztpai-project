import type { QueryKey } from '@tanstack/react-query';
import { queryKeys } from '@/api/queryKeys';
import { isRecord } from '@/utils/isRecord';
import type { Position, RealtimeMessage } from './types';

export type DispatchDeps = {
  invalidate: (key: QueryKey) => void;
  applyPosition: (roomId: string, p: Position) => void;
  clearRoom: (roomId: string) => void;
  onTrackingRelevant: () => void;
};

export function keysToInvalidate(msg: RealtimeMessage): QueryKey[] {
  switch (msg.type) {
    case 'LOCATION_UPDATED':
      return [];
    case 'ALERT_CREATED':
      return [queryKeys.alertsPrefix(msg.roomId)];
    default:
      return [queryKeys.room(msg.roomId), queryKeys.myRoomsPrefix];
  }
}

export function affectsTracking(msg: RealtimeMessage): boolean {
  return msg.type === 'ROOM_STARTED' || msg.type === 'ROOM_ENDED' || msg.type === 'MEMBER_REMOVED';
}

export function clearsLivePositions(msg: RealtimeMessage): boolean {
  switch (msg.type) {
    case 'MEMBER_LEFT':
    case 'MEMBER_REMOVED':
    case 'MEMBER_ROLE_CHANGED':
    case 'ROOM_UPDATED':
    case 'ROOM_ENDED':
      return true;
    default:
      return false;
  }
}

function isPosition(value: unknown): value is Position {
  return (
    isRecord(value) &&
    typeof value.userId === 'string' &&
    typeof value.lat === 'number' &&
    typeof value.lon === 'number' &&
    typeof value.accuracyM === 'number' &&
    typeof value.recordedAt === 'string' &&
    !Number.isNaN(Date.parse(value.recordedAt))
  );
}

export function handleRealtimeMessage(msg: RealtimeMessage, deps: DispatchDeps): void {
  if (msg.type === 'LOCATION_UPDATED' && isPosition(msg.payload)) {
    deps.applyPosition(msg.roomId, msg.payload);
  }
  if (clearsLivePositions(msg)) {
    deps.clearRoom(msg.roomId);
  }
  for (const key of keysToInvalidate(msg)) {
    deps.invalidate(key);
  }
  if (affectsTracking(msg)) {
    deps.onTrackingRelevant();
  }
}
