import Slider from '@react-native-community/slider';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';
import type { RoomDetailsResponse } from '@/api/types';
import { useDeleteGeofence, useSaveGeofence } from '@/features/geofence/hooks';
import { useLatestPositions } from '@/features/locations/hooks';
import { useMembers } from '@/features/members/hooks';
import { useRoom } from '@/features/rooms/hooks';
import { RoomMap } from '@/map/RoomMap';
import { Button } from '@/ui/Button';
import { ErrorText } from '@/ui/ErrorText';
import { Loading } from '@/ui/Loading';
import { Screen } from '@/ui/Screen';
import { colors } from '@/ui/theme';

const MIN_RADIUS_M = 50;
const MAX_RADIUS_M = 5000;
const RADIUS_STEP_M = 50;
const DEFAULT_RADIUS_M = 500;

type Center = { lat: number; lon: number };

export default function GeofenceScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const room = useRoom(id);

  if (room.isPending) {
    return <Loading />;
  }
  if (!room.data) {
    return (
      <Screen>
        <ErrorText error={room.error} />
        <Button title="Spróbuj ponownie" onPress={() => void room.refetch()} />
      </Screen>
    );
  }
  return <GeofenceEditor room={room.data} />;
}

function GeofenceEditor({ room }: { room: RoomDetailsResponse }) {
  const router = useRouter();
  const members = useMembers(room.id);
  const { positions, error: positionsError } = useLatestPositions(room.id);
  const save = useSaveGeofence(room.id);
  const remove = useDeleteGeofence(room.id);
  const existing = room.geofence ?? null;
  const [picked, setPicked] = useState<Center | null>(null);
  const [mapCenter, setMapCenter] = useState<Center | null>(null);
  const [radiusM, setRadiusM] = useState(existing?.radiusM ?? DEFAULT_RADIUS_M);
  const center = picked ?? existing ?? mapCenter;
  const goBack = () => router.back();

  return (
    <Screen>
      <RoomMap
        positions={positions}
        members={members.data ?? []}
        geofence={center ? { ...center, radiusM } : null}
        trail={null}
        selectedUserId={null}
        onSelectUser={() => {}}
        onPress={({ latitude, longitude }) => setPicked({ lat: latitude, lon: longitude })}
        onRegionChangeComplete={({ latitude, longitude }) => setMapCenter({ lat: latitude, lon: longitude })}
      />
      <Text style={styles.hint}>Dotknij mapy, aby przesunąć środek strefy.</Text>
      <View>
        <Text style={styles.radius}>Promień: {radiusM} m</Text>
        <Slider
          minimumValue={MIN_RADIUS_M}
          maximumValue={MAX_RADIUS_M}
          step={RADIUS_STEP_M}
          value={radiusM}
          onValueChange={setRadiusM}
        />
      </View>
      <ErrorText error={save.error ?? remove.error ?? members.error ?? positionsError} />
      <Button
        title="Zapisz"
        disabled={!center}
        loading={save.isPending}
        onPress={() => center && save.mutate({ ...center, radiusM }, { onSuccess: goBack })}
      />
      {existing ? (
        <Button
          title="Usuń strefę"
          variant="danger"
          loading={remove.isPending}
          onPress={() =>
            Alert.alert('Usunąć strefę?', 'Alerty o wyjściu i powrocie przestaną być generowane.', [
              { text: 'Anuluj', style: 'cancel' },
              { text: 'Usuń', style: 'destructive', onPress: () => remove.mutate(undefined, { onSuccess: goBack }) },
            ])
          }
        />
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  hint: { fontSize: 14, color: colors.muted },
  radius: { fontSize: 16, fontWeight: '600', color: colors.text },
});
