import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { Link } from 'expo-router';
import { useForm } from 'react-hook-form';
import { StyleSheet, Text } from 'react-native';
import { register } from '@/auth/authApi';
import { useAuthStore } from '@/auth/authStore';
import { registerSchema, type RegisterValues } from '@/auth/schemas';
import { Button } from '@/ui/Button';
import { ErrorText } from '@/ui/ErrorText';
import { FormTextField } from '@/ui/FormTextField';
import { Screen } from '@/ui/Screen';
import { applyFieldErrors } from '@/ui/applyFieldErrors';
import { colors } from '@/ui/theme';

export default function RegisterScreen() {
  const signIn = useAuthStore((state) => state.signIn);
  const { control, handleSubmit, setError } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { email: '', password: '', displayName: '' },
  });
  const mutation = useMutation({
    mutationFn: register,
    onSuccess: signIn,
    onError: (error) => applyFieldErrors(error, setError),
  });

  return (
    <Screen scroll insetTop>
      <Text style={styles.title}>Załóż konto</Text>
      <FormTextField control={control} name="displayName" label="Nazwa wyświetlana" autoComplete="name" />
      <FormTextField
        control={control}
        name="email"
        label="E-mail"
        autoCapitalize="none"
        autoComplete="email"
        keyboardType="email-address"
      />
      <FormTextField
        control={control}
        name="password"
        label="Hasło"
        autoComplete="new-password"
        secureTextEntry
      />
      <ErrorText error={mutation.error} />
      <Button
        title="Zarejestruj"
        loading={mutation.isPending}
        onPress={handleSubmit((values) => mutation.mutate(values))}
      />
      <Link href="/login" style={styles.link}>
        Masz już konto? Zaloguj się
      </Link>
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 28, fontWeight: '700', color: colors.text, marginTop: 32 },
  link: { color: colors.primary, textAlign: 'center', paddingVertical: 8 },
});
