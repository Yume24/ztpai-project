import type { RoomDetailsResponse } from '@/api/types';
import { settingsSchema, toSettingsValues, toUpdateRoomRequest } from './settingsSchema';

const room = (status: RoomDetailsResponse['status']): RoomDetailsResponse => ({
  id: 'r1',
  name: 'Wycieczka',
  code: 'ABCDEF',
  status,
  startsAt: '2026-11-20T10:00:00Z',
  endsAt: '2026-11-20T14:00:00Z',
  visibilityMode: 'ALL',
  membersSeeTrails: false,
  version: 7,
  myRole: 'OWNER',
  memberCount: 3,
  owner: { id: 'u1', displayName: 'Ola' },
});

test('toUpdateRoomRequest always sends version and sends startsAt only for scheduled rooms', () => {
  const scheduled = room('SCHEDULED');
  const unchanged = toSettingsValues(scheduled);
  expect(toUpdateRoomRequest(unchanged, scheduled)).toEqual({ version: 7 });

  const moved = { ...unchanged, startsAt: new Date('2026-11-20T11:00:00Z'), name: 'Nowa' };
  expect(toUpdateRoomRequest(moved, scheduled)).toEqual({
    version: 7,
    name: 'Nowa',
    startsAt: '2026-11-20T11:00:00.000Z',
  });

  const active = room('ACTIVE');
  expect(toUpdateRoomRequest(moved, active)).toEqual({ version: 7, name: 'Nowa' });
});

test('toUpdateRoomRequest sends only the changed end, visibility and trails fields', () => {
  const active = room('ACTIVE');
  const values = {
    ...toSettingsValues(active),
    endsAt: new Date('2026-11-20T15:00:00Z'),
    visibilityMode: 'GUARDIANS_ONLY' as const,
    membersSeeTrails: true,
  };
  expect(toUpdateRoomRequest(values, active)).toEqual({
    version: 7,
    endsAt: '2026-11-20T15:00:00.000Z',
    visibilityMode: 'GUARDIANS_ONLY',
    membersSeeTrails: true,
  });
});

test('settingsSchema trims the name and rejects empty or too long ones', () => {
  const values = toSettingsValues(room('ACTIVE'));
  expect(settingsSchema.parse({ ...values, name: '  Nowa  ' }).name).toBe('Nowa');
  expect(settingsSchema.safeParse({ ...values, name: '   ' }).success).toBe(false);
  expect(settingsSchema.safeParse({ ...values, name: 'x'.repeat(61) }).success).toBe(false);
});
