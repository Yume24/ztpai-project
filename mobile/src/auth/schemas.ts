import { z } from 'zod';

const MAX_PASSWORD_BYTES = 72;

const email = z.string().trim().pipe(z.email('Podaj poprawny adres e-mail.'));
const displayName = z
  .string()
  .trim()
  .min(2, 'Nazwa musi mieć od 2 do 40 znaków.')
  .max(40, 'Nazwa musi mieć od 2 do 40 znaków.');

export const loginSchema = z.object({
  email,
  password: z.string().min(1, 'Podaj hasło.'),
});

export const registerSchema = z.object({
  email: email.pipe(z.string().max(254, 'Adres e-mail może mieć najwyżej 254 znaki.')),
  password: z
    .string()
    .min(8, 'Hasło musi mieć od 8 do 72 znaków.')
    .max(72, 'Hasło musi mieć od 8 do 72 znaków.')
    .refine((value) => new TextEncoder().encode(value).length <= MAX_PASSWORD_BYTES, {
      message: 'Hasło może mieć najwyżej 72 bajty.',
    }),
  displayName,
});

export const profileSchema = z.object({ displayName });

export type LoginValues = z.infer<typeof loginSchema>;
export type RegisterValues = z.infer<typeof registerSchema>;
export type ProfileValues = z.infer<typeof profileSchema>;
