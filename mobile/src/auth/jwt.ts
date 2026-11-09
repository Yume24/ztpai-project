export function isExpiringSoon(token: string, nowMs: number, marginMs = 60_000): boolean {
  try {
    const payload = token.split('.')[1];
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '='));
    const { exp } = JSON.parse(json) as { exp?: unknown };
    return typeof exp !== 'number' || exp * 1000 - nowMs <= marginMs;
  } catch {
    return true;
  }
}
