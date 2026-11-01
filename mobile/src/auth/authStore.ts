import { create } from 'zustand';
import { setSessionExpiredHandler } from '@/api/authFetch';
import { ApiError } from '@/api/errors';
import { queryClient } from '@/api/queryClient';
import type { AuthResponse, LogoutRequest, UserResponse } from '@/api/types';
import { getMe, logout } from './authApi';
import * as tokenStorage from './tokenStorage';

type AuthState = {
  status: 'loading' | 'signedOut' | 'signedIn';
  user: UserResponse | null;
  hydrate: () => Promise<void>;
  signIn: (response: AuthResponse) => Promise<void>;
  signOut: () => Promise<void>;
  setUser: (user: UserResponse) => void;
};

async function hasSessionEnded(error: unknown): Promise<boolean> {
  if (!(error instanceof ApiError)) {
    return false;
  }
  const { status } = error;
  if (status < 400 || status >= 500 || status === 408 || status === 429) {
    return false;
  }
  return status !== 401 || (await tokenStorage.getTokens()) === null;
}

async function logoutRequest(): Promise<LogoutRequest | null> {
  const tokens = await tokenStorage.getTokens();
  if (!tokens) {
    return null;
  }
  const pushToken = await tokenStorage.getPushToken();
  return { refreshToken: tokens.refreshToken, pushToken: pushToken ?? undefined };
}

async function clearLocalSession(): Promise<void> {
  try {
    await tokenStorage.clearTokens();
  } catch (error) {
    console.warn('Deleting the stored tokens failed', error);
  } finally {
    queryClient.clear();
    useAuthStore.setState({ status: 'signedOut', user: null });
  }
}

export const useAuthStore = create<AuthState>((set) => ({
  status: 'loading',
  user: null,

  hydrate: async () => {
    const tokens = await tokenStorage.getTokens();
    if (!tokens) {
      set({ status: 'signedOut', user: null });
      return;
    }
    try {
      set({ status: 'signedIn', user: await getMe() });
    } catch (error) {
      set((await hasSessionEnded(error)) ? { status: 'signedOut', user: null } : { status: 'signedIn', user: null });
    }
  },

  signIn: async ({ accessToken, refreshToken, user }) => {
    await tokenStorage.saveTokens({ accessToken, refreshToken });
    set({ status: 'signedIn', user });
  },

  signOut: async () => {
    const request = await logoutRequest().catch((error: unknown) => {
      console.warn('Reading the session for logout failed', error);
      return null;
    });
    await clearLocalSession();
    if (request) {
      logout(request).catch((error: unknown) => console.warn('Logout request failed', error));
    }
  },

  setUser: (user) => set({ user }),
}));

setSessionExpiredHandler(() => void clearLocalSession());
