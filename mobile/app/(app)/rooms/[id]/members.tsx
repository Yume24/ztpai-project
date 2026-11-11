import { useLocalSearchParams } from 'expo-router';
import { Alert, FlatList, StyleSheet, Text, View } from 'react-native';
import type { MemberResponse, RoomRole } from '@/api/types';
import { useAuthStore } from '@/auth/authStore';
import { MemberRow } from '@/features/members/MemberRow';
import { memberActions, type MemberAction } from '@/features/members/memberActions';
import { useChangeRole, useMembers, useRemoveMember } from '@/features/members/hooks';
import { useRoom } from '@/features/rooms/hooks';
import { Button } from '@/ui/Button';
import { ErrorText } from '@/ui/ErrorText';
import { Loading } from '@/ui/Loading';
import { Screen } from '@/ui/Screen';
import { colors } from '@/ui/theme';

const ACTION_TITLES: Record<MemberAction, string> = {
  promote: 'Mianuj adminem',
  demote: 'Odbierz rolę admina',
  remove: 'Usuń z pokoju',
};

export default function MembersScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const room = useRoom(id);
  const members = useMembers(id);
  const changeRole = useChangeRole(id);
  const removeMember = useRemoveMember(id);
  const myUserId = useAuthStore((state) => state.user?.id);
  const busy = changeRole.isPending || removeMember.isPending;

  if (room.isPending || members.isPending) {
    return <Loading />;
  }
  if (!room.data || !members.data) {
    return (
      <Screen>
        <ErrorText error={room.error ?? members.error} />
        <Button
          title="Spróbuj ponownie"
          onPress={() => void Promise.all([room.refetch(), members.refetch()])}
        />
      </Screen>
    );
  }
  const myRole = room.data.myRole;

  const run = (member: MemberResponse, action: MemberAction) => {
    switch (action) {
      case 'promote':
        changeRole.mutate({ userId: member.userId, body: { role: 'ADMIN' } });
        break;
      case 'demote':
        changeRole.mutate({ userId: member.userId, body: { role: 'MEMBER' } });
        break;
      case 'remove':
        Alert.alert('Usunąć z pokoju?', `${member.displayName} nie będzie mieć możliwości powrotu do tego pokoju.`, [
          { text: 'Anuluj', style: 'cancel' },
          { text: 'Usuń', style: 'destructive', onPress: () => removeMember.mutate(member.userId) },
        ]);
        break;
    }
  };

  return (
    <Screen>
      <ErrorText error={changeRole.error ?? removeMember.error} />
      <FlatList
        data={members.data}
        keyExtractor={(member) => member.userId}
        contentContainerStyle={styles.list}
        renderItem={({ item: member }) => (
          <MemberRow member={member}>
            <MemberActions
              myRole={myRole}
              member={member}
              isMe={myUserId === member.userId}
              disabled={busy}
              onAction={(action) => run(member, action)}
            />
          </MemberRow>
        )}
        ListEmptyComponent={<Text style={styles.empty}>Brak członków.</Text>}
      />
    </Screen>
  );
}

type ActionsProps = {
  myRole: RoomRole;
  member: MemberResponse;
  isMe: boolean;
  disabled: boolean;
  onAction: (action: MemberAction) => void;
};

function MemberActions({ myRole, member, isMe, disabled, onAction }: ActionsProps) {
  const actions = memberActions(myRole, { role: member.role, isMe });
  if (actions.length === 0) {
    return null;
  }
  return (
    <View style={styles.actions}>
      {actions.map((action) => (
        <View key={action} style={styles.action}>
          <Button
            title={ACTION_TITLES[action]}
            variant={action === 'remove' ? 'danger' : 'secondary'}
            disabled={disabled}
            onPress={() => onAction(action)}
          />
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  list: { gap: 8, paddingBottom: 16 },
  actions: { flexDirection: 'row', gap: 8, paddingHorizontal: 10 },
  action: { flex: 1 },
  empty: { color: colors.muted, fontSize: 16 },
});
