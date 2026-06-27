import { useEffect, useState } from 'react';
import { getReconciliation, exportReport, getMyWarehouses } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { useToast } from '../components/Toast';

/** Reconciliation report viewer with CSV/Excel export. */
export default function Reports() {
  const { push } = useToast();
  const [warehouses, setWarehouses] = useState([]);
  const [filters, setFilters] = useState({ warehouseCode: '', status: '', onlyVariance: false });
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getMyWarehouses().then(setWarehouses).catch(() => {});
  }, []);

  const buildParams = () => {
    const p = {};
    if (filters.warehouseCode) p.warehouseCode = filters.warehouseCode;
    if (filters.status) p.status = filters.status;
    if (filters.onlyVariance) p.onlyVariance = true;
    return p;
  };

  const load = () => {
    setLoading(true);
    getReconciliation(buildParams())
      .then(setRows)
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters]);

  const download = async (format) => {
    try {
      const res = await exportReport(format, buildParams());
      const blob = new Blob([res.data]);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `reconciliation.${format === 'excel' ? 'xlsx' : 'csv'}`;
      a.click();
      URL.revokeObjectURL(url);
      push('Export downloaded', 'success');
    } catch (e) {
      push(errorMessage(e, 'Export failed'), 'error');
    }
  };

  return (
    <div>
      <header className="page-head">
        <h2>Reconciliation Report</h2>
        <p className="muted">Expected vs Received variance across invoices</p>
      </header>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="panel filter-bar">
        <select
          value={filters.warehouseCode}
          onChange={(e) => setFilters((f) => ({ ...f, warehouseCode: e.target.value }))}
        >
          <option value="">All warehouses</option>
          {warehouses.map((w) => (
            <option key={w.code} value={w.code}>{w.name} ({w.code})</option>
          ))}
        </select>
        <select
          value={filters.status}
          onChange={(e) => setFilters((f) => ({ ...f, status: e.target.value }))}
        >
          <option value="">All statuses</option>
          <option value="PENDING">Pending</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="COMPLETED">Completed</option>
        </select>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={filters.onlyVariance}
            onChange={(e) => setFilters((f) => ({ ...f, onlyVariance: e.target.checked }))}
          />
          Only rows with variance
        </label>
        <div className="spacer" />
        <button className="btn btn-secondary" onClick={() => download('csv')}>⬇ CSV</button>
        <button className="btn btn-secondary" onClick={() => download('excel')}>⬇ Excel</button>
      </div>

      <div className="panel">
        <table className="table">
          <thead>
            <tr>
              <th>Invoice</th><th>Vendor</th><th>Warehouse</th><th>SKU</th><th>Item</th>
              <th>Expected</th><th>Received</th><th>Variance</th><th>Status</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="9" className="muted">Loading…</td></tr>
            ) : rows.length === 0 ? (
              <tr><td colSpan="9" className="muted">No data</td></tr>
            ) : (
              rows.map((r, i) => (
                <tr key={i} className={r.variance !== 0 ? 'row-warn' : ''}>
                  <td>{r.invoiceBusinessId}</td>
                  <td>{r.vendorName}</td>
                  <td>{r.warehouseCode}</td>
                  <td className="mono">{r.sku}</td>
                  <td>{r.itemName}</td>
                  <td>{r.expectedQuantity}</td>
                  <td>{r.receivedQuantity}</td>
                  <td className={r.variance === 0 ? 'text-ok' : 'text-warn'}>{r.variance}</td>
                  <td><span className={`badge badge-${r.status.toLowerCase()}`}>{r.status}</span></td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
