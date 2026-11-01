import { queryClient } from '@/api/queryClient';
import { queryKeys } from '@/api/queryKeys';
import * as tokenStorage from '@/auth/tokenStorage';
import { setSessionExpiredHandler } from '@/api/authFetch';
import { api } from '@/api/client';
import { useAuthStore } from './authStore';

jest.mock('@/auth/tokenStorage');
jest.mock('@/api/authFetch', () => ({ setSessionExpiredHandler: jest.fn() }));
jest.mock('@/api/client', () => ({
  ...jest.requireActual('@/api/client'),
  api: { GET: jest.fn(), POST: jest.fn() },
}));

const storage = jest.mocked(tokenStorage);
const GET = jest.mocked(api.GET);
const POST = jest.mocked(api.POST);

const expireSession = jest.mocked(setSessionExpiredHandler).mock.calls[0][0];

const TOKENS = { accessToken: 'access', refreshToken: 'refresh' };
const USER = { id: 'u1', email: 'ala@example.com', displayName: 'Ala', role: 'USER' };

function ok(data: unknown) {
  return { data, response: new Response(null, { status: 200 }) } as never;
}

const unauthorized = () => ({ error: {}, response: new Response(null, { status: 401 }) }) as never;

const settle = () => new Promise<void>((resolve) => setTimeout(resolve, 0));

beforeEach(() => {
  jest.resetAllMocks();
  storage.clearTokens.mockResolvedValue(undefined);
  storage.saveTokens.mockResolvedValue(undefined);
  useAuthStore.setState({ status: 'loading', user: null });
});

test('hydrate signs in when tokens exist and /api/me succeeds', async () => {
  storage.getTokens.mockResolvedValue(TOKENS);
  GET.mockResolvedValue(ok(USER));

  await useAuthStore.getState().hydrate();

  expect(GET).toHaveBeenCalledWith('/api/me');
  expect(useAuthStore.getState()).toMatchObject({ status: 'signedIn', user: USER });
});

test('hydrate signs out when there are no tokens', async () => {
  storage.getTokens.mockResolvedValue(null);

  await useAuthStore.getState().hydrate();

  expect(GET).not.toHaveBeenCalled();
  expect(useAuthStore.getState()).toMatchObject({ status: 'signedOut', user: null });
});

test('hydrate keeps the session without a user on a network error', async () => {
  storage.getTokens.mockResolvedValue(TOKENS);
  GET.mockRejectedValue(new TypeError('Network request failed'));

  await useAuthStore.getState().hydrate();

  expect(useAuthStore.getState()).toMatchObject({ status: 'signedIn', user: null });
});

test('hydrate signs out when /api/me is rejected with 401 and the refresher deleted the tokens', async () => {
  storage.getTokens.mockResolvedValueOnce(TOKENS).mockResolvedValueOnce(null);
  GET.mockResolvedValue(unauthorized());

  await useAuthStore.getState().hydrate();

  expect(useAuthStore.getState()).toMatchObject({ status: 'signedOut', user: null });
});

test('hydrate keeps the session without a user when a 401 follows a transient refresh failure', async () => {
  storage.getTokens.mockResolvedValue(TOKENS);
  GET.mockResolvedValue(unauthorized());

  await useAuthStore.getState().hydrate();

  expect(useAuthStore.getState()).toMatchObject({ status: 'signedIn', user: null });
});

test('signIn stores tokens and the user', async () => {
  await useAuthStore.getState().signIn({ ...TOKENS, user: USER } as never);

  expect(storage.saveTokens).toHaveBeenCalledWith(TOKENS);
  expect(useAuthStore.getState()).toMatchObject({ status: 'signedIn', user: USER });
});

test('signOut sends refresh and push tokens to logout and logs a failed request', async () => {
  const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  storage.getTokens.mockResolvedValue(TOKENS);
  storage.getPushToken.mockResolvedValue('ExponentPushToken[abc]');
  POST.mockRejectedValue(new Error('offline'));
  useAuthStore.setState({ status: 'signedIn', user: USER as never });
  queryClient.setQueryData(queryKeys.me, USER);

  await useAuthStore.getState().signOut();
  await settle();

  expect(POST).toHaveBeenCalledWith('/api/auth/logout', {
    body: { refreshToken: 'refresh', pushToken: 'ExponentPushToken[abc]' },
  });
  expect(storage.clearTokens).toHaveBeenCalled();
  expect(queryClient.getQueryData(queryKeys.me)).toBeUndefined();
  expect(useAuthStore.getState()).toMatchObject({ status: 'signedOut', user: null });
  expect(warn).toHaveBeenCalledWith('Logout request failed', expect.any(Error));
  warn.mockRestore();
});

test('signOut ends the local session without waiting for the logout request', async () => {
  storage.getTokens.mockResolvedValue(TOKENS);
  storage.getPushToken.mockResolvedValue(null);
  POST.mockReturnValue(new Promise(() => {}));
  useAuthStore.setState({ status: 'signedIn', user: USER as never });

  await useAuthStore.getState().signOut();

  expect(POST).toHaveBeenCalledWith('/api/auth/logout', { body: { refreshToken: 'refresh', pushToken: undefined } });
  expect(useAuthStore.getState()).toMatchObject({ status: 'signedOut', user: null });
});

test('session expiry handler signs out locally without calling logout', async () => {
  useAuthStore.setState({ status: 'signedIn', user: USER as never });

  expireSession();
  await settle();

  expect(POST).not.toHaveBeenCalled();
  expect(useAuthStore.getState()).toMatchObject({ status: 'signedOut', user: null });
});

test('session expiry resets the local state even when deleting the tokens fails', async () => {
  const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  storage.clearTokens.mockRejectedValue(new Error('keychain'));
  useAuthStore.setState({ status: 'signedIn', user: USER as never });
  queryClient.setQueryData(queryKeys.me, USER);

  expireSession();
  await settle();

  expect(queryClient.getQueryData(queryKeys.me)).toBeUndefined();
  expect(useAuthStore.getState()).toMatchObject({ status: 'signedOut', user: null });
  expect(warn).toHaveBeenCalledWith('Deleting the stored tokens failed', expect.any(Error));
  warn.mockRestore();
});

test('signOut clears the local session even when reading tokens fails', async () => {
  const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  storage.getTokens.mockRejectedValue(new Error('keychain'));

  await useAuthStore.getState().signOut();

  expect(storage.clearTokens).toHaveBeenCalled();
  expect(useAuthStore.getState().status).toBe('signedOut');
  expect(POST).not.toHaveBeenCalled();
  expect(warn).toHaveBeenCalledWith('Reading the session for logout failed', expect.any(Error));
  warn.mockRestore();
});
