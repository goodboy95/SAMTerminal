import { describe, it, expect, beforeEach } from 'vitest';
import { preloadImage } from '../imageCache';

declare global {
  // eslint-disable-next-line no-var
  var Image: any;
}

class MockImage {
  static created = 0;
  onload: (() => void) | null = null;
  onerror: ((err: unknown) => void) | null = null;

  constructor() {
    MockImage.created += 1;
  }

  set src(_value: string) {
    setTimeout(() => this.onload?.(), 0);
  }
}

describe('imageCache', () => {
  beforeEach(() => {
    MockImage.created = 0;
    global.Image = MockImage;
  });

  it('dedupes the same URL', async () => {
    const first = preloadImage('https://example.com/a.png');
    const second = preloadImage('https://example.com/a.png');
    expect(first).toBe(second);
    await first;
    expect(MockImage.created).toBe(1);
  });
});
