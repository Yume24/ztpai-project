import { useState } from 'react';
import { Alert, FlatList, StyleSheet, Text, View } from 'react-native';
import type { AdminUserResponse } from '@/api/types';
import { Button } from '@/ui/Button';
import { Chip } from '@/ui/Chip';
import { ErrorText } from '@/ui/ErrorText';
import { Loading } from '@/ui/Loading';
import { TextField } from '@/ui/TextField';
import { colors } from '@/ui/theme';
import { useAdminUsers, useBlockUser, useUnblockUser } from './hooks';
import { useDebouncedValue } from './useDebouncedValue';

const FILTERS: { blocked: boolean | undefined; label: string }[] = [
  { blocked: undefined, label: 'Wszyscy' },
  { blocked: true, label: 'Zablokowani' },
  { blocked: false, label: 'Aktywni' },
];

export function AdminUsers() {
  const [search, setSearch] = useState('');
  const [blocked, setBlocked] = useState<boolean | undefined>(undefined);
  const users = useAdminUsers(useDebouncedValue(search.trim()), blocked);
  const block = useBlockUser();
  const unblock = useUnblockUser();
  const items = users.data?.pages.flatMap((page) => page.content) ?? [];

  const confirmToggle = (user: AdminUserResponse) => {
    const blocking = !user.blocked;
    Alert.alert(
      blocking ? 'Zablokować użytkownika?' : 'Odblokować użytkownika?',
      `${user.displayName} (${user.email})`,
      [
        { text: 'Anuluj', style: 'cancel' },
        {
          text: blocking ? 'Zablokuj' : 'Odblokuj',
          style: blocking ? 'destructive' : 'default',
          onPress: () => {
            (blocking ? unblock : block).reset();
            (blocking ? block : unblock).mutate(user.id);
          },
        },
      ],
    );
  };

  return (
    <>
      <TextField
        label="Szukaj"
        placeholder="E-mail lub nazwa"
        value={search}
        onChangeText={setSearch}
        autoCapitalize="none"
        autoCorrect={false}
      />
      <View style={styles.row}>
        {FILTERS.map((filter) => (
          <Chip
            key={filter.label}
            label={filter.label}
            selected={filter.blocked === blocked}
            onPress={() => setBlocked(filter.blocked)}
          />
        ))}
      </View>
      <ErrorText error={users.error ?? block.error ?? unblock.error} />
      {users.isPending ? (
        <Loading />
      ) : (
        <FlatList
          data={items}
          keyExtractor={(user) => user.id}
          contentContainerStyle={styles.list}
          keyboardShouldPersistTaps="handled"
          onEndReached={() => {
            if (users.hasNextPage && !users.isFetchingNextPage) {
              void users.fetchNextPage();
            }
          }}
          onRefresh={() => void users.refetch()}
          refreshing={users.isRefetching && !users.isFetchingNextPage}
          ListEmptyComponent={<Text style={styles.empty}>Brak użytkowników.</Text>}
          renderItem={({ item }) => (
            <View style={styles.card}>
              <View style={styles.info}>
                <Text style={styles.name}>{item.displayName}</Text>
                <Text style={styles.meta}>{item.email}</Text>
                <Text style={styles.meta}>
                  {item.role === 'ADMIN' ? 'Administrator' : 'Użytkownik'}
                  {item.blocked ? ' · Zablokowany' : ''}
                </Text>
              </View>
              {item.role === 'ADMIN' ? null : (
                <Button
                  title={item.blocked ? 'Odblokuj' : 'Zablokuj'}
                  variant={item.blocked ? 'secondary' : 'danger'}
                  onPress={() => confirmToggle(item)}
                />
              )}
            </View>
          )}
        />
      )}
    </>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', gap: 8 },
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
