import { colorForUser } from './colors';

test('colorForUser is deterministic and differs for different users', () => {
  const a = '0b1c7d1e-5a52-4d0c-9d5e-3c1f0a9d2e11';
  const b = '9f3a2b44-77d1-4c8e-b0a6-12ab34cd56ef';
  expect(colorForUser(a)).toBe(colorForUser(a));
  expect(colorForUser(a)).toMatch(/^#[0-9a-f]{6}$/);
  expect(colorForUser(a)).not.toBe(colorForUser(b));
});
