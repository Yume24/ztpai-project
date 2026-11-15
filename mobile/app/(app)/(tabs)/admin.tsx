import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { AdminRooms } from '@/features/admin/AdminRooms';
import { AdminUsers } from '@/features/admin/AdminUsers';
import { Chip } from '@/ui/Chip';
import { Screen } from '@/ui/Screen';

type Section = 'users' | 'rooms';

export default function AdminScreen() {
  const [section, setSection] = useState<Section>('users');

  return (
    <Screen>
      <View style={styles.row}>
        <Chip label="Użytkownicy" selected={section === 'users'} onPress={() => setSection('users')} />
        <Chip label="Pokoje" selected={section === 'rooms'} onPress={() => setSection('rooms')} />
      </View>
      {section === 'users' ? <AdminUsers /> : <AdminRooms />}
    </Screen>
  );
}

const styles = StyleSheet.create({ row: { flexDirection: 'row', gap: 8 } });
