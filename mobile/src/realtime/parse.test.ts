import { parseRealtimeMessage } from './parse';

test('parseRealtimeMessage returns null for bad JSON and unknown types', () => {
  expect(parseRealtimeMessage('not json')).toBeNull();
  expect(parseRealtimeMessage('null')).toBeNull();
  expect(parseRealtimeMessage('"text"')).toBeNull();
  expect(parseRealtimeMessage(JSON.stringify({ type: 'SOMETHING_ELSE', roomId: 'r1', payload: {} }))).toBeNull();
  expect(parseRealtimeMessage(JSON.stringify({ type: 'ROOM_ENDED', payload: {} }))).toBeNull();
});

test('parseRealtimeMessage parses a valid message', () => {
  expect(parseRealtimeMessage(JSON.stringify({ type: 'ROOM_ENDED', roomId: 'r1', payload: { a: 1 } }))).toEqual({
    type: 'ROOM_ENDED',
    roomId: 'r1',
    payload: { a: 1 },
  });
});
