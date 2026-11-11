import { useRouter } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text } from 'react-native';
import { currentMode } from '@/location/mode';
import { requestPermissions, type PermissionResult } from '@/location/permissions';
import { refreshTracking } from '@/location/tracking';
import { Button } from '@/ui/Button';
import { ErrorText } from '@/ui/ErrorText';
import { Screen } from '@/ui/Screen';
import { colors } from '@/ui/theme';

const OUTCOME_MESSAGES: Record<PermissionResult, string> = {
  granted: 'Gotowe. Twoja lokalizacja będzie udostępniana, gdy tylko trwa pokój.',
  'foreground-only':
    'Bez zgody „zawsze” śledzenie działa tylko przy otwartej aplikacji. Możesz to zmienić w ustawieniach systemu.',
  denied: 'Bez zgody na lokalizację nie możemy jej udostępniać. Możesz to zmienić w ustawieniach systemu.',
};

export default function LocationPermissionScreen() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [outcome, setOutcome] = useState<PermissionResult | null>(null);
  const [error, setError] = useState<unknown>(null);

  const allow = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await requestPermissions(currentMode());
      setOutcome(result);
      if (result !== 'denied') {
        await refreshTracking();
      }
      if (result === 'granted') {
        router.back();
      }
    } catch (e) {
      setError(e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Screen scroll>
      <Text style={styles.title}>Udostępnianie lokalizacji</Text>
      <Text style={styles.text}>
        Rendezvous potrzebuje twojej lokalizacji, żeby pokazać cię innym uczestnikom pokoju. Lokalizacja jest
        udostępniana wyłącznie w czasie trwania pokoju, w którym jesteś.
      </Text>
      <Text style={styles.text}>
        Gdy aplikacja działa w tle, w pasku powiadomień pojawi się stałe powiadomienie, że udostępniasz lokalizację.
      </Text>
      <ErrorText error={error} />
      {outcome ? <Text style={styles.text}>{OUTCOME_MESSAGES[outcome]}</Text> : null}
      {outcome && outcome !== 'granted' ? (
        <Button title="Zamknij" variant="secondary" onPress={() => router.back()} />
      ) : (
        <Button title="Zezwól" onPress={() => void allow()} loading={loading} />
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 22, fontWeight: '700', color: colors.text },
  text: { fontSize: 16, color: colors.text },
});
