import { wsUrlFrom } from './config';

test('wsUrlFrom maps http to ws and appends /ws', () => {
  expect(wsUrlFrom('http://10.0.2.2:8080')).toBe('ws://10.0.2.2:8080/ws');
});

test('wsUrlFrom maps https to wss and tolerates trailing slash', () => {
  expect(wsUrlFrom('https://api.example.com/')).toBe('wss://api.example.com/ws');
});
