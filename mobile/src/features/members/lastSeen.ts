import type { RoomStatus } from '@/api/types';
import { formatDuration } from '@/features/rooms/format';

const MINUTE_MS = 60_000;

export function lastSeenLabel(recordedAt: string | undefined, roomStatus: RoomStatus, now: Date): string | null {
  if (roomStatus !== 'ACTIVE') {
    return null;
  }
  if (recordedAt === undefined) {
    return 'nie udostępnia';
  }
  const elapsed = now.getTime() - new Date(recordedAt).getTime();
  if (elapsed < MINUTE_MS) {
    return 'przed chwilą';
  }
  return `ostatnio widziany ${formatDuration(elapsed)} temu`;
}
