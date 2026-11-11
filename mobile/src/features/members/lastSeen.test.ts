import { lastSeenLabel } from './lastSeen';

const now = new Date('2026-01-01T12:00:00Z');

test('lastSeenLabel shows "nie udostępnia", "przed chwilą" and minutes ago', () => {
  expect(lastSeenLabel(undefined, 'ACTIVE', now)).toBe('nie udostępnia');
  expect(lastSeenLabel('2026-01-01T11:59:40Z', 'ACTIVE', now)).toBe('przed chwilą');
  expect(lastSeenLabel('2026-01-01T11:55:00Z', 'ACTIVE', now)).toBe('ostatnio widziany 5 min temu');
  expect(lastSeenLabel('2026-01-01T10:30:00Z', 'ACTIVE', now)).toBe('ostatnio widziany 1 h 30 min temu');
});

test('lastSeenLabel is null when the room is not active', () => {
  expect(lastSeenLabel(undefined, 'SCHEDULED', now)).toBeNull();
  expect(lastSeenLabel('2026-01-01T11:55:00Z', 'ENDED', now)).toBeNull();
});
