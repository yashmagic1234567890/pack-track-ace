import { useEffect, useRef, useState } from 'react';

/**
 * Camera-based barcode/QR scanner using html5-qrcode. Calls onDetected(code)
 * for each successful decode. Lazy-loads the library so the rest of the app
 * works even if the dependency is unavailable.
 */
export default function BarcodeScanner({ onDetected, onClose }) {
  const containerId = 'ft-scanner-region';
  const scannerRef = useRef(null);
  const [error, setError] = useState('');
  const lastRef = useRef({ code: null, at: 0 });

  useEffect(() => {
    let active = true;

    (async () => {
      try {
        const mod = await import('html5-qrcode');
        if (!active) return;
        const { Html5Qrcode } = mod;
        const scanner = new Html5Qrcode(containerId);
        scannerRef.current = scanner;

        await scanner.start(
          { facingMode: 'environment' },
          { fps: 10, qrbox: { width: 250, height: 150 } },
          (decodedText) => {
            // De-bounce identical reads within 1.2s
            const now = Date.now();
            if (lastRef.current.code === decodedText && now - lastRef.current.at < 1200) return;
            lastRef.current = { code: decodedText, at: now };
            onDetected(decodedText);
          },
          () => {}
        );
      } catch (e) {
        setError(
          'Unable to access camera. Use HTTPS/localhost and grant camera permission, or enter the SKU manually.'
        );
      }
    })();

    return () => {
      active = false;
      const s = scannerRef.current;
      if (s) {
        s.stop().then(() => s.clear()).catch(() => {});
      }
    };
  }, [onDetected]);

  return (
    <div className="scanner-overlay">
      <div className="scanner-modal">
        <div className="scanner-head">
          <strong>📷 Camera Scanner</strong>
          <button className="btn btn-ghost" onClick={onClose}>
            ✕
          </button>
        </div>
        <div id={containerId} className="scanner-region" />
        {error && <p className="error-text">{error}</p>}
        <p className="muted small">Point the camera at the item barcode. Each scan adds +1.</p>
      </div>
    </div>
  );
}
