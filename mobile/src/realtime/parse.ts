import { isRecord } from '@/utils/isRecord';
import { REALTIME_EVENT_TYPES, type RealtimeEventType, type RealtimeMessage } from './types';

function isEventType(value: unknown): value is RealtimeEventType {
  return REALTIME_EVENT_TYPES.some((type) => type === value);
}

export function parseRealtimeMessage(raw: string): RealtimeMessage | null {
  let data: unknown;
  try {
    data = JSON.parse(raw);
  } catch {
    return null;
  }
  if (!isRecord(data)) {
    return null;
  }
  const { type, roomId, payload } = data;
  if (!isEventType(type) || typeof roomId !== 'string') {
    return null;
  }
  return { type, roomId, payload };
}
