import { inviteMessage } from './invite';

test('inviteMessage includes name, link and code', () => {
  expect(inviteMessage('Wycieczka', 'ABC234', 'rendezvous://join/ABC234')).toBe(
    'Dołącz do pokoju „Wycieczka” w Rendezvous: rendezvous://join/ABC234 (kod: ABC234)',
  );
});
