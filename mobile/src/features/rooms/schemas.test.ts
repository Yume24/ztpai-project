import { joinSchema, roomFormSchema, toCreateRoomRequest } from './schemas';

test('joinSchema normalizes " abc234 " to "ABC234" and rejects codes with I, O, 0 or 1', () => {
  expect(joinSchema.parse({ code: ' abc234 ' })).toEqual({ code: 'ABC234' });
  for (const code of ['ABCI23', 'ABCO23', 'ABC023', 'ABC123', 'ABC23', 'ABC2345']) {
    const result = joinSchema.safeParse({ code });
    expect(result.success).toBe(false);
    expect(result.error?.issues[0].message).toBe('Kod ma 6 znaków.');
  }
});

test('roomFormSchema rejects end before start and toCreateRoomRequest omits startsAt when "now"', () => {
  const start = new Date('2030-01-01T10:00:00Z');
  const before = roomFormSchema.safeParse({ name: 'Pokój', startsAt: start, endsAt: new Date('2030-01-01T09:00:00Z') });
  expect(before.success).toBe(false);
  expect(before.error?.issues[0].message).toBe('Koniec musi być po początku.');

  const past = roomFormSchema.safeParse({ name: 'Pokój', startsAt: null, endsAt: new Date(Date.now() - 1000) });
  expect(past.success).toBe(false);

  const end = new Date('2030-01-01T12:00:00Z');
  const scheduled = roomFormSchema.parse({ name: '  Pokój  ', startsAt: start, endsAt: end });
  expect(scheduled.name).toBe('Pokój');
  expect(toCreateRoomRequest(scheduled)).toEqual({
    name: 'Pokój',
    startsAt: '2030-01-01T10:00:00.000Z',
    endsAt: '2030-01-01T12:00:00.000Z',
  });

  const immediate = roomFormSchema.parse({ name: 'Pokój', startsAt: null, endsAt: new Date(Date.now() + 3_600_000) });
  expect(toCreateRoomRequest(immediate)).not.toHaveProperty('startsAt');
  expect(roomFormSchema.safeParse({ name: '   ', startsAt: null, endsAt: end }).success).toBe(false);
});
