import { useEffect, useRef } from 'react';
import '@cap.js/widget';

type CapWidgetProps = {
  apiEndpoint: string;
  onSolved: (token: string) => void;
  onError?: (message: string) => void;
};

const CapWidget = ({ apiEndpoint, onSolved, onError }: CapWidgetProps) => {
  const ref = useRef<HTMLElement | null>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const solveHandler = (event: Event) => {
      const detail = (event as CustomEvent).detail || {};
      if (detail?.token) {
        onSolved(detail.token);
      }
    };

    const errorHandler = (event: Event) => {
      const detail = (event as CustomEvent).detail || {};
      const message = detail?.message || detail?.error || 'CAP 验证失败';
      onError?.(message);
    };

    el.addEventListener('solve', solveHandler as EventListener);
    el.addEventListener('error', errorHandler as EventListener);
    return () => {
      el.removeEventListener('solve', solveHandler as EventListener);
      el.removeEventListener('error', errorHandler as EventListener);
    };
  }, [onSolved, onError]);

  return (
    <cap-widget
      ref={ref as any}
      data-cap-api-endpoint={apiEndpoint}
      className="w-full"
    />
  );
};

export default CapWidget;
