import { canSeePosition } from './visibility';

test('in ALL mode everyone sees everyone', () => {
  expect(canSeePosition('ALL', 'MEMBER', { role: 'MEMBER', isMe: false })).toBe(true);
  expect(canSeePosition('ALL', 'MEMBER', { role: 'OWNER', isMe: false })).toBe(true);
  expect(canSeePosition('ALL', 'ADMIN', { role: 'MEMBER', isMe: false })).toBe(true);
});

test('in GUARDIANS_ONLY mode guardians see everyone and members see only guardians and themselves', () => {
  expect(canSeePosition('GUARDIANS_ONLY', 'OWNER', { role: 'MEMBER', isMe: false })).toBe(true);
  expect(canSeePosition('GUARDIANS_ONLY', 'ADMIN', { role: 'MEMBER', isMe: false })).toBe(true);
  expect(canSeePosition('GUARDIANS_ONLY', 'MEMBER', { role: 'OWNER', isMe: false })).toBe(true);
  expect(canSeePosition('GUARDIANS_ONLY', 'MEMBER', { role: 'ADMIN', isMe: false })).toBe(true);
  expect(canSeePosition('GUARDIANS_ONLY', 'MEMBER', { role: 'MEMBER', isMe: true })).toBe(true);
  expect(canSeePosition('GUARDIANS_ONLY', 'MEMBER', { role: 'MEMBER', isMe: false })).toBe(false);
});
