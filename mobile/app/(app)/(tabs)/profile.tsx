import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { StyleSheet, Text } from 'react-native';
import { renameMe } from '@/auth/authApi';
import { useAuthStore } from '@/auth/authStore';
import { profileSchema, type ProfileValues } from '@/auth/schemas';
import { Button } from '@/ui/Button';
import { ErrorText } from '@/ui/ErrorText';
import { FormTextField } from '@/ui/FormTextField';
import { Screen } from '@/ui/Screen';
import { applyFieldErrors } from '@/ui/applyFieldErrors';
import { colors } from '@/ui/theme';

export default function ProfileScreen() {
  const user = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);
  const signOut = useAuthStore((state) => state.signOut);
  const { control, handleSubmit, setError } = useForm<ProfileValues>({
    resolver: zodResolver(profileSchema),
    values: { displayName: user?.displayName ?? '' },
  });
  const mutation = useMutation({
    mutationFn: (values: ProfileValues) => renameMe(values.displayName),
    onSuccess: setUser,
    onError: (error) => applyFieldErrors(error, setError),
  });

  return (
    <Screen scroll>
      <Text style={styles.label}>E-mail</Text>
      <Text style={styles.email}>{user?.email}</Text>
      <FormTextField control={control} name="displayName" label="Nazwa wyświetlana" />
      <ErrorText error={mutation.error} />
      {mutation.isSuccess ? <Text style={styles.saved}>Zapisano.</Text> : null}
      <Button
        title="Zapisz"
        loading={mutation.isPending}
        onPress={handleSubmit((values) => mutation.mutate(values))}
      />
      <Button title="Wyloguj" variant="danger" onPress={() => void signOut()} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  label: { fontSize: 14, fontWeight: '600', color: colors.text },
  email: { fontSize: 16, color: colors.muted },
  saved: { color: colors.muted, fontSize: 14 },
});
