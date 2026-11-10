import type { Position, TrailPoint } from '@/realtime/types';
import { appendToTrail, mergePositions } from './positions';

const pos = (userId: string, recordedAt: string, lat = 1): Position => ({
  userId,
  lat,
  lon: 2,
  accuracyM: 5,
  recordedAt,
});

test('mergePositions keeps the newest position per user', () => {
  const base = [pos('a', '2026-01-01T10:00:00Z', 1), pos('b', '2026-01-01T10:00:00Z', 1)];
  const live = { a: pos('a', '2026-01-01T10:05:00Z', 2), b: pos('b', '2026-01-01T09:00:00Z', 3) };
  const merged = mergePositions(base, live);
  expect(merged).toHaveLength(2);
  expect(merged.find((p) => p.userId === 'a')?.lat).toBe(2);
  expect(merged.find((p) => p.userId === 'b')?.lat).toBe(1);
  expect(mergePositions(base, undefined)).toEqual(base);
});

test('appendToTrail ignores out-of-order points', () => {
  const trail: TrailPoint[] = [{ lat: 1, lon: 2, recordedAt: '2026-01-01T10:00:00Z' }];
  const older = pos('a', '2026-01-01T09:00:00Z');
  expect(appendToTrail(trail, older)).toBe(trail);
  expect(appendToTrail(trail, pos('a', '2026-01-01T10:00:00Z'))).toBe(trail);
  const next = appendToTrail(trail, pos('a', '2026-01-01T10:01:00Z', 7));
  expect(next).toEqual([...trail, { lat: 7, lon: 2, recordedAt: '2026-01-01T10:01:00Z' }]);
  expect(appendToTrail([], older)).toHaveLength(1);
});
