import { useEffect, useMemo, useRef, type ReactNode } from 'react';
import { StyleSheet } from 'react-native';
import MapView, { Circle, Marker, Polyline, type LatLng } from 'react-native-maps';
import type { GeofenceResponse, MemberResponse } from '@/api/types';
import type { Position, TrailPoint } from '@/realtime/types';
import { colorForUser } from './colors';

type Props = {
  positions: Position[];
  members: MemberResponse[];
  geofence: GeofenceResponse | null;
  trail: TrailPoint[] | null;
  selectedUserId: string | null;
  onSelectUser: (userId: string) => void;
  onPress?: (coordinate: LatLng) => void;
  onRegionChangeComplete?: (center: LatLng) => void;
  children?: ReactNode;
};

const FENCE_FILL_ALPHA = '22';
const TRAIL_WIDTH = 4;
const EDGE_PADDING = { top: 60, right: 60, bottom: 60, left: 60 };
const DEFAULT_REGION = { latitude: 52, longitude: 19, latitudeDelta: 12, longitudeDelta: 12 };

export function RoomMap({ positions, members, geofence, trail, selectedUserId, onSelectUser, onPress, onRegionChangeComplete, children }: Props) {
  const mapRef = useRef<MapView>(null);
  const fitted = useRef(false);
  const names = useMemo(() => new Map(members.map((m) => [m.userId, m.displayName])), [members]);

  useEffect(() => {
    if (fitted.current || positions.length === 0) {
      return;
    }
    fitted.current = true;
    mapRef.current?.fitToCoordinates(
      positions.map((p) => ({ latitude: p.lat, longitude: p.lon })),
      { edgePadding: EDGE_PADDING, animated: false },
    );
  }, [positions]);

  return (
    <MapView
      ref={mapRef}
      style={styles.map}
      showsUserLocation
      initialRegion={
        geofence
          ? { latitude: geofence.lat, longitude: geofence.lon, latitudeDelta: 0.05, longitudeDelta: 0.05 }
          : DEFAULT_REGION
      }
      onPress={onPress ? (event) => onPress(event.nativeEvent.coordinate) : undefined}
      onRegionChangeComplete={
        onRegionChangeComplete
          ? (region) => onRegionChangeComplete({ latitude: region.latitude, longitude: region.longitude })
          : undefined
      }
    >
      {geofence ? (
        <Circle
          center={{ latitude: geofence.lat, longitude: geofence.lon }}
          radius={geofence.radiusM}
          strokeColor="#dc2626"
          fillColor={`#dc2626${FENCE_FILL_ALPHA}`}
        />
      ) : null}
      {trail && trail.length > 1 && selectedUserId ? (
        <Polyline
          coordinates={trail.map((p) => ({ latitude: p.lat, longitude: p.lon }))}
          strokeColor={colorForUser(selectedUserId)}
          strokeWidth={TRAIL_WIDTH}
        />
      ) : null}
      {positions.map((p) => (
        <Marker
          key={p.userId}
          coordinate={{ latitude: p.lat, longitude: p.lon }}
          title={names.get(p.userId) ?? 'Nieznany'}
          pinColor={colorForUser(p.userId)}
          onPress={() => onSelectUser(p.userId)}
        />
      ))}
      {children}
    </MapView>
  );
}

const styles = StyleSheet.create({
  map: { height: 300, borderRadius: 12, overflow: 'hidden' },
});
