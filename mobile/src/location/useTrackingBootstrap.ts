import { usePathname, useRouter } from 'expo-router';
import { useEffect, useRef } from 'react';
import { AppState } from 'react-native';
import { setRoomMembershipChangedHandler } from '@/features/rooms/hooks';
import { setTrackingRelevantHandler } from '@/realtime/useRealtime';
import { refreshTracking, stopTracking } from './tracking';

const PERMISSION_ROUTE = '/location-permission';

export function useTrackingBootstrap(): void {
  const router = useRouter();
  const pathname = usePathname();
  const routerRef = useRef(router);
  const pathnameRef = useRef(pathname);

  useEffect(() => {
    routerRef.current = router;
    pathnameRef.current = pathname;
  }, [router, pathname]);

  useEffect(() => {
    let active = true;
    let permissionPrompted = false;

    const refresh = () => {
      refreshTracking()
        .then((result) => {
          if (!active) {
            return;
          }
          if (result === 'needs-permission' && !permissionPrompted && pathnameRef.current !== PERMISSION_ROUTE) {
            permissionPrompted = true;
            routerRef.current.push(PERMISSION_ROUTE);
          }
        })
        .catch((error: unknown) => {
          console.warn('Tracking refresh failed', error);
        });
    };

    refresh();
    setRoomMembershipChangedHandler(refresh);
    setTrackingRelevantHandler(refresh);
    const appState = AppState.addEventListener('change', (state) => {
      if (state === 'active') {
        refresh();
      }
    });

    return () => {
      active = false;
      appState.remove();
      setRoomMembershipChangedHandler(() => {});
      setTrackingRelevantHandler(() => {});
      void stopTracking();
    };
  }, []);
}
