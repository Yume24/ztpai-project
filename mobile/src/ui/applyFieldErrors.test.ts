import { toApiError } from '@/api/errors';
import { applyFieldErrors } from './applyFieldErrors';

test('applyFieldErrors marks every rejected field with a Polish message', () => {
  const setError = jest.fn();
  const error = toApiError(400, {
    code: 'VALIDATION_FAILED',
    errors: [
      { field: 'email', message: 'must be a well-formed email address' },
      { field: 'displayName', message: 'size must be between 2 and 40' },
    ],
  });

  applyFieldErrors(error, setError);

  expect(setError.mock.calls).toEqual([
    ['email', { message: 'Nieprawidłowa wartość.' }],
    ['displayName', { message: 'Nieprawidłowa wartość.' }],
  ]);
});

test('applyFieldErrors ignores errors that are not from the API', () => {
  const setError = jest.fn();
  applyFieldErrors(new TypeError('Network request failed'), setError);
  expect(setError).not.toHaveBeenCalled();
});
