import type { AlertResponse } from '@/api/types';
import { alertText } from './alertText';

const alert = (type: AlertResponse['type']): AlertResponse => ({
  id: 'a1',
  type,
  subject: { userId: 'u1', displayName: 'Ania' },
  createdAt: '2026-11-14T10:00:00Z',
});

test('alertText renders the three alert types in Polish', () => {
  expect(alertText(alert('GEOFENCE_EXIT'))).toBe('Ania wychodzi poza strefę');
  expect(alertText(alert('GEOFENCE_RETURN'))).toBe('Ania wraca do strefy');
  expect(alertText(alert('MEMBER_LEFT'))).toBe('Ania opuszcza pokój');
});
