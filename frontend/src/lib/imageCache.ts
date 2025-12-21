const imageCache = new Map<string, Promise<HTMLImageElement>>();

export const preloadImage = (url?: string | null): Promise<HTMLImageElement | null> => {
  if (!url) {
    return Promise.resolve(null);
  }
  const cached = imageCache.get(url);
  if (cached) {
    return cached;
  }
  const promise = new Promise<HTMLImageElement>((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = (err) => {
      imageCache.delete(url);
      reject(err);
    };
    img.src = url;
  });
  imageCache.set(url, promise);
  return promise;
};

export const getCachedImage = (url: string) => imageCache.get(url);
