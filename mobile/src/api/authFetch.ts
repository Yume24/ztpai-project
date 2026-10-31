import { API_URL } from '@/config';
import * as tokenStorage from '@/auth/tokenStorage';
import type { Tokens } from '@/auth/tokenStorage';

type Storage = Pick<typeof tokenStorage, 'getTokens' | 'saveTokens' | 'clearTokens'>;

type AuthFetchDeps = {
  baseFetch: typeof fetch;
  storage: Storage;
  refreshUrl: string;
  onSessionExpired: () => void;
};

const AUTH_PATH = '/api/auth/';

export function createTokenRefresher({
  baseFetch,
  storage,
  refreshUrl,
  onSessionExpired,
}: AuthFetchDeps): () => Promise<Tokens | null> {
  let inFlight: Promise<Tokens | null> | null = null;

  async function expireSession(): Promise<null> {
    try {
      await storage.clearTokens();
    } finally {
      onSessionExpired();
    }
    return null;
  }

  async function refresh(): Promise<Tokens | null> {
    const current = await storage.getTokens();
    if (!current) {
      return null;
    }

    let response: Response;
    try {
      response = await baseFetch(refreshUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: current.refreshToken }),
      });
    } catch {
      return null;
    }

    if (!response.ok) {
      const rejected = response.status >= 400 && response.status < 500;
      const transient = response.status === 408 || response.status === 429;
      return rejected && !transient ? expireSession() : null;
    }

    const next = await response.json().then(
      (body: Partial<Tokens> | null) =>
        body?.accessToken && body.refreshToken
          ? { accessToken: body.accessToken, refreshToken: body.refreshToken }
          : null,
      () => null,
    );
    if (!next) {
      return expireSession();
    }
    await storage.saveTokens(next);
    return next;
  }

  return function refreshOnce(): Promise<Tokens | null> {
    inFlight ??= refresh()
      .catch((error: unknown) => {
        console.warn('Token refresh failed', error);
        return null;
      })
      .finally(() => {
        inFlight = null;
      });
    return inFlight;
  };
}

export function createAuth(deps: AuthFetchDeps) {
  const refreshTokensOnce = createTokenRefresher(deps);
  const { baseFetch, storage } = deps;

  async function authFetch(input: Request): Promise<Response> {
    if (new URL(input.url).pathname.startsWith(AUTH_PATH)) {
      return baseFetch(input);
    }

    const retry = input.clone();

    const tokens = await storage.getTokens();
    if (tokens) {
      input.headers.set('Authorization', `Bearer ${tokens.accessToken}`);
    }

    const response = await baseFetch(input);
    if (response.status !== 401 || !tokens) {
      return response;
    }

    const refreshed = await refreshTokensOnce();
    if (!refreshed) {
      return response;
    }
    retry.headers.set('Authorization', `Bearer ${refreshed.accessToken}`);
    return baseFetch(retry);
  }

  return { authFetch, refreshTokensOnce };
}

let sessionExpiredHandler: () => void = () => {};

export function setSessionExpiredHandler(handler: () => void): void {
  sessionExpiredHandler = handler;
}

const deps: AuthFetchDeps = {
  baseFetch: (...args) => fetch(...args),
  storage: tokenStorage,
  refreshUrl: `${API_URL}${AUTH_PATH}refresh`,
  onSessionExpired: () => sessionExpiredHandler(),
};

export const { authFetch, refreshTokensOnce } = createAuth(deps);
