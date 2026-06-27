import { useEffect, useState } from 'react';
import { listAllInvoices, uploadInvoices } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { useToast } from '../components/Toast';
import ProgressBar from '../components/ProgressBar';

/** Central Admin: CSV/Excel invoice ingestion and master invoice list. */
export default function Invoices() {
  const { push } = useToast();
  const [invoices, setInvoices] = useState([]);
  const [file, setFile] = useState(null);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  const load = () =>
    listAllInvoices().then(setInvoices).catch((e) => setError(errorMessage(e)));

  useEffect(() => {
    load();
  }, []);

  const submit = async (e) => {
    e.preventDefault();
    if (!file) return;
    setBusy(true);
    setResult(null);
    setError('');
    try {
      const res = await uploadInvoices(file);
      setResult(res);
      push(`Imported ${res.invoicesCreated} invoice(s), ${res.linesCreated} line(s)`, 'success');
      setFile(null);
      e.target.reset();
      load();
    } catch (err) {
      setError(errorMessage(err, 'Upload failed'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <header className="page-head">
        <h2>Invoice Upload</h2>
        <p className="muted">Import inbound invoices via CSV or Excel (.xlsx)</p>
      </header>

      <div className="panel">
        <form onSubmit={submit} className="upload-form">
          <input
            type="file"
            accept=".csv,.xlsx,.xls"
            onChange={(e) => setFile(e.target.files[0])}
          />
          <button className="btn btn-primary" disabled={!file || busy}>
            {busy ? 'Uploading…' : 'Upload'}
          </button>
        </form>
        <p className="muted small">
          Required columns: Invoice_ID, Vendor_Name, Target_Warehouse_ID, Item_SKU, Item_Name,
          Expected_Quantity
        </p>
        {error && <div className="alert alert-error">{error}</div>}
        {result && (
          <div className="alert alert-info">
            <strong>Import summary:</strong> {result.invoicesCreated} invoice(s),{' '}
            {result.linesCreated} line(s), {result.rowsRejected} rejected of {result.rowsProcessed}{' '}
            row(s).
            {result.errors?.length > 0 && (
              <ul className="error-list">
                {result.errors.map((er, i) => (
                  <li key={i}>{er}</li>
                ))}
              </ul>
            )}
          </div>
        )}
      </div>

      <div className="panel">
        <h3>All Invoices ({invoices.length})</h3>
        <table className="table">
          <thead>
            <tr>
              <th>Invoice ID</th><th>Vendor</th><th>Warehouse</th>
              <th>Status</th><th>Lines</th><th>Progress</th>
            </tr>
          </thead>
          <tbody>
            {invoices.map((i) => (
              <tr key={i.id}>
                <td>{i.invoiceBusinessId}</td>
                <td>{i.vendorName}</td>
                <td>{i.warehouseCode}</td>
                <td><span className={`badge badge-${i.status.toLowerCase()}`}>{i.status}</span></td>
                <td>{i.totalLines}</td>
                <td style={{ minWidth: 180 }}>
                  <ProgressBar received={i.totalReceived} expected={i.totalExpected} />
                </td>
              </tr>
            ))}
            {invoices.length === 0 && (
              <tr><td colSpan="6" className="muted">No invoices uploaded yet</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
