import { api, unwrap } from '@/api/client';
import type { AuthResponse, LogoutRequest, UserResponse } from '@/api/types';
import type { LoginValues, RegisterValues } from './schemas';

export async function getMe(): Promise<UserResponse> {
  return unwrap(await api.GET('/api/me'));
}

export async function login(values: LoginValues): Promise<AuthResponse> {
  return unwrap(await api.POST('/api/auth/login', { body: values }));
}

export async function register(values: RegisterValues): Promise<AuthResponse> {
  return unwrap(await api.POST('/api/auth/register', { body: values }));
}

export async function logout(body: LogoutRequest): Promise<void> {
  await unwrap(await api.POST('/api/auth/logout', { body }));
}

export async function renameMe(displayName: string): Promise<UserResponse> {
  return unwrap(await api.PATCH('/api/me', { body: { displayName } }));
}
