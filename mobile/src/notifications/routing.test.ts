import { routeForNotification } from './routing';

const ID = '8f14e45f-ceea-4672-9d6b-0c2a3f5a1b11';

test('routeForNotification maps ROOM_STARTED, ALERT and MEMBER_REMOVED and ignores unknown data', () => {
  expect(routeForNotification({ type: 'ROOM_STARTED', roomId: ID })).toEqual({
    href: { pathname: '/rooms/[id]', params: { id: ID } },
    refreshTracking: true,
  });
  expect(routeForNotification({ type: 'ALERT', roomId: ID })).toEqual({
    href: { pathname: '/rooms/[id]', params: { id: ID } },
    refreshTracking: false,
  });
  expect(routeForNotification({ type: 'MEMBER_REMOVED', roomId: ID })).toEqual({
    href: '/',
    refreshTracking: true,
  });
  expect(routeForNotification({ type: 'SOMETHING_ELSE', roomId: ID })).toBeNull();
  expect(routeForNotification({ type: 'ROOM_STARTED' })).toBeNull();
  expect(routeForNotification({ type: 'ALERT', roomId: 42 })).toBeNull();
  expect(routeForNotification({ roomId: ID })).toBeNull();
  expect(routeForNotification(null)).toBeNull();
  expect(routeForNotification(undefined)).toBeNull();
  expect(routeForNotification('ALERT')).toBeNull();
});
