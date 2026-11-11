import * as Location from 'expo-location';
import type { TrackingMode } from './mode';

export type PermissionResult = 'granted' | 'foreground-only' | 'denied';

export async function permissionStatus(
  mode: TrackingMode,
): Promise<PermissionResult | 'undetermined' | 'upgradable'> {
  const foreground = await Location.getForegroundPermissionsAsync();
  if (!foreground.granted) {
    return foreground.canAskAgain ? 'undetermined' : 'denied';
  }
  if (mode === 'foreground') {
    return 'granted';
  }
  const background = await Location.getBackgroundPermissionsAsync();
  if (background.granted) {
    return 'granted';
  }
  return background.canAskAgain ? 'upgradable' : 'foreground-only';
}

export async function requestPermissions(mode: TrackingMode): Promise<PermissionResult> {
  const foreground = await Location.requestForegroundPermissionsAsync();
  if (!foreground.granted) {
    return 'denied';
  }
  if (mode === 'foreground') {
    return 'granted';
  }
  const background = await Location.requestBackgroundPermissionsAsync();
  return background.granted ? 'granted' : 'foreground-only';
}
