import type { Position } from '@/realtime/types';
import { useLiveLocationsStore } from './liveLocationsStore';

const pos = (recordedAt: string, lat: number): Position => ({ userId: 'u1', lat, lon: 2, accuracyM: 5, recordedAt });

beforeEach(() => useLiveLocationsStore.setState({ byRoom: {} }));

test('applyPosition ignores older positions', () => {
  const { applyPosition } = useLiveLocationsStore.getState();
  applyPosition('r1', pos('2026-01-01T10:00:00Z', 1));
  applyPosition('r1', pos('2026-01-01T09:00:00Z', 2));
  expect(useLiveLocationsStore.getState().byRoom.r1.u1.lat).toBe(1);
  applyPosition('r1', pos('2026-01-01T11:00:00Z', 3));
  expect(useLiveLocationsStore.getState().byRoom.r1.u1.lat).toBe(3);
});

test('clearRoom removes only that room', () => {
  const { applyPosition, clearRoom } = useLiveLocationsStore.getState();
  applyPosition('r1', pos('2026-01-01T10:00:00Z', 1));
  applyPosition('r2', pos('2026-01-01T10:00:00Z', 1));
  clearRoom('r1');
  expect(Object.keys(useLiveLocationsStore.getState().byRoom)).toEqual(['r2']);
});

test('reset removes every room', () => {
  const { applyPosition, reset } = useLiveLocationsStore.getState();
  applyPosition('r1', pos('2026-01-01T10:00:00Z', 1));
  applyPosition('r2', pos('2026-01-01T10:00:00Z', 1));
  reset();
  expect(useLiveLocationsStore.getState().byRoom).toEqual({});
});
