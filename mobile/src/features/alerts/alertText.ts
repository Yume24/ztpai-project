import type { AlertResponse } from '@/api/types';

export function alertText(alert: AlertResponse): string {
  const person = alert.subject.displayName;
  switch (alert.type) {
    case 'GEOFENCE_EXIT':
      return `${person} wychodzi poza strefę`;
    case 'GEOFENCE_RETURN':
      return `${person} wraca do strefy`;
    case 'MEMBER_LEFT':
      return `${person} opuszcza pokój`;
  }
}
