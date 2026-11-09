import type { PositionResponse, TrailPointResponse } from '@/api/types';

export const REALTIME_EVENT_TYPES = [
  'LOCATION_UPDATED',
  'MEMBER_JOINED',
  'MEMBER_LEFT',
  'MEMBER_REMOVED',
  'MEMBER_ROLE_CHANGED',
  'ROOM_UPDATED',
  'GEOFENCE_UPDATED',
  'ALERT_CREATED',
  'ROOM_STARTED',
  'ROOM_ENDED',
] as const;

export type RealtimeEventType = (typeof REALTIME_EVENT_TYPES)[number];

export type RealtimeMessage = { type: RealtimeEventType; roomId: string; payload: unknown };

export type Position = PositionResponse;
export type TrailPoint = TrailPointResponse;
