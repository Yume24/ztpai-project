import { ActivityIndicator, Pressable, StyleSheet, Text } from 'react-native';
import { colors } from './theme';

type Props = {
  title: string;
  onPress: () => void;
  variant?: 'primary' | 'secondary' | 'danger';
  disabled?: boolean;
  loading?: boolean;
};

export function Button({ title, onPress, variant = 'primary', disabled = false, loading = false }: Props) {
  const inactive = disabled || loading;
  const filled = variant !== 'secondary';
  const tint = variant === 'danger' ? colors.danger : colors.primary;
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      disabled={inactive}
      style={[
        styles.button,
        filled ? { backgroundColor: tint } : { borderColor: tint, borderWidth: 1 },
        inactive && styles.inactive,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={filled ? colors.background : tint} />
      ) : (
        <Text style={[styles.title, { color: filled ? colors.background : tint }]}>{title}</Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: { minHeight: 48, borderRadius: 8, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 16 },
  title: { fontSize: 16, fontWeight: '600' },
  inactive: { opacity: 0.5 },
});
