import type { RoomDetailsResponse, RoomStatus } from '@/api/types';

const MINUTE_MS = 60_000;
const MINUTES_PER_HOUR = 60;
const MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR;

export function formatDuration(ms: number): string {
  const totalMinutes = Math.floor(ms / MINUTE_MS);
  if (totalMinutes < 1) {
    return '< 1 min';
  }
  const days = Math.floor(totalMinutes / MINUTES_PER_DAY);
  const hours = Math.floor((totalMinutes % MINUTES_PER_DAY) / MINUTES_PER_HOUR);
  const minutes = totalMinutes % MINUTES_PER_HOUR;
  const parts: string[] = [];
  if (days > 0) {
    parts.push(`${days} d`);
    if (hours > 0) {
      parts.push(`${hours} h`);
    }
  } else {
    if (hours > 0) {
      parts.push(`${hours} h`);
    }
    if (minutes > 0) {
      parts.push(`${minutes} min`);
    }
  }
  return parts.join(' ');
}

export function statusLabel(status: RoomStatus): string {
  switch (status) {
    case 'SCHEDULED':
      return 'Zaplanowany';
    case 'ACTIVE':
      return 'Aktywny';
    case 'ENDED':
      return 'Zakończony';
  }
}

export function statusLine(
  room: Pick<RoomDetailsResponse, 'status' | 'startsAt' | 'endsAt'> & { closedAt?: string | null },
  now: Date,
): string {
  switch (room.status) {
    case 'SCHEDULED':
      return `Start za ${formatDuration(new Date(room.startsAt).getTime() - now.getTime())}`;
    case 'ACTIVE': {
      const end = room.closedAt != null ? room.closedAt : room.endsAt;
      return `Pozostało ${formatDuration(new Date(end).getTime() - now.getTime())}`;
    }
    case 'ENDED':
      return 'Zakończony';
  }
}
