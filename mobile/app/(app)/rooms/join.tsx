import { zodResolver } from '@hookform/resolvers/zod';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useForm } from 'react-hook-form';
import { StyleSheet, Text } from 'react-native';
import { useJoinRoom } from '@/features/rooms/hooks';
import { joinSchema, type JoinValues } from '@/features/rooms/schemas';
import { Button } from '@/ui/Button';
import { ErrorText } from '@/ui/ErrorText';
import { FormTextField } from '@/ui/FormTextField';
import { Screen } from '@/ui/Screen';
import { applyFieldErrors } from '@/ui/applyFieldErrors';
import { colors } from '@/ui/theme';

export default function JoinRoomScreen() {
  const router = useRouter();
  const { code } = useLocalSearchParams<{ code?: string }>();
  const joinRoom = useJoinRoom();
  const { control, handleSubmit, setError } = useForm<JoinValues>({
    resolver: zodResolver(joinSchema),
    defaultValues: { code: code ?? '' },
  });

  const submit = handleSubmit((values) =>
    joinRoom.mutate(values, {
      onSuccess: (room) => router.replace({ pathname: '/rooms/[id]', params: { id: room.id } }),
      onError: (error) => applyFieldErrors(error, setError),
    }),
  );

  return (
    <Screen scroll>
      <Text style={styles.hint}>Po dołączeniu członkowie pokoju zobaczą twoją lokalizację do końca jego trwania.</Text>
      <FormTextField
        control={control}
        name="code"
        label="Kod pokoju"
        autoCapitalize="characters"
        autoCorrect={false}
        maxLength={12}
      />
      <ErrorText error={joinRoom.error} />
      <Button title="Dołącz" loading={joinRoom.isPending} onPress={submit} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  hint: { fontSize: 14, color: colors.muted },
});
