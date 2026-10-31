import { createAuth } from './authFetch';

const REFRESH_URL = 'http://api.test/api/auth/refresh';
const OLD = { accessToken: 'old-access', refreshToken: 'old-refresh' };
const NEW = { accessToken: 'new-access', refreshToken: 'new-refresh' };

function json(status: number, body: unknown = {}): Response {
  return new Response(JSON.stringify(body), { status });
}

function setup(baseFetch: jest.Mock) {
  const storage = {
    getTokens: jest.fn().mockResolvedValue(OLD),
    saveTokens: jest.fn().mockResolvedValue(undefined),
    clearTokens: jest.fn().mockResolvedValue(undefined),
  };
  const onSessionExpired = jest.fn();
  const { authFetch, refreshTokensOnce } = createAuth({
    baseFetch: baseFetch as unknown as typeof fetch,
    storage,
    refreshUrl: REFRESH_URL,
    onSessionExpired,
  });
  return { authFetch, refreshTokensOnce, storage, onSessionExpired };
}

function request(path: string, init?: RequestInit): Request {
  return new Request(`http://api.test${path}`, init);
}

function sentRequests(baseFetch: jest.Mock, path: string): Request[] {
  return baseFetch.mock.calls
    .map(([arg]) => arg)
    .filter((arg): arg is Request => arg instanceof Request && new URL(arg.url).pathname === path);
}

test('adds bearer token to api requests', async () => {
  const baseFetch = jest.fn().mockResolvedValue(json(200));
  const { authFetch } = setup(baseFetch);

  await authFetch(request('/api/rooms/1'));

  expect(baseFetch).toHaveBeenCalledTimes(1);
  expect(baseFetch.mock.calls[0][0].headers.get('Authorization')).toBe('Bearer old-access');
});

test('does not attach token or refresh for /api/auth/ requests', async () => {
  const baseFetch = jest.fn().mockResolvedValue(json(401));
  const { authFetch, storage } = setup(baseFetch);

  const response = await authFetch(request('/api/auth/login', { method: 'POST', body: '{}' }));

  expect(response.status).toBe(401);
  expect(baseFetch).toHaveBeenCalledTimes(1);
  expect(baseFetch.mock.calls[0][0].headers.get('Authorization')).toBeNull();
  expect(storage.getTokens).not.toHaveBeenCalled();
});

test('on 401 refreshes once and retries the original request with the new token', async () => {
  const baseFetch = jest.fn(async (input: Request | string, _init?: RequestInit) => {
    if (typeof input === 'string') {
      return json(200, NEW);
    }
    const bearer = input.headers.get('Authorization');
    return bearer === 'Bearer new-access' ? json(200, { ok: true }) : json(401);
  });
  const { authFetch, storage } = setup(baseFetch);

  const response = await authFetch(request('/api/rooms', { method: 'POST', body: '{"name":"x"}' }));

  expect(response.status).toBe(200);
  expect(storage.saveTokens).toHaveBeenCalledWith(NEW);
  const refreshCall = baseFetch.mock.calls.find(([arg]) => arg === REFRESH_URL);
  expect(refreshCall?.[1]).toMatchObject({
    method: 'POST',
    body: JSON.stringify({ refreshToken: 'old-refresh' }),
  });
  const sent = sentRequests(baseFetch, '/api/rooms');
  expect(sent).toHaveLength(2);
  expect(await sent[1].text()).toBe('{"name":"x"}');
});

test('concurrent 401s share a single refresh call', async () => {
  const baseFetch = jest.fn(async (input: Request | string) => {
    if (typeof input === 'string') {
      return json(200, NEW);
    }
    return input.headers.get('Authorization') === 'Bearer new-access' ? json(200) : json(401);
  });
  const { authFetch } = setup(baseFetch);

  const responses = await Promise.all([
    authFetch(request('/api/rooms/1')),
    authFetch(request('/api/rooms/2')),
    authFetch(request('/api/rooms/3')),
  ]);

  expect(responses.map((r) => r.status)).toEqual([200, 200, 200]);
  expect(baseFetch.mock.calls.filter(([arg]) => arg === REFRESH_URL)).toHaveLength(1);
  const retries = baseFetch.mock.calls.filter(
    ([arg]) => arg instanceof Request && arg.headers.get('Authorization') === 'Bearer new-access',
  );
  expect(retries).toHaveLength(3);
});

test('failed refresh clears tokens, calls onSessionExpired and returns the 401', async () => {
  const baseFetch = jest.fn(async (input: Request | string) =>
    typeof input === 'string' ? json(401) : json(401, { code: 'UNAUTHORIZED' }),
  );
  const { authFetch, storage, onSessionExpired } = setup(baseFetch);

  const response = await authFetch(request('/api/rooms/1'));

  expect(response.status).toBe(401);
  expect(storage.clearTokens).toHaveBeenCalledTimes(1);
  expect(onSessionExpired).toHaveBeenCalledTimes(1);
  expect(storage.saveTokens).not.toHaveBeenCalled();
});

