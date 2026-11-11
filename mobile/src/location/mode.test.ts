import { ExecutionEnvironment } from 'expo-constants';
import { modeFor } from './mode';

test('modeFor uses foreground in Expo Go and background in builds', () => {
  expect(modeFor(ExecutionEnvironment.StoreClient)).toBe('foreground');
  expect(modeFor(ExecutionEnvironment.Standalone)).toBe('background');
  expect(modeFor(ExecutionEnvironment.Bare)).toBe('background');
});
