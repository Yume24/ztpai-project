import { ApiError, errorMessage, toApiError, type ErrorCode } from './errors';

const GENERIC = 'Coś poszło nie tak. Spróbuj ponownie.';

const ALL_CODES: ErrorCode[] = [
  'VALIDATION_FAILED',
  'INVALID_TIME_WINDOW',
  'UNAUTHORIZED',
  'INVALID_CREDENTIALS',
  'INVALID_REFRESH_TOKEN',
  'FORBIDDEN',
  'USER_BLOCKED',
  'MEMBER_REMOVED',
  'ROOM_ACTION_FORBIDDEN',
  'ADMIN_ACTION_FORBIDDEN',
  'USER_NOT_FOUND',
  'ROOM_NOT_FOUND',
  'MEMBER_NOT_FOUND',
  'EMAIL_TAKEN',
  'ROOM_ENDED',
  'ROOM_FULL',
  'ROOM_VERSION_CONFLICT',
];

test('toApiError reads code, detail and field errors from ProblemDetail', () => {
  const error = toApiError(400, {
    status: 400,
    code: 'VALIDATION_FAILED',
    detail: 'Request validation failed.',
    errors: [
      { field: 'email', message: 'must be a well-formed email address' },
      { field: 'password', message: 'size must be between 8 and 72' },
    ],
  });

  expect(error).toBeInstanceOf(ApiError);
  expect(error.status).toBe(400);
  expect(error.code).toBe('VALIDATION_FAILED');
  expect(error.message).toBe('Request validation failed.');
  expect(error.fieldErrors).toEqual({
    email: 'must be a well-formed email address',
    password: 'size must be between 8 and 72',
  });
});

test('errorMessage has a Polish message for every backend error code', () => {
  expect(ALL_CODES).toHaveLength(17);
  for (const code of ALL_CODES) {
    const message = errorMessage(toApiError(400, { code, detail: 'x' }));
    expect(message).not.toBe(GENERIC);
  }
});

test('errorMessage falls back to the generic message for network errors', () => {
  expect(errorMessage(new TypeError('Network request failed'))).toBe(GENERIC);
  expect(errorMessage(toApiError(500, null))).toBe(GENERIC);
  expect(errorMessage(toApiError(400, { code: 'SOMETHING_NEW' }))).toBe(GENERIC);
});
