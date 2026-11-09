import { refreshTokensOnce } from '@/api/authFetch';
import { createRealtimeClient } from './realtimeClient';
import type { RealtimeMessage } from './types';

jest.mock('@/api/authFetch', () => ({ refreshTokensOnce: jest.fn() }));

type FakeSocket = {
  onopen: (() => void) | null;
  onmessage: ((event: { data: unknown }) => void) | null;
  onclose: (() => void) | null;
  close: jest.Mock;
};

const FRESH_TOKEN = `h.${btoa(JSON.stringify({ exp: 4_000_000_000 }))}.s`;

const EXPIRING_TOKEN = `h.${btoa(JSON.stringify({ exp: 1 }))}.s`;

function setup(getAccessToken: () => Promise<string | null> = async () => FRESH_TOKEN) {
  const sockets: FakeSocket[] = [];
  const onMessage = jest.fn<void, [RealtimeMessage]>();
  const onReconnected = jest.fn();
  const createSocket = jest.fn((): WebSocket => {
    const socket: FakeSocket = { onopen: null, onmessage: null, onclose: null, close: jest.fn() };
    sockets.push(socket);
    return socket as unknown as WebSocket;
  });
  const client = createRealtimeClient({
    url: 'ws://host/ws',
    getAccessToken,
    onMessage,
    onReconnected,
    createSocket,
  });
  return { client, sockets, createSocket, onMessage, onReconnected };
}

async function flush() {
  await jest.advanceTimersByTimeAsync(0);
}

beforeEach(() => jest.useFakeTimers());
afterEach(() => jest.useRealTimers());

test('client reconnects after an unexpected close and reports onReconnected', async () => {
  const { client, sockets, createSocket, onReconnected } = setup();
  client.connect();
  await flush();
  expect(createSocket).toHaveBeenCalledWith('ws://host/ws', FRESH_TOKEN);
  sockets[0].onopen?.();
  expect(onReconnected).not.toHaveBeenCalled();

  sockets[0].onclose?.();
  await jest.advanceTimersByTimeAsync(999);
  expect(sockets).toHaveLength(1);
  await jest.advanceTimersByTimeAsync(1);
  expect(sockets).toHaveLength(2);

  sockets[1].onclose?.();
  await jest.advanceTimersByTimeAsync(1999);
  expect(sockets).toHaveLength(2);
  await jest.advanceTimersByTimeAsync(1);
  expect(sockets).toHaveLength(3);

  sockets[2].onopen?.();
  expect(onReconnected).toHaveBeenCalledTimes(1);

  sockets[2].onclose?.();
  await jest.advanceTimersByTimeAsync(1000);
  expect(sockets).toHaveLength(4);
});

test('client forwards parsed messages and drops invalid ones', async () => {
  const { client, sockets, onMessage } = setup();
  client.connect();
  await flush();
  sockets[0].onmessage?.({ data: 'garbage' });
  sockets[0].onmessage?.({ data: JSON.stringify({ type: 'ROOM_ENDED', roomId: 'r1', payload: null }) });
  expect(onMessage).toHaveBeenCalledTimes(1);
  expect(onMessage).toHaveBeenCalledWith({ type: 'ROOM_ENDED', roomId: 'r1', payload: null });
});

test('disconnect closes the socket, clears the pending timer and never reconnects', async () => {
  const { client, sockets } = setup();
  client.connect();
  await flush();
  sockets[0].onopen?.();
  sockets[0].onclose?.();
  client.disconnect();
  await jest.advanceTimersByTimeAsync(60_000);
  expect(sockets).toHaveLength(1);
  expect(jest.getTimerCount()).toBe(0);
});

test('disconnect closes the open socket and ignores its late close event', async () => {
  const { client, sockets } = setup();
  client.connect();
  await flush();
  const old = sockets[0];
  client.disconnect();
  expect(old.close).toHaveBeenCalled();
  old.onclose?.();
  await jest.advanceTimersByTimeAsync(60_000);
  expect(sockets).toHaveLength(1);
});

test('handlers of a replaced socket do nothing', async () => {
  const { client, sockets, onMessage } = setup();
  client.connect();
  await flush();
  sockets[0].onclose?.();
  await jest.advanceTimersByTimeAsync(1000);
  expect(sockets).toHaveLength(2);
  sockets[0].onclose?.();
  sockets[0].onmessage?.({ data: JSON.stringify({ type: 'ROOM_ENDED', roomId: 'r1', payload: null }) });
  await jest.advanceTimersByTimeAsync(60_000);
  expect(sockets).toHaveLength(2);
  expect(onMessage).not.toHaveBeenCalled();
});

test('an expiring token is replaced by the refreshed one', async () => {
  jest.mocked(refreshTokensOnce).mockResolvedValue({ accessToken: 'refreshed', refreshToken: 'r' });
  const { client, createSocket } = setup(async () => EXPIRING_TOKEN);
  client.connect();
  await flush();
  expect(createSocket).toHaveBeenCalledWith('ws://host/ws', 'refreshed');
});

test('no socket is opened with an expiring token when the refresh fails; it retries later', async () => {
  jest.mocked(refreshTokensOnce).mockResolvedValue(null);
  const { client, createSocket } = setup(async () => EXPIRING_TOKEN);
  client.connect();
  await flush();
  expect(createSocket).not.toHaveBeenCalled();
  jest.mocked(refreshTokensOnce).mockResolvedValue({ accessToken: 'refreshed', refreshToken: 'r' });
  await jest.advanceTimersByTimeAsync(1000);
  expect(createSocket).toHaveBeenCalledWith('ws://host/ws', 'refreshed');
});

test('a throwing createSocket is logged and schedules a retry instead of stopping', async () => {
  const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  const { client, createSocket, sockets } = setup();
  const failure = new Error('boom');
  createSocket.mockImplementationOnce(() => {
    throw failure;
  });
  client.connect();
  await flush();
  expect(sockets).toHaveLength(0);
  expect(warn).toHaveBeenCalledWith('Realtime socket creation failed', failure);
  await jest.advanceTimersByTimeAsync(1000);
  expect(sockets).toHaveLength(1);
  warn.mockRestore();
});

test('a failing token lookup is logged and retried later', async () => {
  const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  const failure = new Error('keychain locked');
  const getAccessToken = jest
    .fn<Promise<string | null>, []>()
    .mockRejectedValueOnce(failure)
    .mockResolvedValue(FRESH_TOKEN);
  const { client, createSocket } = setup(getAccessToken);
  client.connect();
  await flush();
  expect(createSocket).not.toHaveBeenCalled();
  expect(warn).toHaveBeenCalledWith('Realtime token lookup failed', failure);
  await jest.advanceTimersByTimeAsync(1000);
  expect(createSocket).toHaveBeenCalledWith('ws://host/ws', FRESH_TOKEN);
  warn.mockRestore();
});

test('a token that resolves after disconnect opens no socket', async () => {
  let resolveToken: (token: string) => void = () => {};
  const { client, createSocket } = setup(() => new Promise<string>((resolve) => (resolveToken = resolve)));
  client.connect();
  client.disconnect();
  resolveToken(FRESH_TOKEN);
  await jest.advanceTimersByTimeAsync(60_000);
  expect(createSocket).not.toHaveBeenCalled();
});
