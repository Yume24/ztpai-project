import { isRecord } from '@/utils/isRecord';

const ERROR_MESSAGES = {
  VALIDATION_FAILED: 'Sprawdź poprawność danych.',
  INVALID_TIME_WINDOW:
    'Nieprawidłowy czas trwania pokoju (od 15 min do 72 h, start najpóźniej za 30 dni).',
  UNAUTHORIZED: 'Sesja wygasła. Zaloguj się ponownie.',
  INVALID_CREDENTIALS: 'Nieprawidłowy e-mail lub hasło.',
  INVALID_REFRESH_TOKEN: 'Sesja wygasła. Zaloguj się ponownie.',
  FORBIDDEN: 'Brak uprawnień.',
  USER_BLOCKED: 'Konto zostało zablokowane.',
  MEMBER_REMOVED: 'Usunięto cię z tego pokoju.',
  ROOM_ACTION_FORBIDDEN: 'Nie masz uprawnień do tej akcji w pokoju.',
  ADMIN_ACTION_FORBIDDEN: 'Nie można wykonać tej akcji na koncie administratora.',
  USER_NOT_FOUND: 'Nie znaleziono użytkownika.',
  ROOM_NOT_FOUND: 'Nie znaleziono pokoju.',
  MEMBER_NOT_FOUND: 'Ta osoba nie jest już w pokoju.',
  EMAIL_TAKEN: 'Konto z tym adresem e-mail już istnieje.',
  ROOM_ENDED: 'Pokój już się zakończył.',
  ROOM_FULL: 'Pokój jest pełny (maks. 50 osób).',
  ROOM_VERSION_CONFLICT:
    'Ktoś w międzyczasie zmienił ustawienia. Odświeżono dane — spróbuj ponownie.',
} as const;

export type ErrorCode = keyof typeof ERROR_MESSAGES;

const GENERIC_MESSAGE = 'Coś poszło nie tak. Spróbuj ponownie.';

function isErrorCode(value: unknown): value is ErrorCode {
  return typeof value === 'string' && Object.hasOwn(ERROR_MESSAGES, value);
}

export class ApiError extends Error {
  readonly status: number;
  readonly code?: ErrorCode;
  readonly fieldErrors: Record<string, string>;

  constructor(
    status: number,
    message: string,
    code?: ErrorCode,
    fieldErrors: Record<string, string> = {},
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

export function toApiError(status: number, body: unknown): ApiError {
  const problem: Record<string, unknown> = isRecord(body) ? body : {};
  const code = isErrorCode(problem.code) ? problem.code : undefined;
  const detail = typeof problem.detail === 'string' ? problem.detail : `HTTP ${status}`;

  const fieldErrors: Record<string, string> = {};
  if (Array.isArray(problem.errors)) {
    for (const entry of problem.errors) {
      if (isRecord(entry) && typeof entry.field === 'string' && typeof entry.message === 'string') {
        fieldErrors[entry.field] = entry.message;
      }
    }
  }

  return new ApiError(status, detail, code, fieldErrors);
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError && error.code) {
    return ERROR_MESSAGES[error.code];
  }
  return GENERIC_MESSAGE;
}
