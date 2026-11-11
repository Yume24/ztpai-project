import { memberActions } from './memberActions';

test('memberActions follows the owner/admin rules and never targets self or owner', () => {
  expect(memberActions('OWNER', { role: 'MEMBER', isMe: false })).toEqual(['promote', 'remove']);
  expect(memberActions('OWNER', { role: 'ADMIN', isMe: false })).toEqual(['demote', 'remove']);
  expect(memberActions('ADMIN', { role: 'MEMBER', isMe: false })).toEqual(['remove']);
  expect(memberActions('ADMIN', { role: 'ADMIN', isMe: false })).toEqual([]);
  expect(memberActions('MEMBER', { role: 'MEMBER', isMe: false })).toEqual([]);
  expect(memberActions('OWNER', { role: 'OWNER', isMe: false })).toEqual([]);
  expect(memberActions('ADMIN', { role: 'OWNER', isMe: false })).toEqual([]);
  expect(memberActions('OWNER', { role: 'ADMIN', isMe: true })).toEqual([]);
  expect(memberActions('ADMIN', { role: 'MEMBER', isMe: true })).toEqual([]);
});
