import type { LocationObject } from 'expo-location';
import { toPointRequest, UNKNOWN_ACCURACY_M } from './points';

function location(accuracy: number | null): LocationObject {
  return {
    coords: { latitude: 52.1, longitude: 21.0, accuracy, altitude: null, altitudeAccuracy: null, heading: null, speed: null },
    timestamp: Date.UTC(2026, 10, 15, 12, 0, 0),
  };
}

test('toPointRequest maps coords, converts ms timestamp to ISO and uses 1000 m for unknown accuracy', () => {
  expect(toPointRequest(location(7.5))).toEqual({
    lat: 52.1,
    lon: 21.0,
    accuracyM: 7.5,
    recordedAt: '2026-11-15T12:00:00.000Z',
  });
  expect(toPointRequest(location(null)).accuracyM).toBe(UNKNOWN_ACCURACY_M);
  expect(UNKNOWN_ACCURACY_M).toBe(1000);
});

test('toPointRequest replaces a negative accuracy, which the backend would reject', () => {
  expect(toPointRequest(location(-1)).accuracyM).toBe(UNKNOWN_ACCURACY_M);
});

test('toPointRequest keeps a zero accuracy', () => {
  expect(toPointRequest(location(0)).accuracyM).toBe(0);
});
