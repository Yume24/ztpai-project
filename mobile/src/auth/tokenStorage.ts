import * as SecureStore from 'expo-secure-store';

export type Tokens = { accessToken: string; refreshToken: string };

const ACCESS_KEY = 'accessToken';
const REFRESH_KEY = 'refreshToken';
const PUSH_KEY = 'pushToken';

// AFTER_FIRST_UNLOCK lets the background location task read tokens while the screen is locked.
const OPTIONS: SecureStore.SecureStoreOptions = {
  keychainAccessible: SecureStore.AFTER_FIRST_UNLOCK,
};

export async function getTokens(): Promise<Tokens | null> {
  const [accessToken, refreshToken] = await Promise.all([
    SecureStore.getItemAsync(ACCESS_KEY, OPTIONS),
    SecureStore.getItemAsync(REFRESH_KEY, OPTIONS),
  ]);
  return accessToken && refreshToken ? { accessToken, refreshToken } : null;
}

export async function saveTokens(tokens: Tokens): Promise<void> {
  await Promise.all([
    SecureStore.setItemAsync(ACCESS_KEY, tokens.accessToken, OPTIONS),
    SecureStore.setItemAsync(REFRESH_KEY, tokens.refreshToken, OPTIONS),
  ]);
}

export async function clearTokens(): Promise<void> {
  await Promise.all([
    SecureStore.deleteItemAsync(ACCESS_KEY, OPTIONS),
    SecureStore.deleteItemAsync(REFRESH_KEY, OPTIONS),
  ]);
}

export function getPushToken(): Promise<string | null> {
  return SecureStore.getItemAsync(PUSH_KEY, OPTIONS);
}

export async function savePushToken(token: string): Promise<void> {
  await SecureStore.setItemAsync(PUSH_KEY, token, OPTIONS);
}
