import { QueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/api/queryKeys';
import type { Position } from '@/realtime/types';
import { appendLivePosition } from './trailCache';

const clients: QueryClient[] = [];
const newClient = () => {
  const client = new QueryClient();
  clients.push(client);
  return client;
};
afterEach(() => clients.splice(0).forEach((client) => client.clear()));

const pos = (recordedAt: string, lat: number): Position => ({ userId: 'u1', lat, lon: 2, accuracyM: 5, recordedAt });

test('appendLivePosition accumulates points in the cached trail and skips stale ones', () => {
  const queryClient = newClient();
  const key = queryKeys.trail('r1', 'u1');
  queryClient.setQueryData(key, [{ lat: 1, lon: 2, recordedAt: '2026-01-01T10:00:00Z' }]);

  appendLivePosition(queryClient, 'r1', pos('2026-01-01T10:01:00Z', 3));
  appendLivePosition(queryClient, 'r1', pos('2026-01-01T10:02:00Z', 4));
  appendLivePosition(queryClient, 'r1', pos('2026-01-01T10:01:00Z', 9));

  expect(queryClient.getQueryData<{ lat: number }[]>(key)?.map((p) => p.lat)).toEqual([1, 3, 4]);
});

test('appendLivePosition leaves a trail that is not loaded yet untouched', () => {
  const queryClient = newClient();
  appendLivePosition(queryClient, 'r1', pos('2026-01-01T10:01:00Z', 3));
  expect(queryClient.getQueryData(queryKeys.trail('r1', 'u1'))).toBeUndefined();
});
