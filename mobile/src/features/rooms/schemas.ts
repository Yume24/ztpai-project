import { z } from 'zod';
import type { CreateRoomRequest } from '@/api/types';

const CODE_PATTERN = /^[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{6}$/;

export const roomFormSchema = z
  .object({
    name: z
      .string()
      .trim()
      .min(1, 'Podaj nazwę pokoju.')
      .max(60, 'Nazwa może mieć najwyżej 60 znaków.'),
    startsAt: z.date().nullable(),
    endsAt: z.date(),
  })
  .refine((values) => values.endsAt.getTime() > (values.startsAt ?? new Date()).getTime(), {
    message: 'Koniec musi być po początku.',
    path: ['endsAt'],
  });

export const joinSchema = z.object({
  code: z.string().trim().toUpperCase().regex(CODE_PATTERN, 'Kod ma 6 znaków.'),
});

export type RoomFormValues = z.infer<typeof roomFormSchema>;
export type JoinValues = z.infer<typeof joinSchema>;

export function toCreateRoomRequest(values: RoomFormValues): CreateRoomRequest {
  return {
    name: values.name,
    ...(values.startsAt ? { startsAt: values.startsAt.toISOString() } : {}),
    endsAt: values.endsAt.toISOString(),
  };
}
