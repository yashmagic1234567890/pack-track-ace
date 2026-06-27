import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyWarehouses, listInvoicesByWarehouse } from '../api/endpoints';
import { errorMessage } from '../api/client';
import ProgressBar from '../components/ProgressBar';

/** Warehouse selection + invoice selection before entering the scan station. */
export default function Receiving() {
  const navigate = useNavigate();
  const [warehouses, setWarehouses] = useState([]);
  const [selected, setSelected] = useState('');
  const [invoices, setInvoices] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getMyWarehouses()
      .then((ws) => {
        setWarehouses(ws);
        if (ws.length === 1) setSelected(ws[0].code);
      })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  useEffect(() => {
    if (!selected) {
      setInvoices([]);
      return;
    }
    setLoading(true);
    listInvoicesByWarehouse(selected)
      .then(setInvoices)
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false));
  }, [selected]);

  return (
    <div>
      <header className="page-head">
        <h2>Receiving</h2>
        <p className="muted">Select a warehouse and invoice to begin scan-to-receive</p>
      </header>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="panel">
        <label>Warehouse</label>
        <select
          className="select-lg"
          value={selected}
          onChange={(e) => setSelected(e.target.value)}
        >
          <option value="">— Select warehouse —</option>
          {warehouses.map((w) => (
            <option key={w.code} value={w.code}>
              {w.name} ({w.code})
            </option>
          ))}
        </select>
      </div>

      {selected && (
        <div className="panel">
          <h3>Invoices for {selected}</h3>
          {loading ? (
            <p className="muted">Loading…</p>
          ) : (
            <div className="invoice-list">
              {invoices.map((i) => (
                <button
                  key={i.id}
                  className="invoice-tile"
                  onClick={() => navigate(`/receiving/${i.id}`)}
                  disabled={i.status === 'COMPLETED'}
                >
                  <div className="invoice-tile-head">
                    <strong>{i.invoiceBusinessId}</strong>
                    <span className={`badge badge-${i.status.toLowerCase()}`}>{i.status}</span>
                  </div>
                  <div className="muted">{i.vendorName}</div>
                  <ProgressBar received={i.totalReceived} expected={i.totalExpected} />
                </button>
              ))}
              {invoices.length === 0 && <p className="muted">No invoices for this warehouse.</p>}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
