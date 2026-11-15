import Ionicons from '@expo/vector-icons/Ionicons';
import { Tabs } from 'expo-router';
import type { ComponentProps } from 'react';
import { useAuthStore } from '@/auth/authStore';
import { colors } from '@/ui/theme';

type IconName = ComponentProps<typeof Ionicons>['name'];

function tabIcon(name: IconName) {
  return function TabIcon({ focused, size }: { focused: boolean; size: number }) {
    return <Ionicons name={name} size={size} color={focused ? colors.primary : colors.muted} />;
  };
}

export default function TabsLayout() {
  const user = useAuthStore((state) => state.user);

  return (
    <Tabs screenOptions={{ tabBarActiveTintColor: colors.primary, tabBarInactiveTintColor: colors.muted }}>
      <Tabs.Screen name="index" options={{ title: 'Pokoje', tabBarIcon: tabIcon('people') }} />
      <Tabs.Screen name="profile" options={{ title: 'Profil', tabBarIcon: tabIcon('person-circle') }} />
      <Tabs.Screen
        name="admin"
        options={{
          title: 'Admin',
          tabBarIcon: tabIcon('shield-checkmark'),
          href: user?.role === 'ADMIN' ? undefined : null,
        }}
      />
    </Tabs>
  );
}
