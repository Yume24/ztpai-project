import type { Position, TrailPoint } from '@/realtime/types';

const isNewer = (a: { recordedAt: string }, b: { recordedAt: string }) =>
  Date.parse(a.recordedAt) > Date.parse(b.recordedAt);

export function mergePositions(base: Position[], live: Record<string, Position> | undefined): Position[] {
  const byUser = new Map<string, Position>();
  for (const position of [...base, ...Object.values(live ?? {})]) {
    const current = byUser.get(position.userId);
    if (!current || isNewer(position, current)) {
      byUser.set(position.userId, position);
    }
  }
  return [...byUser.values()];
}

export function appendToTrail(trail: TrailPoint[], p: Position): TrailPoint[] {
  const last = trail[trail.length - 1];
  if (last && !isNewer(p, last)) {
    return trail;
  }
  return [...trail, { lat: p.lat, lon: p.lon, recordedAt: p.recordedAt }];
}
