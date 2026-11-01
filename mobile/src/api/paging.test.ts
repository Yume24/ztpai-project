import { nextPageParam } from './paging';

test('nextPageParam returns next page until totalPages is reached', () => {
  expect(nextPageParam({ page: 0, totalPages: 2 })).toBe(1);
  expect(nextPageParam({ page: 1, totalPages: 2 })).toBeUndefined();
  expect(nextPageParam({ page: 0, totalPages: 0 })).toBeUndefined();
});
