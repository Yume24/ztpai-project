import { formatDuration, statusLabel, statusLine } from './format';

const MIN = 60_000;
const HOUR = 60 * MIN;

test('formatDuration formats minutes, hours and days in Polish', () => {
  expect(formatDuration(30_000)).toBe('< 1 min');
  expect(formatDuration(45 * MIN)).toBe('45 min');
  expect(formatDuration(2 * HOUR + 15 * MIN)).toBe('2 h 15 min');
  expect(formatDuration(3 * HOUR)).toBe('3 h');
  expect(formatDuration(28 * HOUR)).toBe('1 d 4 h');
});

test('statusLine shows countdown to start, time left and ended', () => {
  const now = new Date('2026-11-15T10:00:00Z');
  const base = { startsAt: '2026-11-15T12:00:00Z', endsAt: '2026-11-15T15:30:00Z' };

  expect(statusLine({ ...base, status: 'SCHEDULED' }, now)).toBe('Start za 2 h');
  expect(statusLine({ ...base, status: 'ACTIVE' }, now)).toBe('Pozostało 5 h 30 min');
  expect(statusLine({ ...base, status: 'ACTIVE', closedAt: '2026-11-15T10:45:00Z' }, now)).toBe(
    'Pozostało 45 min',
  );
  expect(statusLine({ ...base, status: 'ACTIVE', closedAt: null }, now)).toBe(
    'Pozostało 5 h 30 min',
  );
  expect(statusLine({ ...base, status: 'ENDED' }, now)).toBe('Zakończony');
});

test('statusLabel maps statuses to Polish labels', () => {
  expect(statusLabel('SCHEDULED')).toBe('Zaplanowany');
  expect(statusLabel('ACTIVE')).toBe('Aktywny');
  expect(statusLabel('ENDED')).toBe('Zakończony');
});
