import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'expo-router';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { useState } from 'react';
import { StyleSheet, Switch, Text, View } from 'react-native';
import { useCreateRoom } from '@/features/rooms/hooks';
import { roomFormSchema, toCreateRoomRequest, type RoomFormValues } from '@/features/rooms/schemas';
import { Button } from '@/ui/Button';
import { DateTimeField } from '@/ui/DateTimeField';
import { ErrorText } from '@/ui/ErrorText';
import { FormTextField } from '@/ui/FormTextField';
import { Screen } from '@/ui/Screen';
import { applyFieldErrors } from '@/ui/applyFieldErrors';
import { colors } from '@/ui/theme';

const HOUR_MS = 3_600_000;

export default function NewRoomScreen() {
  const router = useRouter();
  const createRoom = useCreateRoom();
  const [defaultValues] = useState<RoomFormValues>(() => ({
    name: '',
    startsAt: null,
    endsAt: new Date(Date.now() + 2 * HOUR_MS),
  }));
  const { control, handleSubmit, setError } = useForm<RoomFormValues>({
    resolver: zodResolver(roomFormSchema),
    defaultValues,
  });
  const startsAt = useWatch({ control, name: 'startsAt' });

  const submit = handleSubmit((values) =>
    createRoom.mutate(toCreateRoomRequest(values), {
      onSuccess: (room) => router.replace({ pathname: '/rooms/[id]', params: { id: room.id } }),
      onError: (error) => applyFieldErrors(error, setError),
    }),
  );

  return (
    <Screen scroll>
      <FormTextField control={control} name="name" label="Nazwa pokoju" maxLength={60} />
      <Controller
        control={control}
        name="startsAt"
        render={({ field, fieldState }) => (
          <View style={styles.group}>
            <View style={styles.switchRow}>
              <Text style={styles.switchLabel}>Start: teraz</Text>
              <Switch
                value={field.value === null}
                onValueChange={(now) => field.onChange(now ? null : new Date(Date.now() + HOUR_MS))}
              />
            </View>
            {field.value ? (
              <DateTimeField
                label="Początek"
                value={field.value}
                onChange={field.onChange}
                minimumDate={new Date()}
                error={fieldState.error?.message}
              />
            ) : null}
          </View>
        )}
      />
      <Controller
        control={control}
        name="endsAt"
        render={({ field, fieldState }) => (
          <DateTimeField
            label="Koniec"
            value={field.value}
            onChange={field.onChange}
            minimumDate={startsAt ?? new Date()}
            error={fieldState.error?.message}
          />
        )}
      />
      <ErrorText error={createRoom.error} />
      <Button title="Utwórz pokój" loading={createRoom.isPending} onPress={submit} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  group: { gap: 8 },
  switchRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  switchLabel: { fontSize: 16, color: colors.text },
});
