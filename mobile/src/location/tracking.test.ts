import Constants from 'expo-constants';
import * as Location from 'expo-location';
import * as TaskManager from 'expo-task-manager';
import { api } from '@/api/client';
import { ApiError } from '@/api/errors';
import { getTokens } from '@/auth/tokenStorage';
import './backgroundTask';
import { sendLocations } from './sender';
import { LOCATION_TASK } from './taskName';
import { isTracking, refreshTracking, startTracking, stopTracking } from './tracking';

jest.mock('expo-constants', () => ({
  __esModule: true,
  default: { executionEnvironment: 'standalone' },
  ExecutionEnvironment: { Bare: 'bare', Standalone: 'standalone', StoreClient: 'storeClient' },
}));
jest.mock('expo-task-manager', () => ({ defineTask: jest.fn() }));
jest.mock('expo-location', () => ({
  Accuracy: { High: 4 },
  PermissionStatus: { UNDETERMINED: 'undetermined', DENIED: 'denied', GRANTED: 'granted' },
  getForegroundPermissionsAsync: jest.fn(),
  getBackgroundPermissionsAsync: jest.fn(),
  hasStartedLocationUpdatesAsync: jest.fn(),
  startLocationUpdatesAsync: jest.fn(),
  stopLocationUpdatesAsync: jest.fn(),
  watchPositionAsync: jest.fn(),
}));
jest.mock('@/api/client', () => ({
  api: { GET: jest.fn(), POST: jest.fn() },
  unwrap: async (result: { data: unknown }) => result.data,
}));
jest.mock('@/auth/tokenStorage', () => ({ getTokens: jest.fn() }));

const location = Location as jest.Mocked<typeof Location>;
const apiMock = api as unknown as { GET: jest.Mock; POST: jest.Mock };
const getTokensMock = jest.mocked(getTokens);
const [definedTaskName, definedHandler] = (TaskManager.defineTask as jest.Mock).mock.calls[0] as [string, unknown];
const taskHandler = definedHandler as (body: {
  data: unknown;
  error: unknown;
}) => Promise<void>;
const setEnvironment = (env: string) => {
  (Constants as { executionEnvironment: string }).executionEnvironment = env;
};

const fix = (lat = 52) => ({
  coords: { latitude: lat, longitude: 21, accuracy: 5 },
  timestamp: Date.UTC(2026, 10, 15, 12),
});
const permission = (status: string, canAskAgain = status !== 'denied') => ({
  status,
  granted: status === 'granted',
  canAskAgain,
});

function mockPermissions(foreground: string, background = 'granted') {
  location.getForegroundPermissionsAsync.mockResolvedValue(permission(foreground) as never);
  location.getBackgroundPermissionsAsync.mockResolvedValue(permission(background) as never);
}

function mockActiveRooms(totalElements: number) {
  apiMock.GET.mockResolvedValue({ data: { totalElements } });
}

beforeEach(async () => {
  jest.clearAllMocks();
  setEnvironment('standalone');
  mockPermissions('granted');
  location.hasStartedLocationUpdatesAsync.mockResolvedValue(false);
  location.watchPositionAsync.mockResolvedValue({ remove: jest.fn() });
  await stopTracking();
  jest.clearAllMocks();
  getTokensMock.mockResolvedValue({ accessToken: 'access', refreshToken: 'refresh' });
  location.hasStartedLocationUpdatesAsync.mockResolvedValue(false);
  location.watchPositionAsync.mockResolvedValue({ remove: jest.fn() });
});

test('defines the background task under the agreed name', () => {
  expect(LOCATION_TASK).toBe('rendezvous-location');
  expect(definedTaskName).toBe('rendezvous-location');
});

test('refreshTracking starts when there is an active room and stops otherwise', async () => {
  mockActiveRooms(1);
  expect(await refreshTracking()).toBe('started');
  expect(location.startLocationUpdatesAsync).toHaveBeenCalledWith(
    'rendezvous-location',
    expect.objectContaining({ timeInterval: 10_000, distanceInterval: 20, pausesUpdatesAutomatically: false }),
  );
  expect(apiMock.GET).toHaveBeenCalledWith('/api/me/rooms', { params: { query: { status: 'ACTIVE', size: 1 } } });

  mockActiveRooms(0);
  location.hasStartedLocationUpdatesAsync.mockResolvedValue(true);
  expect(await refreshTracking()).toBe('stopped');
  expect(location.stopLocationUpdatesAsync).toHaveBeenCalledWith('rendezvous-location');
});

test('startTracking does not restart updates that are already running', async () => {
  location.hasStartedLocationUpdatesAsync.mockResolvedValue(true);
  expect(await startTracking()).toBe('started');
  expect(location.startLocationUpdatesAsync).not.toHaveBeenCalled();
});

