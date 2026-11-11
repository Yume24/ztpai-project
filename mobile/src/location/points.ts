import type { LocationObject } from 'expo-location';
import type { LocationPointRequest } from '@/api/types';

export const UNKNOWN_ACCURACY_M = 1000;

export function toPointRequest(loc: LocationObject): LocationPointRequest {
  const { latitude, longitude, accuracy } = loc.coords;
  return {
    lat: latitude,
    lon: longitude,
    accuracyM: accuracy != null && accuracy >= 0 ? accuracy : UNKNOWN_ACCURACY_M,
    recordedAt: new Date(loc.timestamp).toISOString(),
  };
}
