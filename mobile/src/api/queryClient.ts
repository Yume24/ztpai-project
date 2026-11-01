import { QueryClient, focusManager } from '@tanstack/react-query';
import { AppState } from 'react-native';
import { ApiError } from './errors';

const MAX_RETRIES = 2;

focusManager.setEventListener((setFocused) => {
  const subscription = AppState.addEventListener('change', (state) => setFocused(state === 'active'));
  return () => subscription.remove();
});

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        const isClientError = error instanceof ApiError && error.status >= 400 && error.status < 500;
        return !isClientError && failureCount < MAX_RETRIES;
      },
    },
  },
});
