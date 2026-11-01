import { availableActions } from './permissions';

test('availableActions mirrors the room permission matrix', () => {
  expect(availableActions('OWNER', 'ACTIVE')).toEqual([
    'share',
    'settings',
    'geofence',
    'alerts',
    'members',
    'close',
    'delete',
  ]);
  expect(availableActions('OWNER', 'ACTIVE')).not.toContain('leave');
  expect(availableActions('MEMBER', 'ACTIVE')).toEqual(['share', 'leave']);
  expect(availableActions('ADMIN', 'ENDED')).toEqual(['alerts', 'members', 'leave']);
  expect(availableActions('OWNER', 'ENDED')).toEqual(['alerts', 'members', 'delete']);
  expect(availableActions('MEMBER', 'ENDED')).toEqual(['leave']);
});
