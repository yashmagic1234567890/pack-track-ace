import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getInvoiceDetail, scan, adjust } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { useToast } from '../components/Toast';
import ProgressBar from '../components/ProgressBar';
import BarcodeScanner from '../components/BarcodeScanner';

/** Scan-to-Receive station: barcode/camera scanning with +1 increments. */
export default function ScanStation() {
  const { invoiceId } = useParams();
  const navigate = useNavigate();
  const { push } = useToast();
  const [invoice, setInvoice] = useState(null);
  const [error, setError] = useState('');
  const [manualCode, setManualCode] = useState('');
  const [cameraOn, setCameraOn] = useState(false);
  const inputRef = useRef(null);

  const load = useCallback(() => {
    getInvoiceDetail(invoiceId)
      .then(setInvoice)
      .catch((e) => setError(errorMessage(e)));
  }, [invoiceId]);

  useEffect(() => {
    load();
  }, [load]);

  const applyResult = (res) => {
    setInvoice((prev) =>
      prev
        ? {
            ...prev,
            status: res.invoiceStatus,
            totalReceived: res.invoiceTotalReceived,
            lines: prev.lines.map((l) =>
              l.id === res.lineId ? { ...l, receivedQuantity: res.receivedQuantity } : l
            ),
          }
        : prev
    );
  };

  const doScan = useCallback(
    async (code) => {
      const sku = (code || '').trim();
      if (!sku) return;
      try {
        const res = await scan({ invoiceId: Number(invoiceId), sku });
        applyResult(res);
        push(`✓ ${res.itemName}: ${res.receivedQuantity}/${res.expectedQuantity}`, 'success');
      } catch (err) {
        push(errorMessage(err, 'Scan rejected'), 'error');
      }
    },
    [invoiceId, push]
  );

  const submitManual = (e) => {
    e.preventDefault();
    doScan(manualCode);
    setManualCode('');
    inputRef.current?.focus();
  };

  const doAdjust = async (line, delta) => {
    try {
      const res = await adjust({
        invoiceId: Number(invoiceId),
        sku: line.sku,
        delta,
        reason: delta < 0 ? 'Manual correction (-)' : 'Manual correction (+)',
      });
      applyResult(res);
    } catch (err) {
      push(errorMessage(err, 'Adjustment failed'), 'error');
    }
  };

  if (error) return <div className="alert alert-error">{error}</div>;
  if (!invoice) return <div className="muted">Loading invoice…</div>;

  return (
    <div className="scan-station">
      <header className="page-head with-back">
        <button className="btn btn-ghost" onClick={() => navigate('/receiving')}>← Back</button>
        <div>
          <h2>{invoice.invoiceBusinessId}</h2>
          <p className="muted">
            {invoice.vendorName} · {invoice.warehouseCode} ·{' '}
            <span className={`badge badge-${invoice.status.toLowerCase()}`}>{invoice.status}</span>
          </p>
        </div>
      </header>

      <div className="panel scan-controls">
        <form onSubmit={submitManual} className="scan-input-row">
          <input
            ref={inputRef}
            autoFocus
            className="scan-input"
            placeholder="Scan or type SKU, then Enter"
            value={manualCode}
            onChange={(e) => setManualCode(e.target.value)}
          />
          <button className="btn btn-primary btn-lg">+1 Receive</button>
          <button type="button" className="btn btn-secondary btn-lg" onClick={() => setCameraOn(true)}>
            📷 Camera
          </button>
        </form>
        <ProgressBar received={invoice.totalReceived} expected={invoice.totalExpected} />
      </div>

      <div className="panel">
        <h3>Line Items</h3>
        <table className="table scan-table">
          <thead>
            <tr>
              <th>SKU</th><th>Item</th><th>Expected</th><th>Received</th>
              <th>Variance</th><th>Progress</th><th>Adjust</th>
            </tr>
          </thead>
          <tbody>
            {invoice.lines.map((l) => {
              const variance = l.expectedQuantity - l.receivedQuantity;
              const over = l.receivedQuantity > l.expectedQuantity;
              return (
                <tr key={l.id} className={over ? 'row-over' : l.receivedQuantity >= l.expectedQuantity ? 'row-done' : ''}>
                  <td className="mono">{l.sku}</td>
                  <td>{l.itemName}</td>
                  <td>{l.expectedQuantity}</td>
                  <td className="strong">{l.receivedQuantity}</td>
                  <td className={variance === 0 ? 'text-ok' : 'text-warn'}>{variance}</td>
                  <td style={{ minWidth: 140 }}>
                    <ProgressBar received={l.receivedQuantity} expected={l.expectedQuantity} />
                  </td>
                  <td className="nowrap">
                    <button className="btn btn-mini" onClick={() => doAdjust(l, -1)}>−</button>
                    <button className="btn btn-mini" onClick={() => doAdjust(l, 1)}>+</button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {cameraOn && (
        <BarcodeScanner
          onDetected={(code) => doScan(code)}
          onClose={() => setCameraOn(false)}
        />
      )}
    </div>
  );
}
