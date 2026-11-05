import { useRouter } from 'expo-router';
import { useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import type { RoomStatus } from '@/api/types';
import { statusLabel } from '@/features/rooms/format';
import { useMyRooms } from '@/features/rooms/hooks';
import { Button } from '@/ui/Button';
import { Chip } from '@/ui/Chip';
import { ErrorText } from '@/ui/ErrorText';
import { Loading } from '@/ui/Loading';
import { Screen } from '@/ui/Screen';
import { colors } from '@/ui/theme';

const FILTERS: { status: RoomStatus; label: string }[] = [
  { status: 'ACTIVE', label: 'Aktywne' },
  { status: 'SCHEDULED', label: 'Zaplanowane' },
  { status: 'ENDED', label: 'Zakończone' },
];

export default function RoomsScreen() {
  const router = useRouter();
  const [status, setStatus] = useState<RoomStatus>('ACTIVE');
  const rooms = useMyRooms(status);
  const items = rooms.data?.pages.flatMap((page) => page.content) ?? [];

  return (
    <Screen>
      <View style={styles.row}>
        {FILTERS.map((filter) => (
          <Chip
            key={filter.status}
            label={filter.label}
            selected={filter.status === status}
            onPress={() => setStatus(filter.status)}
          />
        ))}
      </View>
      <View style={styles.row}>
        <View style={styles.flex}>
          <Button title="Utwórz pokój" onPress={() => router.push('/rooms/new')} />
        </View>
        <View style={styles.flex}>
          <Button title="Dołącz kodem" variant="secondary" onPress={() => router.push('/rooms/join')} />
        </View>
      </View>
      <ErrorText error={rooms.error} />
      {rooms.isPending ? (
        <Loading />
      ) : (
        <FlatList
          data={items}
          keyExtractor={(room) => room.id}
          contentContainerStyle={styles.list}
          onEndReached={() => {
            if (rooms.hasNextPage && !rooms.isFetchingNextPage) {
              void rooms.fetchNextPage();
            }
          }}
          onRefresh={() => void rooms.refetch()}
          refreshing={rooms.isRefetching && !rooms.isFetchingNextPage}
          ListEmptyComponent={<Text style={styles.empty}>Nie masz jeszcze żadnych pokoi w tej kategorii.</Text>}
          renderItem={({ item }) => (
            <Pressable
              accessibilityRole="button"
              style={styles.card}
              onPress={() => router.push({ pathname: '/rooms/[id]', params: { id: item.id } })}
            >
              <Text style={styles.name}>{item.name}</Text>
              <Text style={styles.meta}>
                {statusLabel(item.status)} · {item.memberCount} os.
              </Text>
            </Pressable>
          )}
        />
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', gap: 8 },
  flex: { flex: 1 },
  list: { gap: 8, paddingBottom: 16 },
  empty: { color: colors.muted, fontSize: 16, paddingTop: 16 },
  card: { padding: 14, borderRadius: 8, borderWidth: 1, borderColor: colors.border, gap: 4 },
  name: { fontSize: 17, fontWeight: '600', color: colors.text },
  meta: { fontSize: 14, color: colors.muted },
});
