import { useState } from 'react';
import { Alert, FlatList, StyleSheet, Text, View } from 'react-native';
import type { AdminRoomResponse, RoomStatus } from '@/api/types';
import { statusLabel } from '@/features/rooms/format';
import { Button } from '@/ui/Button';
import { Chip } from '@/ui/Chip';
import { ErrorText } from '@/ui/ErrorText';
import { Loading } from '@/ui/Loading';
import { colors } from '@/ui/theme';
import { useAdminRooms, useForceCloseRoom } from './hooks';
import { OptionalDateField } from './OptionalDateField';

const FILTERS: { status: RoomStatus | undefined; label: string }[] = [
  { status: undefined, label: 'Wszystkie' },
  { status: 'ACTIVE', label: 'Aktywne' },
  { status: 'SCHEDULED', label: 'Zaplanowane' },
  { status: 'ENDED', label: 'Zakończone' },
];

export function AdminRooms() {
  const [status, setStatus] = useState<RoomStatus | undefined>(undefined);
  const [from, setFrom] = useState<Date | undefined>(undefined);
  const [to, setTo] = useState<Date | undefined>(undefined);
  const rooms = useAdminRooms({ status, from, to });
  const close = useForceCloseRoom();
  const items = rooms.data?.pages.flatMap((page) => page.content) ?? [];

  const confirmClose = (room: AdminRoomResponse) =>
    Alert.alert('Zamknąć pokój?', `Pokój „${room.name}” zostanie zakończony dla wszystkich uczestników.`, [
      { text: 'Anuluj', style: 'cancel' },
      { text: 'Zamknij', style: 'destructive', onPress: () => close.mutate(room.id) },
    ]);

  return (
    <>
      <View style={styles.row}>
        {FILTERS.map((filter) => (
          <Chip
            key={filter.label}
            label={filter.label}
            selected={filter.status === status}
            onPress={() => setStatus(filter.status)}
          />
        ))}
      </View>
      <View style={styles.row}>
        <OptionalDateField label="Od" value={from} onChange={setFrom} />
        <OptionalDateField label="Do" value={to} onChange={setTo} />
      </View>
      <ErrorText error={rooms.error ?? close.error} />
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
          ListEmptyComponent={<Text style={styles.empty}>Brak pokoi.</Text>}
          renderItem={({ item }) => (
            <View style={styles.card}>
              <View style={styles.info}>
                <Text style={styles.name}>{item.name}</Text>
                <Text style={styles.meta}>Właściciel: {item.owner.displayName}</Text>
                <Text style={styles.meta}>
                  {statusLabel(item.status)} · {item.memberCount} os.
                </Text>
              </View>
              {item.status === 'ENDED' ? null : (
                <Button title="Zamknij" variant="danger" onPress={() => confirmClose(item)} />
              )}
            </View>
          )}
        />
      )}
    </>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  list: { gap: 8, paddingBottom: 16 },
  empty: { color: colors.muted, fontSize: 16, paddingTop: 16 },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    padding: 14,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: colors.border,
  },
  info: { flex: 1, gap: 2 },
  name: { fontSize: 17, fontWeight: '600', color: colors.text },
  meta: { fontSize: 14, color: colors.muted },
});
