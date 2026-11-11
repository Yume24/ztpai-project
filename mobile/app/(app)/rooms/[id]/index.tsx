import { useLocalSearchParams, useRouter } from 'expo-router';
import { Stack } from 'expo-router/js-stack';
import { useState } from 'react';
import { Alert, Share, StyleSheet, Text, View } from 'react-native';
import { ApiError } from '@/api/errors';
import { useAuthStore } from '@/auth/authStore';
import { useLatestPositions, useTrail } from '@/features/locations/hooks';
import { MembersPanel } from '@/features/members/MembersPanel';
import { useMembers } from '@/features/members/hooks';
import { statusLine } from '@/features/rooms/format';
import { useCloseRoom, useDeleteRoom, useLeaveRoom, useRoom } from '@/features/rooms/hooks';
import { inviteLink, inviteMessage } from '@/features/rooms/invite';
import { availableActions, type RoomAction } from '@/features/rooms/permissions';
import { RoomMap } from '@/map/RoomMap';
import { Button } from '@/ui/Button';
import { ErrorText } from '@/ui/ErrorText';
import { Loading } from '@/ui/Loading';
import { Screen } from '@/ui/Screen';
import { colors } from '@/ui/theme';
import { useNow } from '@/ui/useNow';

type SubScreen = '/rooms/[id]/settings' | '/rooms/[id]/geofence' | '/rooms/[id]/alerts' | '/rooms/[id]/members';

const SCREEN_ACTIONS: { action: RoomAction; title: string; pathname: SubScreen }[] = [
  { action: 'settings', title: 'Ustawienia', pathname: '/rooms/[id]/settings' },
  { action: 'geofence', title: 'Strefa', pathname: '/rooms/[id]/geofence' },
  { action: 'alerts', title: 'Alerty', pathname: '/rooms/[id]/alerts' },
  { action: 'members', title: 'Członkowie', pathname: '/rooms/[id]/members' },
];

export default function RoomDetailsScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const room = useRoom(id);
  const leave = useLeaveRoom(id);
  const close = useCloseRoom(id);
  const remove = useDeleteRoom(id);
  const members = useMembers(id);
  const { positions, error: positionsError } = useLatestPositions(id);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const { trail, error: trailError, forbiddenMessage: trailMessage } = useTrail(id, selectedUserId);
  const myUserId = useAuthStore((state) => state.user?.id);
  const now = useNow();
  const toggleSelected = (userId: string) => setSelectedUserId((current) => (current === userId ? null : userId));
  const goBack = () => router.back();

  if (room.isPending) {
    return <Loading />;
  }
  if (room.error instanceof ApiError && room.error.status === 404) {
    return (
      <Screen>
        <Text style={styles.message}>Nie znaleziono pokoju.</Text>
        <Button title="Wróć" onPress={goBack} />
      </Screen>
    );
  }
  if (!room.data) {
    return (
      <Screen>
        <ErrorText error={room.error} />
        <Button title="Spróbuj ponownie" onPress={() => void room.refetch()} />
        <Button title="Wróć" variant="secondary" onPress={goBack} />
      </Screen>
    );
  }

  const details = room.data;
  const actions = availableActions(details.myRole, details.status);
  const confirm = (title: string, message: string, confirmLabel: string, run: () => void) =>
    Alert.alert(title, message, [
      { text: 'Anuluj', style: 'cancel' },
      { text: confirmLabel, style: 'destructive', onPress: run },
    ]);

  return (
    <Screen>
      <Stack.Screen options={{ title: details.name }} />
      <Text style={styles.title}>{details.name}</Text>
      <Text style={styles.status}>{statusLine(details, now)}</Text>
      <RoomMap
        positions={positions}
        members={members.data ?? []}
        geofence={details.geofence ?? null}
        trail={trail}
        selectedUserId={selectedUserId}
        onSelectUser={toggleSelected}
      />
      {trailMessage ? <Text style={styles.message}>{trailMessage}</Text> : null}
      <ErrorText error={trailError ?? members.error ?? positionsError} />
      <MembersPanel
        members={members.data ?? []}
        positions={positions}
        room={details}
        myUserId={myUserId}
        selectedUserId={selectedUserId}
        onSelect={toggleSelected}
        header={
          <View>
            <Text style={styles.meta}>
              Kod pokoju: <Text style={styles.code}>{details.code}</Text>
            </Text>
            <Text style={styles.meta}>
              Właściciel: {details.owner.displayName} · {details.memberCount} os.
            </Text>
          </View>
        }
        footer={
          <View>
            <View style={styles.actions}>
              {actions.includes('share') ? (
                <Button
                  title="Udostępnij zaproszenie"
                  onPress={() =>
                    void Share.share({
                      message: inviteMessage(details.name, details.code, inviteLink(details.code)),
                    })
                  }
                />
              ) : null}
              {SCREEN_ACTIONS.filter(({ action }) => actions.includes(action)).map(({ action, title, pathname }) => (
                <Button
                  key={action}
                  title={title}
                  variant="secondary"
                  onPress={() => router.push({ pathname, params: { id } })}
                />
              ))}
              {actions.includes('leave') ? (
                <Button
                  title="Opuść pokój"
                  variant="danger"
                  loading={leave.isPending}
                  onPress={() =>
                    confirm('Opuścić pokój?', 'Przestaniesz udostępniać lokalizację w tym pokoju.', 'Opuść', () =>
                      leave.mutate(undefined, { onSuccess: goBack }),
                    )
                  }
                />
              ) : null}
              {actions.includes('close') ? (
                <Button
                  title="Zakończ pokój"
                  variant="danger"
                  loading={close.isPending}
                  onPress={() => confirm('Zakończyć pokój?', 'Pokój zakończy się od razu dla wszystkich.', 'Zakończ', () => close.mutate())}
                />
              ) : null}
              {actions.includes('delete') ? (
                <Button
                  title="Usuń pokój"
                  variant="danger"
                  loading={remove.isPending}
                  onPress={() =>
                    confirm('Usunąć pokój?', 'Pokój i jego dane zostaną usunięte bezpowrotnie.', 'Usuń', () =>
                      remove.mutate(undefined, { onSuccess: goBack }),
                    )
                  }
                />
              ) : null}
            </View>
            <ErrorText error={leave.error ?? close.error ?? remove.error} />
          </View>
        }
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 28, fontWeight: '700', color: colors.text, marginTop: 16 },
  status: { fontSize: 18, color: colors.primary, fontWeight: '600' },
  meta: { fontSize: 15, color: colors.muted },
  code: { fontWeight: '700', color: colors.text, letterSpacing: 2 },
  message: { fontSize: 16, color: colors.text },
  actions: { gap: 8, marginTop: 8 },
});
