import { refreshTokensOnce } from '@/api/authFetch';
import { isExpiringSoon } from '@/auth/jwt';
import { reconnectDelay } from './backoff';
import { parseRealtimeMessage } from './parse';
import type { RealtimeMessage } from './types';

export type RealtimeClientDeps = {
  url: string;
  getAccessToken: () => Promise<string | null>;
  onMessage: (message: RealtimeMessage) => void;
  onReconnected: () => void;
  createSocket?: (url: string, token: string) => WebSocket;
};

// React Native's WebSocket takes a third `options` argument that the DOM lib type lacks.
type NativeWebSocketConstructor = new (
  url: string,
  protocols: string | string[] | undefined,
  options: { headers: Record<string, string> },
) => WebSocket;
const NativeWebSocket: NativeWebSocketConstructor = WebSocket;

const defaultCreateSocket = (url: string, token: string): WebSocket =>
  new NativeWebSocket(url, undefined, { headers: { Authorization: `Bearer ${token}` } });

export function createRealtimeClient({
  url,
  getAccessToken,
  onMessage,
  onReconnected,
  createSocket = defaultCreateSocket,
}: RealtimeClientDeps) {
  let running = false;
  let socket: WebSocket | null = null;
  let timer: ReturnType<typeof setTimeout> | null = null;
  let attempt = 0;
  let reconnecting = false;
  let generation = 0;

  async function freshAccessToken(): Promise<string | null> {
    const token = await getAccessToken();
    if (token && isExpiringSoon(token, Date.now())) {
      return (await refreshTokensOnce())?.accessToken ?? null;
    }
    return token;
  }

  function scheduleReconnect() {
    reconnecting = true;
    timer = setTimeout(() => {
      timer = null;
      void open();
    }, reconnectDelay(attempt));
    attempt += 1;
  }

  async function open() {
    const current = generation;
    let token: string | null = null;
    try {
      token = await freshAccessToken();
    } catch (error) {
      console.warn('Realtime token lookup failed', error);
    }
    if (current !== generation) {
      return;
    }
    if (!token) {
      scheduleReconnect();
      return;
    }

    let next: WebSocket;
    try {
      next = createSocket(url, token);
    } catch (error) {
      console.warn('Realtime socket creation failed', error);
      scheduleReconnect();
      return;
    }
    socket = next;
    next.onopen = () => {
      if (socket !== next) {
        return;
      }
      attempt = 0;
      if (reconnecting) {
        reconnecting = false;
        onReconnected();
      }
    };
    next.onmessage = (event) => {
      if (socket !== next || typeof event.data !== 'string') {
        return;
      }
      const message = parseRealtimeMessage(event.data);
      if (message) {
        onMessage(message);
      }
    };
    next.onclose = () => {
      if (socket !== next) {
        return;
      }
      socket = null;
      scheduleReconnect();
    };
  }

  return {
    connect(): void {
      if (running) {
        return;
      }
      running = true;
      void open();
    },

    disconnect(): void {
      running = false;
      generation += 1;
      attempt = 0;
      reconnecting = false;
      if (timer) {
        clearTimeout(timer);
        timer = null;
      }
      const closing = socket;
      socket = null;
      closing?.close();
    },
  };
}
