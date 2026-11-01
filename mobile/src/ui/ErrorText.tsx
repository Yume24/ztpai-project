import { StyleSheet, Text } from 'react-native';
import { errorMessage } from '@/api/errors';
import { colors } from './theme';

export function ErrorText({ error }: { error: unknown }) {
  if (!error) {
    return null;
  }
  return <Text style={styles.text}>{errorMessage(error)}</Text>;
}

const styles = StyleSheet.create({ text: { color: colors.danger, fontSize: 14 } });
