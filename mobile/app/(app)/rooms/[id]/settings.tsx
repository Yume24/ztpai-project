import { zodResolver } from '@hookform/resolvers/zod';
import { useQueryClient } from '@tanstack/react-query';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { Alert, StyleSheet, Switch, Text, View } from 'react-native';
import { ApiError, errorMessage } from '@/api/errors';
import { queryKeys } from '@/api/queryKeys';
import type { RoomDetailsResponse, VisibilityMode } from '@/api/types';
import { useRoom, useUpdateRoom } from '@/features/rooms/hooks';
import {
  settingsSchema,
  toSettingsValues,
  toUpdateRoomRequest,
  type SettingsValues,
} from '@/features/rooms/settingsSchema';
import { Button } from '@/ui/Button';
import { Chip } from '@/ui/Chip';
import { DateTimeField } from '@/ui/DateTimeField';
import { ErrorText } from '@/ui/ErrorText';
import { FormTextField } from '@/ui/FormTextField';
import { Loading } from '@/ui/Loading';
import { Screen } from '@/ui/Screen';
import { applyFieldErrors } from '@/ui/applyFieldErrors';
import { colors } from '@/ui/theme';

const VISIBILITY_OPTIONS: { mode: VisibilityMode; label: string }[] = [
  { mode: 'ALL', label: 'Wszyscy widzą wszystkich' },
  { mode: 'GUARDIANS_ONLY', label: 'Tylko opiekunowie widzą wszystkich' },
];

export default function RoomSettingsScreen() {
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
  return <SettingsForm key={room.data.version} room={room.data} />;
}

function SettingsForm({ room }: { room: RoomDetailsResponse }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const updateRoom = useUpdateRoom(room.id);
  const { control, handleSubmit, setError } = useForm<SettingsValues>({
    resolver: zodResolver(settingsSchema),
    defaultValues: toSettingsValues(room),
  });
  const startsAt = useWatch({ control, name: 'startsAt' });

  const submit = handleSubmit(async (formValues) => {
    try {
      await updateRoom.mutateAsync(toUpdateRoomRequest(formValues, room));
      router.back();
    } catch (error) {
      applyFieldErrors(error, setError);
      if (error instanceof ApiError && error.code === 'ROOM_VERSION_CONFLICT') {
        Alert.alert('Konflikt zmian', errorMessage(error));
        void queryClient.invalidateQueries({ queryKey: queryKeys.room(room.id) });
      }
    }
  });

  return (
    <Screen scroll>
      <FormTextField control={control} name="name" label="Nazwa pokoju" maxLength={60} />
      {room.status === 'SCHEDULED' ? (
        <Controller
          control={control}
          name="startsAt"
          render={({ field, fieldState }) => (
            <DateTimeField
              label="Początek"
              value={field.value}
              onChange={field.onChange}
              minimumDate={new Date()}
              error={fieldState.error?.message}
            />
          )}
        />
      ) : null}
      <Controller
        control={control}
        name="endsAt"
        render={({ field, fieldState }) => (
          <DateTimeField
            label="Koniec"
            value={field.value}
            onChange={field.onChange}
            minimumDate={startsAt}
            error={fieldState.error?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="visibilityMode"
        render={({ field }) => (
          <View style={styles.group}>
            <Text style={styles.label}>Widoczność</Text>
            <View style={styles.chips}>
              {VISIBILITY_OPTIONS.map(({ mode, label }) => (
                <Chip key={mode} label={label} selected={field.value === mode} onPress={() => field.onChange(mode)} />
              ))}
            </View>
          </View>
        )}
      />
      <Controller
        control={control}
        name="membersSeeTrails"
        render={({ field }) => (
          <View style={styles.switchRow}>
            <Text style={styles.switchLabel}>Uczestnicy widzą trasy</Text>
            <Switch value={field.value} onValueChange={field.onChange} />
          </View>
        )}
      />
      <ErrorText error={updateRoom.error} />
      <Button title="Zapisz" loading={updateRoom.isPending} onPress={submit} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  group: { gap: 8 },
  label: { fontSize: 14, fontWeight: '600', color: colors.text },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  switchRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  switchLabel: { fontSize: 16, color: colors.text },
});
