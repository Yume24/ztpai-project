import type { FieldValues, Path, UseFormSetError } from 'react-hook-form';
import { ApiError } from '@/api/errors';

const INVALID_FIELD_MESSAGE = 'Nieprawidłowa wartość.';

export function applyFieldErrors<T extends FieldValues>(error: unknown, setError: UseFormSetError<T>): void {
  if (!(error instanceof ApiError)) {
    return;
  }
  for (const field of Object.keys(error.fieldErrors)) {
    setError(field as Path<T>, { message: INVALID_FIELD_MESSAGE });
  }
}
