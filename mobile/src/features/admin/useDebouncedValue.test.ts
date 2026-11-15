import { act, renderHook } from '@testing-library/react-native';
import { useDebouncedValue } from './useDebouncedValue';

beforeEach(() => jest.useFakeTimers());
afterEach(() => jest.useRealTimers());

test('useDebouncedValue returns the latest value only after the delay', async () => {
  const { result, rerender } = await renderHook((props: { value: string }) => useDebouncedValue(props.value, 300), {
    initialProps: { value: 'a' },
  });
  expect(result.current).toBe('a');

  await rerender({ value: 'ab' });
  await act(async () => {
    jest.advanceTimersByTime(299);
  });
  expect(result.current).toBe('a');

  await rerender({ value: 'abc' });
  await act(async () => {
    jest.advanceTimersByTime(299);
  });
  expect(result.current).toBe('a');

  await act(async () => {
    jest.advanceTimersByTime(1);
  });
  expect(result.current).toBe('abc');
});
