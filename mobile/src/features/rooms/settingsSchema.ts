import { z } from 'zod';
import type { RoomDetailsResponse, UpdateRoomRequest } from '@/api/types';

export const settingsSchema = z
  .object({
    name: z.string().trim().min(1, 'Podaj nazwę pokoju.').max(60, 'Nazwa może mieć najwyżej 60 znaków.'),
    startsAt: z.date(),
    endsAt: z.date(),
    visibilityMode: z.enum(['ALL', 'GUARDIANS_ONLY']),
    membersSeeTrails: z.boolean(),
  })
  .refine((values) => values.endsAt.getTime() > values.startsAt.getTime(), {
    message: 'Koniec musi być po początku.',
    path: ['endsAt'],
  });

export type SettingsValues = z.infer<typeof settingsSchema>;

export function toSettingsValues(room: RoomDetailsResponse): SettingsValues {
  return {
    name: room.name,
    startsAt: new Date(room.startsAt),
    endsAt: new Date(room.endsAt),
    visibilityMode: room.visibilityMode,
    membersSeeTrails: room.membersSeeTrails,
  };
}

const sameTime = (value: Date, iso: string) => value.getTime() === Date.parse(iso);

export function toUpdateRoomRequest(values: SettingsValues, room: RoomDetailsResponse): UpdateRoomRequest {
  const request: UpdateRoomRequest = { version: room.version };
  if (values.name !== room.name) {
    request.name = values.name;
  }
  if (room.status === 'SCHEDULED' && !sameTime(values.startsAt, room.startsAt)) {
    request.startsAt = values.startsAt.toISOString();
  }
  if (!sameTime(values.endsAt, room.endsAt)) {
    request.endsAt = values.endsAt.toISOString();
  }
  if (values.visibilityMode !== room.visibilityMode) {
    request.visibilityMode = values.visibilityMode;
  }
  if (values.membersSeeTrails !== room.membersSeeTrails) {
    request.membersSeeTrails = values.membersSeeTrails;
  }
  return request;
}
