import { reconnectDelay } from './backoff';

test('reconnectDelay doubles from 1s and caps at 30s', () => {
  expect([0, 1, 2, 3, 4, 5, 6].map(reconnectDelay)).toEqual([1000, 2000, 4000, 8000, 16000, 30000, 30000]);
});
