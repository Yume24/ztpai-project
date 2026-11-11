import type { ExpoPushToken, NotificationPermissionsStatus } from 'expo-notifications';
import {
  PermissionStatus,
  getExpoPushTokenAsync,
  getPermissionsAsync,
  requestPermissionsAsync,
  setNotificationChannelAsync,
} from 'expo-notifications';
import { Platform } from 'react-native';
import { authFetch } from '@/api/authFetch';
import { savePushToken } from '@/auth/tokenStorage';
import * as mode from '@/location/mode';
import { registerForPush } from './push';

jest.mock('@/api/authFetch', () => ({ authFetch: jest.fn() }));
jest.mock('@/auth/tokenStorage', () => ({ savePushToken: jest.fn().mockResolvedValue(undefined) }));
jest.mock('expo-constants', () => ({
  __esModule: true,
  default: { easConfig: { projectId: 'eas-project' } },
}));
jest.mock('expo-notifications', () => ({
  AndroidImportance: { HIGH: 4 },
  PermissionStatus: { GRANTED: 'granted', DENIED: 'denied' },
  setNotificationChannelAsync: jest.fn().mockResolvedValue(undefined),
  getPermissionsAsync: jest.fn(),
  requestPermissionsAsync: jest.fn(),
  getExpoPushTokenAsync: jest.fn(),
}));

const authFetchMock = jest.mocked(authFetch);

function permission(status: PermissionStatus.GRANTED | PermissionStatus.DENIED): NotificationPermissionsStatus {
  return { status, granted: status === PermissionStatus.GRANTED, canAskAgain: true, expires: 'never' };
}

beforeEach(() => {
  jest.clearAllMocks();
  jest.spyOn(mode, 'currentMode').mockReturnValue('background');
  jest.mocked(getPermissionsAsync).mockResolvedValue(permission(PermissionStatus.GRANTED));
  const token: ExpoPushToken = { type: 'expo', data: 'ExponentPushToken[abc]' };
  jest.mocked(getExpoPushTokenAsync).mockResolvedValue(token);
  authFetchMock.mockResolvedValue(new Response(null, { status: 204 }));
});

test('registerForPush puts the token percent-encoded in the path', async () => {
  await expect(registerForPush()).resolves.toBe('ExponentPushToken[abc]');

  const request = authFetchMock.mock.calls[0][0];
  expect(request.method).toBe('PUT');
  expect(request.url).toContain('/api/me/push-tokens/ExponentPushToken%5Babc%5D');
  expect(savePushToken).toHaveBeenCalledWith('ExponentPushToken[abc]');
});

test('registerForPush creates the Android channel before requesting the token and passes the projectId', async () => {
  const original = Platform.OS;
  Platform.OS = 'android';
  try {
    await registerForPush();
  } finally {
    Platform.OS = original;
  }

  expect(setNotificationChannelAsync).toHaveBeenCalledWith('default', { name: 'Rendezvous', importance: 4 });
  expect(jest.mocked(setNotificationChannelAsync).mock.invocationCallOrder[0]).toBeLessThan(
    jest.mocked(getExpoPushTokenAsync).mock.invocationCallOrder[0],
  );
  expect(getExpoPushTokenAsync).toHaveBeenCalledWith({ projectId: 'eas-project' });
});

test('registerForPush does nothing in Expo Go', async () => {
  jest.spyOn(mode, 'currentMode').mockReturnValue('foreground');

  await expect(registerForPush()).resolves.toBeNull();
  expect(getPermissionsAsync).not.toHaveBeenCalled();
  expect(authFetchMock).not.toHaveBeenCalled();
});

test('registerForPush returns null without an error when permission is denied', async () => {
  jest.mocked(getPermissionsAsync).mockResolvedValue(permission(PermissionStatus.DENIED));
  jest.mocked(requestPermissionsAsync).mockResolvedValue(permission(PermissionStatus.DENIED));

  await expect(registerForPush()).resolves.toBeNull();
  expect(authFetchMock).not.toHaveBeenCalled();
});

test('registerForPush logs a failed request and returns null', async () => {
  const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
  authFetchMock.mockResolvedValue(new Response('{}', { status: 500 }));

  await expect(registerForPush()).resolves.toBeNull();
  expect(warn).toHaveBeenCalledWith('Push registration failed', expect.anything());
  expect(savePushToken).not.toHaveBeenCalled();
});
