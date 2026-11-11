import type { LocationObject } from 'expo-location';
import * as TaskManager from 'expo-task-manager';
import { LOCATION_TASK } from './taskName';
import { deliverLocations } from './tracking';

// Must be defined at module scope so the task also runs when the app is started in the background.
TaskManager.defineTask<{ locations: LocationObject[] }>(LOCATION_TASK, async ({ data, error }) => {
  if (error || !data) {
    return;
  }
  await deliverLocations(data.locations);
});
