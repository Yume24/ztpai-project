import type { Href } from 'expo-router';
import { isRecord } from '@/utils/isRecord';

export type NotificationRoute = { href: Href; refreshTracking: boolean };

export function routeForNotification(data: unknown): NotificationRoute | null {
  if (!isRecord(data)) {
    return null;
  }
  const { type, roomId } = data;
  switch (type) {
    case 'ROOM_STARTED':
      return typeof roomId === 'string' && roomId !== ''
        ? { href: { pathname: '/rooms/[id]', params: { id: roomId } }, refreshTracking: true }
        : null;
    case 'ALERT':
      return typeof roomId === 'string' && roomId !== ''
        ? { href: { pathname: '/rooms/[id]', params: { id: roomId } }, refreshTracking: false }
        : null;
    case 'MEMBER_REMOVED':
      return { href: '/', refreshTracking: true };
    default:
      return null;
  }
}
