import { useQuery } from '@tanstack/react-query';
// Uses the JS stack so the header sits above the content (the native stack let content slide under it).
import { Stack } from 'expo-router/js-stack';
import { useEffect } from 'react';
import { queryKeys } from '@/api/queryKeys';
import { getMe } from '@/auth/authApi';
import { useAuthStore } from '@/auth/authStore';
import { useTrackingBootstrap } from '@/location/useTrackingBootstrap';
import { useNotifications } from '@/notifications/useNotifications';
import { useRealtime } from '@/realtime/useRealtime';

// Keeps the room list underneath when a deep link opens a room screen directly.
export const unstable_settings = { anchor: '(tabs)' };

export default function AppLayout() {
  useRealtime();
  useTrackingBootstrap();
  useNotifications();
  const user = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);
  const { data } = useQuery({ queryKey: queryKeys.me, queryFn: getMe, enabled: user === null });

  useEffect(() => {
    if (data) {
      setUser(data);
    }
  }, [data, setUser]);

  return (
    <Stack screenOptions={{ headerBackButtonDisplayMode: 'minimal' }}>
      <Stack.Screen name="(tabs)" options={{ headerShown: false, title: 'Pokoje' }} />
      <Stack.Screen name="rooms/new" options={{ title: 'Nowy pokój' }} />
      <Stack.Screen name="rooms/join" options={{ title: 'Dołącz do pokoju' }} />
      <Stack.Screen name="rooms/[id]/index" options={{ title: 'Pokój' }} />
      <Stack.Screen name="rooms/[id]/settings" options={{ title: 'Ustawienia pokoju' }} />
      <Stack.Screen name="rooms/[id]/members" options={{ title: 'Członkowie' }} />
      <Stack.Screen name="rooms/[id]/geofence" options={{ title: 'Strefa' }} />
      <Stack.Screen name="rooms/[id]/alerts" options={{ title: 'Alerty' }} />
      <Stack.Screen name="location-permission" options={{ title: 'Lokalizacja' }} />
    </Stack>
  );
}
