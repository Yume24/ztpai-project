import { useEffect, useState } from 'react';

export const CLOCK_TICK_MS = 30_000;

export function useNow(intervalMs: number = CLOCK_TICK_MS): Date {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), intervalMs);
    return () => clearInterval(timer);
  }, [intervalMs]);
  return now;
}
