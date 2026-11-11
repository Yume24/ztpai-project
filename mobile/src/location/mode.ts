import Constants, { ExecutionEnvironment } from 'expo-constants';

export type TrackingMode = 'background' | 'foreground';

export function modeFor(env: ExecutionEnvironment): TrackingMode {
  return env === ExecutionEnvironment.StoreClient ? 'foreground' : 'background';
}

export function currentMode(): TrackingMode {
  return modeFor(Constants.executionEnvironment);
}
