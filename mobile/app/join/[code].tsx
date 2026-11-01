import { useLocalSearchParams, useRouter } from 'expo-router';
import { useEffect } from 'react';
import { usePendingJoin } from '@/auth/pendingJoin';
import { useAuthStore } from '@/auth/authStore';
import { Loading } from '@/ui/Loading';

export default function JoinLinkScreen() {
  const { code } = useLocalSearchParams<{ code: string }>();
  const status = useAuthStore((state) => state.status);
  const router = useRouter();

  useEffect(() => {
    if (status === 'signedIn') {
      router.replace({ pathname: '/rooms/join', params: { code } });
    } else if (status === 'signedOut') {
      usePendingJoin.getState().setCode(code);
      router.replace('/login');
    }
  }, [status, code, router]);

  return <Loading />;
}
