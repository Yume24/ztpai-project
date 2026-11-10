import { useEffect } from 'react';
import { queryKeys } from '@/api/queryKeys';
import { queryClient } from '@/api/queryClient';
import { useAuthStore } from '@/auth/authStore';
import { getTokens } from '@/auth/tokenStorage';
import { WS_URL } from '@/config';
import { useLiveLocationsStore } from '@/features/live/liveLocationsStore';
import { handleRealtimeMessage } from './dispatch';
import { createRealtimeClient } from './realtimeClient';

let trackingRelevantHandler: () => void = () => {};

export function setTrackingRelevantHandler(handler: () => void): void {
  trackingRelevantHandler = handler;
}

export function useRealtime(): void {
  const status = useAuthStore((state) => state.status);

  useEffect(() => {
    if (status !== 'signedIn') {
      return;
    }
    const { applyPosition, clearRoom, reset } = useLiveLocationsStore.getState();
    const client = createRealtimeClient({
      url: WS_URL,
      getAccessToken: async () => (await getTokens())?.accessToken ?? null,
      onMessage: (message) =>
        handleRealtimeMessage(message, {
          invalidate: (queryKey) => void queryClient.invalidateQueries({ queryKey }),
          applyPosition,
          clearRoom,
          onTrackingRelevant: () => trackingRelevantHandler(),
        }),
      onReconnected: () => {
        reset();
        void queryClient.invalidateQueries({ queryKey: queryKeys.roomsPrefix });
        void queryClient.invalidateQueries({ queryKey: queryKeys.myRoomsPrefix });
        void queryClient.invalidateQueries({ queryKey: queryKeys.me });
      },
    });
    client.connect();
    return () => {
      client.disconnect();
      reset();
    };
  }, [status]);
}
