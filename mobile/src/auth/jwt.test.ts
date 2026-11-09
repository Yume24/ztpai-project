import { isExpiringSoon } from './jwt';

function tokenExpiringAt(expSeconds: number): string {
  const payload = btoa(JSON.stringify({ exp: expSeconds })).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `header.${payload}.signature`;
}

const NOW = 1_700_000_000_000;

test('isExpiringSoon detects tokens expiring within a minute and malformed tokens', () => {
  expect(isExpiringSoon(tokenExpiringAt(NOW / 1000 + 30), NOW)).toBe(true);
  expect(isExpiringSoon(tokenExpiringAt(NOW / 1000 - 30), NOW)).toBe(true);
  expect(isExpiringSoon(tokenExpiringAt(NOW / 1000 + 600), NOW)).toBe(false);
  expect(isExpiringSoon(tokenExpiringAt(NOW / 1000 + 600), NOW, 20 * 60_000)).toBe(true);
  expect(isExpiringSoon('garbage', NOW)).toBe(true);
  expect(isExpiringSoon('a.!!!.c', NOW)).toBe(true);
  expect(isExpiringSoon(`a.${btoa('{}')}.c`, NOW)).toBe(true);
});

test('isExpiringSoon decodes unpadded base64url payloads containing - and _', () => {
  const json = JSON.stringify({ exp: 4_000_000_000, n: '>>>???' });
  const base64 = btoa(json);
  const payload = base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  expect(payload).toMatch(/[-_]/);
  expect(payload.length % 4).not.toBe(0);
  expect(isExpiringSoon(`h.${payload}.s`, NOW)).toBe(false);
});
