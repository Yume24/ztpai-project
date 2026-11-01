import type { ReactNode } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView, type Edge } from 'react-native-safe-area-context';
import { colors } from './theme';

type Props = {
  children: ReactNode;
  scroll?: boolean;
  insetTop?: boolean;
};

export function Screen({ children, scroll = false, insetTop = false }: Props) {
  const edges: Edge[] = insetTop ? ['top', 'left', 'right'] : ['left', 'right'];
  return (
    <SafeAreaView style={styles.safe} edges={edges}>
      {scroll ? (
        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          {children}
        </ScrollView>
      ) : (
        <View style={styles.fixed}>{children}</View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  content: { flexGrow: 1, padding: 16, gap: 12 },
  fixed: { flex: 1, padding: 16, gap: 12 },
});
