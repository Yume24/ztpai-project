import { loginSchema, profileSchema, registerSchema } from './schemas';

const valid = { email: 'ala@example.com', password: 'secret123', displayName: 'Ala' };

test('registerSchema accepts valid values', () => {
  expect(registerSchema.safeParse(valid).success).toBe(true);
});

test('registerSchema rejects password shorter than 8 chars', () => {
  expect(registerSchema.safeParse({ ...valid, password: '1234567' }).success).toBe(false);
});

test('registerSchema rejects 37 Polish ż letters (74 bytes) but accepts 36 (72 bytes)', () => {
  const tooLong = registerSchema.safeParse({ ...valid, password: 'ż'.repeat(37) });
  expect(tooLong.success).toBe(false);
  expect(tooLong.error?.issues[0].message).toBe('Hasło może mieć najwyżej 72 bajty.');
  expect(registerSchema.safeParse({ ...valid, password: 'ż'.repeat(36) }).success).toBe(true);
});

test('registerSchema trims displayName and rejects "  A  "', () => {
  const trimmed = registerSchema.safeParse({ ...valid, displayName: '  Ala  ' });
  expect(trimmed.data?.displayName).toBe('Ala');
  expect(registerSchema.safeParse({ ...valid, displayName: '  A  ' }).success).toBe(false);
});

test('registerSchema rejects displayName longer than 40 chars', () => {
  expect(registerSchema.safeParse({ ...valid, displayName: 'a'.repeat(41) }).success).toBe(false);
});

test('registerSchema rejects email longer than 254 chars', () => {
  const email = `${'a'.repeat(250)}@example.com`;
  expect(registerSchema.safeParse({ ...valid, email }).success).toBe(false);
});

test('loginSchema rejects malformed email', () => {
  expect(loginSchema.safeParse({ email: 'nope', password: 'x' }).success).toBe(false);
});

test('loginSchema rejects empty password', () => {
  expect(loginSchema.safeParse({ email: 'ala@example.com', password: '' }).success).toBe(false);
});

test('profileSchema trims displayName and enforces 2-40 chars', () => {
  expect(profileSchema.safeParse({ displayName: ' Ola ' }).data?.displayName).toBe('Ola');
  expect(profileSchema.safeParse({ displayName: ' O ' }).success).toBe(false);
  expect(profileSchema.safeParse({ displayName: 'a'.repeat(41) }).success).toBe(false);
});
