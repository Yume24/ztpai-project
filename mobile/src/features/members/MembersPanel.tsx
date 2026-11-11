import { useMemo, type ReactElement } from 'react';
import { FlatList, StyleSheet } from 'react-native';
import type { MemberResponse, RoomDetailsResponse } from '@/api/types';
import type { Position } from '@/realtime/types';
import { useNow } from '@/ui/useNow';
import { lastSeenLabel } from './lastSeen';
import { MemberRow } from './MemberRow';
import { canSeePosition } from './visibility';

type Props = {
  members: MemberResponse[];
  positions: Position[];
  room: Pick<RoomDetailsResponse, 'status' | 'visibilityMode' | 'myRole'>;
  myUserId: string | undefined;
  selectedUserId: string | null;
  onSelect: (userId: string) => void;
  header?: ReactElement;
  footer?: ReactElement;
};

export function MembersPanel({ members, positions, room, myUserId, selectedUserId, onSelect, header, footer }: Props) {
  const now = useNow();
  const recordedAt = useMemo(() => new Map(positions.map((p) => [p.userId, p.recordedAt])), [positions]);
  const extraData = useMemo(
    () => ({ selectedUserId, now, recordedAt, room, myUserId }),
    [selectedUserId, now, recordedAt, room, myUserId],
  );

  return (
    <FlatList
      data={members}
      keyExtractor={(member) => member.userId}
      extraData={extraData}
      ListHeaderComponent={header}
      ListFooterComponent={footer}
      contentContainerStyle={styles.list}
      renderItem={({ item: member }) => {
        const isMe = member.userId === myUserId;
        const visible = canSeePosition(room.visibilityMode, room.myRole, { role: member.role, isMe });
        const caption = visible ? lastSeenLabel(recordedAt.get(member.userId), room.status, now) : null;
        return (
          <MemberRow
            member={member}
            caption={caption}
            selected={member.userId === selectedUserId}
            onPress={() => onSelect(member.userId)}
          />
        );
      }}
    />
  );
}

const styles = StyleSheet.create({
  list: { gap: 4, paddingBottom: 16 },
});
