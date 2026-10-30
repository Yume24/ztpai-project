// Expo inlines EXPO_PUBLIC_* only for the literal `process.env.X` form.
const apiUrl = process.env.EXPO_PUBLIC_API_URL;

if (!apiUrl) {
  throw new Error('Brak EXPO_PUBLIC_API_URL w .env');
}

export const API_URL: string = apiUrl;

export function wsUrlFrom(url: string): string {
  return `${url.replace(/\/+$/, '').replace(/^http/, 'ws')}/ws`;
}

export const WS_URL: string = wsUrlFrom(API_URL);
