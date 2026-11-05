import DateTimePicker, { DateTimePickerAndroid } from '@react-native-community/datetimepicker';
import { Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { colors } from './theme';

type Props = {
  label: string;
  value: Date;
  onChange: (value: Date) => void;
  minimumDate?: Date;
  error?: string;
};

function formatValue(value: Date): string {
  return value.toLocaleString('pl-PL', { dateStyle: 'medium', timeStyle: 'short' });
}

export function DateTimeField({ label, value, onChange, minimumDate, error }: Props) {
  const openAndroidPicker = () => {
    DateTimePickerAndroid.open({
      value,
      mode: 'date',
      minimumDate,
      onValueChange: (_event, date) => {
        DateTimePickerAndroid.open({
          value: date,
          mode: 'time',
          is24Hour: true,
          onValueChange: (_timeEvent, picked) => onChange(picked),
        });
      },
    });
  };

  return (
    <View style={styles.container}>
      <Text style={styles.label}>{label}</Text>
      {Platform.OS === 'android' ? (
        <Pressable accessibilityRole="button" onPress={openAndroidPicker} style={styles.input}>
          <Text style={styles.value}>{formatValue(value)}</Text>
        </Pressable>
      ) : (
        <DateTimePicker
          value={value}
          mode="datetime"
          display="compact"
          locale="pl-PL"
          minimumDate={minimumDate}
          onValueChange={(_event, date) => onChange(date)}
          style={styles.iosPicker}
        />
      )}
      {error ? <Text style={styles.error}>{error}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { gap: 4 },
  label: { fontSize: 14, fontWeight: '600', color: colors.text },
  input: {
    minHeight: 48,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    paddingHorizontal: 12,
    justifyContent: 'center',
  },
  value: { fontSize: 16, color: colors.text },
  iosPicker: { alignSelf: 'flex-start' },
  error: { fontSize: 13, color: colors.danger },
});
