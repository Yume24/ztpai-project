import { create } from 'zustand';

type PendingJoinState = {
  code: string | null;
  setCode: (code: string) => void;
  take: () => string | null;
};

export const usePendingJoin = create<PendingJoinState>((set, get) => ({
  code: null,
  setCode: (code) => set({ code }),
  take: () => {
    const { code } = get();
    set({ code: null });
    return code;
  },
}));
