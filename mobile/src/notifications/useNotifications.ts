import * as Notifications from 'expo-notifications';
import { useRouter } from 'expo-router';
import { useEffect, useRef } from 'react';
import { refreshTracking } from '@/location/tracking';
import { registerForPush } from './push';
import { routeForNotification } from './routing';

export function useNotifications(): void {
  const router = useRouter();
  const response = Notifications.useLastNotificationResponse();
  const handled = useRef<Notifications.NotificationResponse | null>(null);

  useEffect(() => {
    void registerForPush();
  }, []);

  useEffect(() => {
    if (
      !response ||
      response === handled.current ||
      response.actionIdentifier !== Notifications.DEFAULT_ACTION_IDENTIFIER
    ) {
      return;
    }
    handled.current = response;
    const route = routeForNotification(response.notification.request.content.data);
    if (route) {
      router.push(route.href);
      if (route.refreshTracking) {
        refreshTracking().catch((error: unknown) => console.warn('Tracking refresh failed', error));
      }
    }
    void Notifications.clearLastNotificationResponseAsync();
  }, [response, router]);
}
