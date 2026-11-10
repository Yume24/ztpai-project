import { queryKeys } from '@/api/queryKeys';
import { affectsTracking, clearsLivePositions, handleRealtimeMessage, keysToInvalidate } from './dispatch';
import { REALTIME_EVENT_TYPES, type Position, type RealtimeEventType, type RealtimeMessage } from './types';

const msg = (type: RealtimeEventType, payload: unknown = {}): RealtimeMessage => ({ type, roomId: 'r1', payload });

const ROOM_KEYS = [['rooms', 'r1'], ['me', 'rooms']];

test('keysToInvalidate maps every event type', () => {
  const expected: Record<RealtimeEventType, unknown[]> = {
    LOCATION_UPDATED: [],
    ALERT_CREATED: [['rooms', 'r1', 'alerts']],
    MEMBER_JOINED: ROOM_KEYS,
    MEMBER_LEFT: ROOM_KEYS,
    MEMBER_REMOVED: ROOM_KEYS,
    MEMBER_ROLE_CHANGED: ROOM_KEYS,
    ROOM_UPDATED: ROOM_KEYS,
    GEOFENCE_UPDATED: ROOM_KEYS,
    ROOM_STARTED: ROOM_KEYS,
    ROOM_ENDED: ROOM_KEYS,
  };
  expect(Object.keys(expected).sort()).toEqual([...REALTIME_EVENT_TYPES].sort());
  for (const type of REALTIME_EVENT_TYPES) {
    expect(keysToInvalidate(msg(type))).toEqual(expected[type]);
  }
});

test('affectsTracking is true only for ROOM_STARTED, ROOM_ENDED and MEMBER_REMOVED', () => {
  const affecting = REALTIME_EVENT_TYPES.filter((type) => affectsTracking(msg(type)));
  expect(affecting.sort()).toEqual(['MEMBER_REMOVED', 'ROOM_ENDED', 'ROOM_STARTED']);
});

test('clearsLivePositions is true only for events that can hide or end positions', () => {
  const clearing = REALTIME_EVENT_TYPES.filter((type) => clearsLivePositions(msg(type)));
  expect(clearing.sort()).toEqual(['MEMBER_LEFT', 'MEMBER_REMOVED', 'MEMBER_ROLE_CHANGED', 'ROOM_ENDED', 'ROOM_UPDATED']);
});

function makeDeps() {
  return { invalidate: jest.fn(), applyPosition: jest.fn(), clearRoom: jest.fn(), onTrackingRelevant: jest.fn() };
}

test('handleRealtimeMessage applies LOCATION_UPDATED to the live store without invalidating', () => {
  const deps = makeDeps();
  const position: Position = { userId: 'u1', lat: 1, lon: 2, accuracyM: 5, recordedAt: '2026-01-01T10:00:00Z' };
  handleRealtimeMessage(msg('LOCATION_UPDATED', position), deps);
  expect(deps.applyPosition).toHaveBeenCalledWith('r1', position);
  expect(deps.clearRoom).not.toHaveBeenCalled();
  expect(deps.invalidate).not.toHaveBeenCalled();
  expect(deps.onTrackingRelevant).not.toHaveBeenCalled();
});

test('handleRealtimeMessage clears the live positions of the room when visibility may have changed', () => {
  const deps = makeDeps();
  handleRealtimeMessage(msg('MEMBER_ROLE_CHANGED', { userId: 'u2' }), deps);
  expect(deps.clearRoom).toHaveBeenCalledWith('r1');
  expect(deps.invalidate).toHaveBeenCalledWith(queryKeys.room('r1'));
});

test('handleRealtimeMessage keeps live positions for events that hide nothing', () => {
  const deps = makeDeps();
  handleRealtimeMessage(msg('MEMBER_JOINED', { userId: 'u2' }), deps);
  handleRealtimeMessage(msg('GEOFENCE_UPDATED'), deps);
  expect(deps.clearRoom).not.toHaveBeenCalled();
});

test('handleRealtimeMessage ignores a malformed LOCATION_UPDATED payload', () => {
  const deps = makeDeps();
  handleRealtimeMessage(msg('LOCATION_UPDATED', { nope: true }), deps);
  expect(deps.applyPosition).not.toHaveBeenCalled();
});

test('handleRealtimeMessage rejects positions with an unparsable date or missing accuracy', () => {
  const deps = makeDeps();
  const valid = { userId: 'u1', lat: 1, lon: 2, accuracyM: 5, recordedAt: '2026-01-01T10:00:00Z' };
  handleRealtimeMessage(msg('LOCATION_UPDATED', { ...valid, recordedAt: 'yesterday' }), deps);
  handleRealtimeMessage(msg('LOCATION_UPDATED', { ...valid, accuracyM: undefined }), deps);
  expect(deps.applyPosition).not.toHaveBeenCalled();
});

test('handleRealtimeMessage invalidates room keys and notifies tracking', () => {
  const deps = makeDeps();
  handleRealtimeMessage(msg('ROOM_ENDED'), deps);
  expect(deps.invalidate).toHaveBeenCalledWith(queryKeys.room('r1'));
  expect(deps.invalidate).toHaveBeenCalledWith(queryKeys.myRoomsPrefix);
  expect(deps.onTrackingRelevant).toHaveBeenCalledTimes(1);
});
