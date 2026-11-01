import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { Link } from 'expo-router';
import { useForm } from 'react-hook-form';
import { StyleSheet, Text } from 'react-native';
import { login } from '@/auth/authApi';
import { useAuthStore } from '@/auth/authStore';
import { loginSchema, type LoginValues } from '@/auth/schemas';
import { Button } from '@/ui/Button';
import { ErrorText } from '@/ui/ErrorText';
import { FormTextField } from '@/ui/FormTextField';
import { Screen } from '@/ui/Screen';
import { applyFieldErrors } from '@/ui/applyFieldErrors';
import { colors } from '@/ui/theme';

export default function LoginScreen() {
  const signIn = useAuthStore((state) => state.signIn);
  const { control, handleSubmit, setError } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });
  const mutation = useMutation({
    mutationFn: login,
    onSuccess: signIn,
    onError: (error) => applyFieldErrors(error, setError),
  });

  return (
    <Screen scroll insetTop>
      <Text style={styles.title}>Zaloguj się</Text>
      <FormTextField
        control={control}
        name="email"
        label="E-mail"
        autoCapitalize="none"
        autoComplete="email"
        keyboardType="email-address"
      />
      <FormTextField control={control} name="password" label="Hasło" autoComplete="password" secureTextEntry />
      <ErrorText error={mutation.error} />
      <Button
        title="Zaloguj"
        loading={mutation.isPending}
        onPress={handleSubmit((values) => mutation.mutate(values))}
      />
      <Link href="/register" style={styles.link}>
        Nie masz konta? Zarejestruj się
      </Link>
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 28, fontWeight: '700', color: colors.text, marginTop: 32 },
  link: { color: colors.primary, textAlign: 'center', paddingVertical: 8 },
});
