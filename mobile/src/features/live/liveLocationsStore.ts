import { create } from 'zustand';
import type { Position } from '@/realtime/types';

type LiveLocationsState = {
  byRoom: Record<string, Record<string, Position>>;
  applyPosition: (roomId: string, p: Position) => void;
  clearRoom: (roomId: string) => void;
  reset: () => void;
};

export const useLiveLocationsStore = create<LiveLocationsState>((set) => ({
  byRoom: {},

  applyPosition: (roomId, p) =>
    set((state) => {
      const current = state.byRoom[roomId]?.[p.userId];
      if (current && Date.parse(p.recordedAt) <= Date.parse(current.recordedAt)) {
        return state;
      }
      return { byRoom: { ...state.byRoom, [roomId]: { ...state.byRoom[roomId], [p.userId]: p } } };
    }),

  clearRoom: (roomId) =>
    set((state) => {
      const { [roomId]: _removed, ...rest } = state.byRoom;
      return { byRoom: rest };
    }),

  reset: () => set({ byRoom: {} }),
}));
