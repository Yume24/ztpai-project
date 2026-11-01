import type { RoomStatus, AlertType } from './types';

const myRoomsPrefix = ['me', 'rooms'] as const;
const roomsPrefix = ['rooms'] as const;
const adminUsersPrefix = ['admin', 'users'] as const;
const adminRoomsPrefix = ['admin', 'rooms'] as const;

export const queryKeys = {
  me: ['me', 'profile'],
  myRoomsPrefix,
  roomsPrefix,
  adminUsersPrefix,
  adminRoomsPrefix,
  myRooms: (status?: RoomStatus) => [...myRoomsPrefix, status ?? 'ALL'],
  room: (id: string) => ['rooms', id],
  alertsPrefix: (id: string) => ['rooms', id, 'alerts'],
  members: (id: string) => ['rooms', id, 'members'],
  latest: (id: string) => ['rooms', id, 'latest'],
  trail: (id: string, userId: string) => ['rooms', id, 'trail', userId],
  alerts: (id: string, type?: AlertType) => ['rooms', id, 'alerts', type ?? 'ALL'],
  adminUsers: (q: string, blocked?: boolean) => [...adminUsersPrefix, q, blocked ?? 'ALL'],
  adminRooms: (filters: { status?: RoomStatus; from?: string; to?: string }) => [...adminRoomsPrefix, filters],
} as const;
