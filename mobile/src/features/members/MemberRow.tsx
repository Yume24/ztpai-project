import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import type { MemberResponse, RoomRole } from '@/api/types';
import { colorForUser } from '@/map/colors';
import { colors } from '@/ui/theme';

const ROLE_LABELS: Record<RoomRole, string> = {
  OWNER: 'Właściciel',
  ADMIN: 'Admin',
  MEMBER: 'Uczestnik',
};

type Props = {
  member: MemberResponse;
  caption?: string | null;
  selected?: boolean;
  onPress?: () => void;
  children?: ReactNode;
};

export function MemberRow({ member, caption, selected = false, onPress, children }: Props) {
  return (
    <View>
      <Pressable style={[styles.row, selected && styles.selected]} onPress={onPress} disabled={!onPress}>
        <View style={[styles.dot, { backgroundColor: colorForUser(member.userId) }]} />
        <View style={styles.texts}>
          <Text style={styles.name}>{member.displayName}</Text>
          {caption ? <Text style={styles.caption}>{caption}</Text> : null}
        </View>
        <Text style={styles.badge}>{ROLE_LABELS[member.role]}</Text>
      </Pressable>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 10, borderRadius: 10 },
  selected: { backgroundColor: colors.chip },
  dot: { width: 14, height: 14, borderRadius: 7 },
  texts: { flex: 1 },
  name: { fontSize: 16, fontWeight: '600', color: colors.text },
  caption: { fontSize: 13, color: colors.muted },
  badge: { fontSize: 12, color: colors.muted, fontWeight: '600' },
});
