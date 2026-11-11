import * as Location from 'expo-location';
import type { LocationObject, LocationSubscription } from 'expo-location';
import { api, unwrap } from '@/api/client';
import { getTokens } from '@/auth/tokenStorage';
import { LOCATION_TASK } from './taskName';
import { currentMode } from './mode';
import { permissionStatus } from './permissions';
import { toPointRequest } from './points';
import { sendLocations } from './sender';

const TRACKING_OPTIONS = {
  accuracy: Location.Accuracy.High,
  timeInterval: 10_000,
  distanceInterval: 20,
} as const;

let subscription: LocationSubscription | null = null;
let starting: Promise<StartResult> | null = null;

type StartResult = 'started' | 'needs-permission' | 'denied' | 'signed-out';

export async function deliverLocations(locations: LocationObject[]): Promise<void> {
  try {
    if (!(await getTokens())) {
      await stopTracking();
      return;
    }
    const activeRoomCount = await sendLocations(locations.map(toPointRequest));
    if (activeRoomCount === 0) {
      await stopTracking();
    }
  } catch (error) {
    console.warn('Location delivery failed', error);
  }
}

export async function isTracking(): Promise<boolean> {
  if (subscription != null) {
    return true;
  }
  return currentMode() === 'background' && Location.hasStartedLocationUpdatesAsync(LOCATION_TASK);
}

async function begin(): Promise<StartResult> {
  if (!(await getTokens())) {
    return 'signed-out';
  }
  const mode = currentMode();
  const permission = await permissionStatus(mode);
  if (permission === 'undetermined') {
    return 'needs-permission';
  }
  if (permission === 'denied') {
    return 'denied';
  }
  if (mode === 'background' && permission === 'granted') {
    removeWatcher();
    if (!(await Location.hasStartedLocationUpdatesAsync(LOCATION_TASK))) {
      await Location.startLocationUpdatesAsync(LOCATION_TASK, {
        ...TRACKING_OPTIONS,
        foregroundService: {
          notificationTitle: 'Rendezvous',
          notificationBody: 'Udostępniasz lokalizację w Rendezvous',
        },
        showsBackgroundLocationIndicator: true,
        pausesUpdatesAutomatically: false,
      });
    }
  } else {
    await stopBackgroundUpdates();
    if (subscription == null) {
      subscription = await Location.watchPositionAsync(TRACKING_OPTIONS, (location) => {
        void deliverLocations([location]);
      });
    }
  }
  return permission === 'upgradable' ? 'needs-permission' : 'started';
}

export function startTracking(): Promise<StartResult> {
  starting ??= begin().finally(() => {
    starting = null;
  });
  return starting;
}

function removeWatcher(): void {
  subscription?.remove();
  subscription = null;
}

async function stopBackgroundUpdates(): Promise<void> {
  if (currentMode() === 'background' && (await Location.hasStartedLocationUpdatesAsync(LOCATION_TASK))) {
    await Location.stopLocationUpdatesAsync(LOCATION_TASK);
  }
}

export async function stopTracking(): Promise<void> {
  await starting?.catch(() => undefined);
  removeWatcher();
  await stopBackgroundUpdates();
}

export async function refreshTracking(): Promise<StartResult | 'stopped'> {
  const rooms = await unwrap(await api.GET('/api/me/rooms', { params: { query: { status: 'ACTIVE', size: 1 } } }));
  if (rooms.totalElements > 0) {
    return startTracking();
  }
  await stopTracking();
  return 'stopped';
}
