import { useEffect, useState } from 'react';
import { listAudit } from '../api/endpoints';
import { errorMessage } from '../api/client';

/** Central Admin: append-only audit trail of scans and manual actions. */
export default function AuditTrail() {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const [action, setAction] = useState('');

  const load = () => {
    const params = {};
    if (action) params.action = action;
    listAudit(params).then(setRows).catch((e) => setError(errorMessage(e)));
  };
  useEffect(load, [action]);

  return (
    <div>
      <header className="page-head">
        <h2>Audit Trail</h2>
        <p className="muted">Every scan and manual action, in order</p>
      </header>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="panel filter-bar">
        <select value={action} onChange={(e) => setAction(e.target.value)}>
          <option value="">All actions</option>
          <option value="SCAN">Scan</option>
          <option value="MANUAL_ADJUST">Manual Adjust</option>
          <option value="INVOICE_UPLOAD">Invoice Upload</option>
          <option value="LOGIN">Login</option>
        </select>
      </div>

      <div className="panel">
        <table className="table">
          <thead>
            <tr><th>Time</th><th>User</th><th>Action</th><th>Entity</th><th>Details</th></tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id}>
                <td className="nowrap mono small">{new Date(r.createdAt).toLocaleString()}</td>
                <td>{r.username}</td>
                <td><span className="badge badge-inprogress">{r.action}</span></td>
                <td>{r.entityType} #{r.entityId}</td>
                <td className="small">{r.details}</td>
              </tr>
            ))}
            {rows.length === 0 && <tr><td colSpan="5" className="muted">No audit entries</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
