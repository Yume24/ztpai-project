import type { ExpoConfig } from 'expo/config';

const config: ExpoConfig = {
  name: 'Rendezvous',
  slug: 'rendezvous',
  scheme: 'rendezvous',
  platforms: ['ios', 'android'],
  version: '1.0.0',
  orientation: 'portrait',
  icon: './assets/icon.png',
  userInterfaceStyle: 'light',
  ios: {
    supportsTablet: true,
    bundleIdentifier: 'com.rendezvous.app',
  },
  android: {
    package: 'com.rendezvous.app',
    googleServicesFile: process.env.GOOGLE_SERVICES_JSON,
    adaptiveIcon: {
      backgroundColor: '#E6F4FE',
      foregroundImage: './assets/android-icon-foreground.png',
      backgroundImage: './assets/android-icon-background.png',
      monochromeImage: './assets/android-icon-monochrome.png',
    },
    predictiveBackGestureEnabled: false,
  },
  extra: {
    eas: {
      projectId: process.env.EAS_PROJECT_ID,
    },
  },
  plugins: [
    'expo-router',
    [
      'expo-location',
      {
        isAndroidBackgroundLocationEnabled: true,
        isIosBackgroundLocationEnabled: true,
        isAndroidForegroundServiceEnabled: true,
        locationWhenInUsePermission:
          'Rendezvous pokazuje twoją pozycję członkom pokoju, do którego należysz.',
        locationAlwaysAndWhenInUsePermission:
          'Rendezvous udostępnia twoją lokalizację członkom pokoju także wtedy, gdy telefon jest zablokowany — tylko w czasie trwania pokoju.',
      },
    ],
    'expo-notifications',
    'expo-secure-store',
    '@react-native-community/datetimepicker',
    ['expo-build-properties', { android: { usesCleartextTraffic: true } }],
    ['react-native-maps', { androidGoogleMapsApiKey: process.env.GOOGLE_MAPS_API_KEY }],
  ],
};

export default config;
