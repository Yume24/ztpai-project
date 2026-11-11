import { useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { FlatList, StyleSheet, Text, View } from 'react-native';
import type { AlertType } from '@/api/types';
import { alertText } from '@/features/alerts/alertText';
import { useAlerts } from '@/features/alerts/hooks';
import { formatDuration } from '@/features/rooms/format';
import { Chip } from '@/ui/Chip';
import { ErrorText } from '@/ui/ErrorText';
import { Loading } from '@/ui/Loading';
import { Screen } from '@/ui/Screen';
import { colors } from '@/ui/theme';
import { useNow } from '@/ui/useNow';

const FILTERS: { type: AlertType | undefined; label: string }[] = [
  { type: undefined, label: 'Wszystkie' },
  { type: 'GEOFENCE_EXIT', label: 'Wyjście' },
  { type: 'GEOFENCE_RETURN', label: 'Powrót' },
  { type: 'MEMBER_LEFT', label: 'Opuszczenie' },
];

export default function AlertsScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const [type, setType] = useState<AlertType | undefined>(undefined);
  const alerts = useAlerts(id, type);
  const now = useNow();
  const items = alerts.data?.pages.flatMap((page) => page.content) ?? [];

  return (
    <Screen>
      <View style={styles.chips}>
        {FILTERS.map((filter) => (
          <Chip key={filter.label} label={filter.label} selected={filter.type === type} onPress={() => setType(filter.type)} />
        ))}
      </View>
      <ErrorText error={alerts.error} />
      {alerts.isPending ? (
        <Loading />
      ) : (
        <FlatList
          data={items}
          keyExtractor={(alert) => alert.id}
          contentContainerStyle={styles.list}
          extraData={now}
          onEndReached={() => {
            if (alerts.hasNextPage && !alerts.isFetchingNextPage) {
              void alerts.fetchNextPage();
            }
          }}
          onRefresh={() => void alerts.refetch()}
          refreshing={alerts.isRefetching && !alerts.isFetchingNextPage}
          ListEmptyComponent={<Text style={styles.empty}>Brak alertów.</Text>}
          renderItem={({ item }) => (
            <View style={styles.card}>
              <Text style={styles.text}>{alertText(item)}</Text>
              <Text style={styles.time}>{formatDuration(now.getTime() - Date.parse(item.createdAt))} temu</Text>
            </View>
          )}
        />
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  list: { gap: 8, paddingBottom: 16 },
  empty: { color: colors.muted, fontSize: 16, paddingTop: 16 },
  card: { padding: 14, borderRadius: 8, borderWidth: 1, borderColor: colors.border, gap: 4 },
  text: { fontSize: 16, fontWeight: '600', color: colors.text },
  time: { fontSize: 14, color: colors.muted },
});
