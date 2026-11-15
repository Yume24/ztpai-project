import { Stack } from 'expo-router';
import { renderRouter, screen } from 'expo-router/testing-library';
import { Text } from 'react-native';

type AuthStatus = 'loading' | 'signedOut' | 'signedIn';

let mockStatus: AuthStatus = 'signedOut';

jest.mock('@/location/backgroundTask', () => ({}));
jest.mock('expo-notifications', () => ({ setNotificationHandler: jest.fn() }));
jest.mock('@/auth/authStore', () => ({
  useAuthStore: <T,>(selector: (state: { status: AuthStatus; hydrate: () => Promise<void> }) => T): T =>
    selector({ status: mockStatus, hydrate: async () => undefined }),
}));

const router = {
  appDir: 'app',
  overrides: {
    '(app)/_layout': () => <Stack />,
    '(app)/(tabs)/_layout': () => <Stack />,
    '(app)/(tabs)/index': () => <Text>Pokoje</Text>,
  },
};

test('a signed-out start lands on the login screen, not an unmatched route', async () => {
  mockStatus = 'signedOut';
  await renderRouter(router, { initialUrl: '/' });
  expect(screen.queryByTestId('expo-router-unmatched')).toBeNull();
  expect(screen).toHavePathname('/login');
});

test('a signed-in start lands on the room list', async () => {
  mockStatus = 'signedIn';
  await renderRouter(router, { initialUrl: '/' });
  expect(screen.getByText('Pokoje')).toBeTruthy();
});
