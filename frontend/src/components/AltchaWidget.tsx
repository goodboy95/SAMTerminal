import { useEffect, useRef } from 'react';
import 'altcha';

type AltchaWidgetProps = {
  challengeUrl: string;
  verifyUrl?: string;
  onVerified: (payload: string) => void;
  onError?: (message: string) => void;
};

const AltchaWidget = ({ challengeUrl, verifyUrl, onVerified, onError }: AltchaWidgetProps) => {
  const ref = useRef<HTMLElement | null>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const handler = (event: Event) => {
      const detail = (event as CustomEvent).detail || {};
      if (detail?.state === 'verified' && detail?.payload) {
        onVerified(detail.payload);
      }
      if (detail?.state === 'error') {
        onError?.(detail?.message || 'ALTCHA 验证失败');
      }
    };

    el.addEventListener('statechange', handler as EventListener);
    return () => {
      el.removeEventListener('statechange', handler as EventListener);
    };
  }, [onVerified, onError]);

  return (
    <altcha-widget
      ref={ref as any}
      challengeurl={challengeUrl}
      verifyurl={verifyUrl}
      className="w-full"
    />
  );
};

export default AltchaWidget;
