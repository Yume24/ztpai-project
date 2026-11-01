import { usePendingJoin } from './pendingJoin';

test('take returns the stored code once and clears it', () => {
  usePendingJoin.getState().setCode('ABC123');

  expect(usePendingJoin.getState().take()).toBe('ABC123');
  expect(usePendingJoin.getState().take()).toBeNull();
});
