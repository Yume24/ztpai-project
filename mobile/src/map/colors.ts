const SATURATION = 0.65;
const LIGHTNESS = 0.45;
const HUES = 360;

function hash(value: string): number {
  let h = 0;
  for (let i = 0; i < value.length; i++) {
    h = (Math.imul(h, 31) + value.charCodeAt(i)) >>> 0;
  }
  return h;
}

function hslToHex(hue: number, s: number, l: number): string {
  const a = s * Math.min(l, 1 - l);
  const channel = (n: number) => {
    const k = (n + hue / 30) % 12;
    const value = l - a * Math.max(-1, Math.min(k - 3, 9 - k, 1));
    return Math.round(value * 255)
      .toString(16)
      .padStart(2, '0');
  };
  return `#${channel(0)}${channel(8)}${channel(4)}`;
}

export function colorForUser(userId: string): string {
  return hslToHex(hash(userId) % HUES, SATURATION, LIGHTNESS);
}
