import { isRecord } from './isRecord';

test('isRecord accepts objects and rejects null and primitives', () => {
  expect(isRecord({ a: 1 })).toBe(true);
  expect(isRecord({})).toBe(true);
  expect(isRecord(null)).toBe(false);
  expect(isRecord(undefined)).toBe(false);
  expect(isRecord('text')).toBe(false);
  expect(isRecord(42)).toBe(false);
});