test('network error during refresh keeps the session and returns the 401', async () => {
  const baseFetch = jest.fn(async (input: Request | string, _init?: RequestInit) => {
    if (typeof input === 'string') {
      throw new TypeError('Network request failed');
    }
    return json(401);
  });
  const { authFetch, storage, onSessionExpired } = setup(baseFetch);

  const response = await authFetch(request('/api/rooms/1'));

  expect(response.status).toBe(401);
  expect(storage.clearTokens).not.toHaveBeenCalled();
  expect(storage.saveTokens).not.toHaveBeenCalled();
  expect(onSessionExpired).not.toHaveBeenCalled();
});

test('a later 401 starts a new refresh after a network failure', async () => {
  const baseFetch = jest.fn(async (input: Request | string, _init?: RequestInit) => {
    if (typeof input === 'string') {
      throw new TypeError('Network request failed');
    }
    return json(401);
  });
  const { authFetch } = setup(baseFetch);

  await authFetch(request('/api/rooms/1'));
  await authFetch(request('/api/rooms/2'));

  expect(baseFetch.mock.calls.filter(([arg]) => arg === REFRESH_URL)).toHaveLength(2);
});

test('a transient refresh failure (503) keeps the tokens and returns the 401', async () => {
  const baseFetch = jest.fn(async (input: Request | string, _init?: RequestInit) =>
    typeof input === 'string' ? json(503) : json(401),
  );
  const { authFetch, storage, onSessionExpired } = setup(baseFetch);

  const response = await authFetch(request('/api/rooms/1'));

  expect(response.status).toBe(401);
  expect(storage.clearTokens).not.toHaveBeenCalled();
  expect(onSessionExpired).not.toHaveBeenCalled();
});

test('a 2xx refresh response without a valid token pair expires the session', async () => {
  const baseFetch = jest.fn(async (input: Request | string, _init?: RequestInit) =>
    typeof input === 'string' ? json(200, { accessToken: 'only-access' }) : json(401),
  );
  const { authFetch, storage, onSessionExpired } = setup(baseFetch);

  const response = await authFetch(request('/api/rooms/1'));

  expect(response.status).toBe(401);
  expect(storage.clearTokens).toHaveBeenCalledTimes(1);
  expect(onSessionExpired).toHaveBeenCalledTimes(1);
});

test('a storage failure does not reject the refresh and is logged', async () => {
  const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  const baseFetch = jest.fn(async (input: Request | string, _init?: RequestInit) =>
    typeof input === 'string' ? json(200, NEW) : json(401),
  );
  const { authFetch, storage } = setup(baseFetch);
  const failure = new Error('keychain locked');
  storage.saveTokens.mockRejectedValue(failure);

  const response = await authFetch(request('/api/rooms/1'));

  expect(response.status).toBe(401);
  expect(warn).toHaveBeenCalledWith('Token refresh failed', failure);
  warn.mockRestore();
});

test('a rejected refresh ends the session even when deleting the tokens fails', async () => {
  const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  const baseFetch = jest.fn(async (_input: Request | string, _init?: RequestInit) => json(401));
  const { authFetch, storage, onSessionExpired } = setup(baseFetch);
  storage.clearTokens.mockRejectedValue(new Error('keychain locked'));

  const response = await authFetch(request('/api/rooms/1'));

  expect(response.status).toBe(401);
  expect(onSessionExpired).toHaveBeenCalledTimes(1);
  expect(warn).toHaveBeenCalledWith('Token refresh failed', expect.any(Error));
  warn.mockRestore();
});

test('refreshTokensOnce shares the in-flight refresh started by a 401', async () => {
  let releaseRefresh: () => void = () => {};
  const refreshGate = new Promise<void>((resolve) => {
    releaseRefresh = resolve;
  });
  const baseFetch = jest.fn(async (input: Request | string, _init?: RequestInit) => {
    if (typeof input === 'string') {
      await refreshGate;
      return json(200, NEW);
    }
    return input.headers.get('Authorization') === 'Bearer new-access' ? json(200) : json(401);
  });
  const { authFetch, refreshTokensOnce } = setup(baseFetch);

  const pending = authFetch(request('/api/rooms/1'));
  await waitForRefreshCall(baseFetch);
  const shared = refreshTokensOnce();
  releaseRefresh();

  expect(await shared).toEqual(NEW);
  expect((await pending).status).toBe(200);
  expect(baseFetch.mock.calls.filter(([arg]) => arg === REFRESH_URL)).toHaveLength(1);
});

async function waitForRefreshCall(baseFetch: jest.Mock): Promise<void> {
  while (!baseFetch.mock.calls.some(([arg]) => arg === REFRESH_URL)) {
    await new Promise((resolve) => setTimeout(resolve, 0));
  }
}
