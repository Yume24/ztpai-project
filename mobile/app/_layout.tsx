import '@/location/backgroundTask';
import { QueryClientProvider } from '@tanstack/react-query';
import * as Notifications from 'expo-notifications';
import { Stack, useRouter } from 'expo-router';
import { useEffect } from 'react';
import { queryClient } from '@/api/queryClient';
import { usePendingJoin } from '@/auth/pendingJoin';
import { useAuthStore } from '@/auth/authStore';
import { Loading } from '@/ui/Loading';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

export default function RootLayout() {
  const status = useAuthStore((state) => state.status);
  const hydrate = useAuthStore((state) => state.hydrate);
  const router = useRouter();

  useEffect(() => {
    void hydrate();
  }, [hydrate]);

  useEffect(() => {
    if (status !== 'signedIn') {
      return;
    }
    const code = usePendingJoin.getState().take();
    if (code) {
      router.replace({ pathname: '/rooms/join', params: { code } });
    }
  }, [status, router]);

  if (status === 'loading') {
    return <Loading />;
  }

  return (
    <QueryClientProvider client={queryClient}>
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Protected guard={status === 'signedIn'}>
          <Stack.Screen name="(app)" />
        </Stack.Protected>
        <Stack.Protected guard={status === 'signedOut'}>
          <Stack.Screen name="login" />
          <Stack.Screen name="register" />
        </Stack.Protected>
      </Stack>
    </QueryClientProvider>
  );
}