test('startTracking reports a missing or refused permission without starting', async () => {
  mockPermissions('undetermined');
  expect(await startTracking()).toBe('needs-permission');
  mockPermissions('denied');
  expect(await startTracking()).toBe('denied');
  expect(location.startLocationUpdatesAsync).not.toHaveBeenCalled();
  expect(location.watchPositionAsync).not.toHaveBeenCalled();
});

test('without "always" yet, tracking runs in the foreground and still asks for the background permission', async () => {
  mockPermissions('granted', 'undetermined');
  expect(await startTracking()).toBe('needs-permission');
  expect(location.watchPositionAsync).toHaveBeenCalledTimes(1);
  expect(location.startLocationUpdatesAsync).not.toHaveBeenCalled();
});

test('a denied "while in use" that can still be asked counts as undetermined (Android before the first prompt)', async () => {
  location.getForegroundPermissionsAsync.mockResolvedValue({ status: 'denied', granted: false, canAskAgain: true } as never);
  expect(await startTracking()).toBe('needs-permission');
  expect(location.watchPositionAsync).not.toHaveBeenCalled();
});

test('a refused "always" that Android can still ask for again tracks in the foreground', async () => {
  location.getBackgroundPermissionsAsync.mockResolvedValue({ status: 'denied', granted: false, canAskAgain: true } as never);
  expect(await startTracking()).toBe('needs-permission');
  expect(location.watchPositionAsync).toHaveBeenCalledTimes(1);
  expect(location.startLocationUpdatesAsync).not.toHaveBeenCalled();
  expect(await isTracking()).toBe(true);
});

test('revoking "always" stops the background updates before the foreground watcher starts', async () => {
  await startTracking();
  expect(location.startLocationUpdatesAsync).toHaveBeenCalledTimes(1);
  location.hasStartedLocationUpdatesAsync.mockResolvedValue(true);
  mockPermissions('granted', 'denied');

  expect(await startTracking()).toBe('started');
  expect(location.stopLocationUpdatesAsync).toHaveBeenCalledWith('rendezvous-location');
  expect(location.watchPositionAsync).toHaveBeenCalledTimes(1);
  const [stoppedAt] = location.stopLocationUpdatesAsync.mock.invocationCallOrder;
  const [watchedAt] = location.watchPositionAsync.mock.invocationCallOrder;
  expect(stoppedAt).toBeLessThan(watchedAt);
});

test('a refresh whose room request outlives the session starts nothing', async () => {
  let respond: (result: unknown) => void = () => {};
  apiMock.GET.mockReturnValue(new Promise((resolve) => (respond = resolve)));
  const refresh = refreshTracking();
  getTokensMock.mockResolvedValue(null);
  respond({ data: { totalElements: 1 } });

  expect(await refresh).toBe('signed-out');
  expect(location.startLocationUpdatesAsync).not.toHaveBeenCalled();
  expect(location.watchPositionAsync).not.toHaveBeenCalled();
});

test('a permission that cannot be asked again is final', async () => {
  location.getForegroundPermissionsAsync.mockResolvedValue({ status: 'denied', granted: false, canAskAgain: false } as never);
  expect(await startTracking()).toBe('denied');
});

test('granting "always" later upgrades a foreground watcher to background updates', async () => {
  const remove = jest.fn();
  location.watchPositionAsync.mockResolvedValue({ remove });
  mockPermissions('granted', 'denied');
  location.getBackgroundPermissionsAsync.mockResolvedValue({ status: 'denied', granted: false, canAskAgain: false } as never);
  await startTracking();
  expect(location.watchPositionAsync).toHaveBeenCalledTimes(1);
  mockPermissions('granted', 'granted');
  expect(await startTracking()).toBe('started');
  expect(remove).toHaveBeenCalledTimes(1);
  expect(location.startLocationUpdatesAsync).toHaveBeenCalledTimes(1);
});

test('stopTracking waits for a start in progress so no watcher is left behind', async () => {
  setEnvironment('storeClient');
  const remove = jest.fn();
  location.watchPositionAsync.mockResolvedValue({ remove });
  const start = startTracking();
  await stopTracking();
  await start;
  expect(remove).toHaveBeenCalledTimes(1);
  expect(await isTracking()).toBe(false);
});

test('refreshTracking leaves tracking untouched when the request fails', async () => {
  apiMock.GET.mockRejectedValue(new Error('offline'));
  await expect(refreshTracking()).rejects.toThrow('offline');
  expect(location.stopLocationUpdatesAsync).not.toHaveBeenCalled();
  expect(location.startLocationUpdatesAsync).not.toHaveBeenCalled();
});

