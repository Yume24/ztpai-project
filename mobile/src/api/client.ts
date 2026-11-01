import createClient from 'openapi-fetch';
import { API_URL } from '@/config';
import { authFetch } from './authFetch';
import { toApiError } from './errors';
import type { paths } from './schema';

export const api = createClient<paths>({ baseUrl: API_URL, fetch: authFetch });

export async function unwrap<T>(result: {
  data?: T;
  error?: unknown;
  response: Response;
}): Promise<T> {
  if (!result.response.ok) {
    throw toApiError(result.response.status, result.error);
  }
  // A 2xx without data only occurs for void endpoints, where T is void.
  return result.data as T;
}
