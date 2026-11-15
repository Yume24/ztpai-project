import { StyleSheet, Text, View } from 'react-native';
import { Button } from '@/ui/Button';
import { DateTimeField } from '@/ui/DateTimeField';
import { colors } from '@/ui/theme';

type Props = { label: string; value: Date | undefined; onChange: (value: Date | undefined) => void };

export function OptionalDateField({ label, value, onChange }: Props) {
  if (!value) {
    return (
      <View style={styles.container}>
        <Text style={styles.label}>{label}</Text>
        <Button title="Ustaw" variant="secondary" onPress={() => onChange(new Date())} />
      </View>
    );
  }
  return (
    <View style={styles.container}>
      <DateTimeField label={label} value={value} onChange={onChange} />
      <Button title="Wyczyść" variant="secondary" onPress={() => onChange(undefined)} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, gap: 4 },
  label: { fontSize: 14, fontWeight: '600', color: colors.text },
});