test('foreground-only permission in a build falls back to a foreground watcher', async () => {
  mockPermissions('granted', 'denied');
  expect(await startTracking()).toBe('started');
  expect(location.startLocationUpdatesAsync).not.toHaveBeenCalled();
  expect(location.watchPositionAsync).toHaveBeenCalledTimes(1);
  expect(await isTracking()).toBe(true);
});

test('Expo Go uses one foreground watcher, even for repeated and concurrent starts', async () => {
  setEnvironment('storeClient');
  const results = await Promise.all([startTracking(), startTracking()]);
  expect(results).toEqual(['started', 'started']);
  await startTracking();
  expect(location.watchPositionAsync).toHaveBeenCalledTimes(1);
  expect(location.startLocationUpdatesAsync).not.toHaveBeenCalled();
  expect(await isTracking()).toBe(true);
});

test('stopTracking removes the foreground watcher', async () => {
  setEnvironment('storeClient');
  const remove = jest.fn();
  location.watchPositionAsync.mockResolvedValue({ remove });
  await startTracking();
  await stopTracking();
  expect(remove).toHaveBeenCalledTimes(1);
  expect(await isTracking()).toBe(false);
});

test('a foreground position is sent and a zero room count stops the watcher', async () => {
  setEnvironment('storeClient');
  const remove = jest.fn();
  location.watchPositionAsync.mockResolvedValue({ remove });
  apiMock.POST.mockResolvedValue({ data: { activeRoomCount: 0 } });
  await startTracking();
  const onPosition = location.watchPositionAsync.mock.calls[0][1];
  onPosition(fix() as never);
  await new Promise<void>((resolve) => setImmediate(resolve));
  expect(apiMock.POST).toHaveBeenCalledWith('/api/me/locations', {
    body: { points: [{ lat: 52, lon: 21, accuracyM: 5, recordedAt: '2026-11-15T12:00:00.000Z' }] },
  });
  expect(remove).toHaveBeenCalled();
});

test('background task stops tracking when the server reports zero active rooms', async () => {
  location.hasStartedLocationUpdatesAsync.mockResolvedValue(true);
  apiMock.POST.mockResolvedValue({ data: { activeRoomCount: 0 } });
  await taskHandler({ data: { locations: [fix()] }, error: null });
  expect(location.stopLocationUpdatesAsync).toHaveBeenCalledWith('rendezvous-location');
});

test('background task keeps tracking while rooms are active and ignores task errors', async () => {
  location.hasStartedLocationUpdatesAsync.mockResolvedValue(true);
  apiMock.POST.mockResolvedValue({ data: { activeRoomCount: 2 } });
  await taskHandler({ data: { locations: [fix()] }, error: null });
  await taskHandler({ data: null, error: new Error('boom') });
  expect(location.stopLocationUpdatesAsync).not.toHaveBeenCalled();
  expect(apiMock.POST).toHaveBeenCalledTimes(1);
});

test('a position that arrives after the session ended stops tracking instead of being sent', async () => {
  location.hasStartedLocationUpdatesAsync.mockResolvedValue(true);
  getTokensMock.mockResolvedValue(null);
  await taskHandler({ data: { locations: [fix()] }, error: null });
  expect(apiMock.POST).not.toHaveBeenCalled();
  expect(location.stopLocationUpdatesAsync).toHaveBeenCalledWith('rendezvous-location');
});

test('a rejected send is logged and keeps tracking while the session lasts', async () => {
  const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  location.hasStartedLocationUpdatesAsync.mockResolvedValue(true);
  apiMock.POST.mockRejectedValue(new ApiError(401, 'Unauthorized'));
  await taskHandler({ data: { locations: [fix()] }, error: null });
  expect(warn).toHaveBeenCalledWith('Location delivery failed', expect.any(ApiError));
  expect(location.stopLocationUpdatesAsync).not.toHaveBeenCalled();
  warn.mockRestore();
});

test('sendLocations skips an empty list and sends 120 points as 50, 50 and 20 in order', async () => {
  expect(await sendLocations([])).toBe(-1);
  expect(apiMock.POST).not.toHaveBeenCalled();
  apiMock.POST.mockResolvedValueOnce({ data: { activeRoomCount: 3 } })
    .mockResolvedValueOnce({ data: { activeRoomCount: 2 } })
    .mockResolvedValueOnce({ data: { activeRoomCount: 1 } });
  const points = Array.from({ length: 120 }, (_, i) => ({
    lat: i % 90,
    lon: i,
    accuracyM: 1,
    recordedAt: '2026-11-15T12:00:00.000Z',
  }));
  expect(await sendLocations(points)).toBe(1);
  const batches = apiMock.POST.mock.calls.map((call) => call[1].body.points as { lon: number }[]);
  expect(batches.map((batch) => batch.length)).toEqual([50, 50, 20]);
  expect(batches.flat().map((point) => point.lon)).toEqual(points.map((point) => point.lon));
});
