import Constants from 'expo-constants';
import {
  AndroidImportance,
  getExpoPushTokenAsync,
  getPermissionsAsync,
  requestPermissionsAsync,
  setNotificationChannelAsync,
} from 'expo-notifications';
import { Platform } from 'react-native';
import { api, unwrap } from '@/api/client';
import { savePushToken } from '@/auth/tokenStorage';
import { currentMode } from '@/location/mode';

export async function registerForPush(): Promise<string | null> {
  if (currentMode() === 'foreground') {
    return null;
  }
  try {
    if (Platform.OS === 'android') {
      await setNotificationChannelAsync('default', {
        name: 'Rendezvous',
        importance: AndroidImportance.HIGH,
      });
    }
    let { status } = await getPermissionsAsync();
    if (status !== 'granted') {
      ({ status } = await requestPermissionsAsync());
    }
    if (status !== 'granted') {
      return null;
    }
    const projectId = Constants.easConfig?.projectId ?? Constants.expoConfig?.extra?.eas?.projectId;
    const { data: token } = await getExpoPushTokenAsync({ projectId });
    await unwrap(await api.PUT('/api/me/push-tokens/{token}', { params: { path: { token } } }));
    await savePushToken(token);
    return token;
  } catch (error) {
    console.warn('Push registration failed', error);
    return null;
  }
}
