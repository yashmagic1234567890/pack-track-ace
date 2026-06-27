import { useEffect, useState } from 'react';
import { getStats } from '../api/endpoints';
import { errorMessage } from '../api/client';
import ProgressBar from '../components/ProgressBar';

/** Role-scoped KPI dashboard and analytics. */
export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    getStats().then(setStats).catch((e) => setError(errorMessage(e)));
  }, []);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (!stats) return <div className="muted">Loading dashboard…</div>;

  const cards = [
    { label: 'Total Invoices', value: stats.totalInvoices, accent: 'blue' },
    { label: 'Pending', value: stats.pendingInvoices, accent: 'amber' },
    { label: 'In Progress', value: stats.inProgressInvoices, accent: 'purple' },
    { label: 'Completed', value: stats.completedInvoices, accent: 'green' },
    { label: 'Warehouses', value: stats.totalWarehouses, accent: 'blue' },
    { label: 'Hub Users', value: stats.totalHubUsers, accent: 'purple' },
  ];

  return (
    <div>
      <header className="page-head">
        <h2>Dashboard</h2>
        <p className="muted">Operational overview and analytics</p>
      </header>

      <div className="kpi-grid">
        {cards.map((c) => (
          <div key={c.label} className={`kpi-card accent-${c.accent}`}>
            <span className="kpi-value">{c.value}</span>
            <span className="kpi-label">{c.label}</span>
          </div>
        ))}
      </div>

      <div className="panel">
        <h3>Overall Receiving Progress</h3>
        <ProgressBar
          received={stats.totalReceivedUnits}
          expected={stats.totalExpectedUnits}
          percent={stats.overallProgressPercent}
        />
        <div className="stat-row">
          <span>Expected units: <strong>{stats.totalExpectedUnits}</strong></span>
          <span>Received units: <strong>{stats.totalReceivedUnits}</strong></span>
          <span>Variance: <strong>{stats.totalVariance}</strong></span>
        </div>
      </div>

      <div className="grid-2">
        <div className="panel">
          <h3>By Warehouse</h3>
          <table className="table">
            <thead>
              <tr><th>Warehouse</th><th>Expected</th><th>Received</th><th>Variance</th></tr>
            </thead>
            <tbody>
              {stats.perWarehouse.map((w) => (
                <tr key={w.warehouseCode}>
                  <td>{w.name} <small className="muted">({w.warehouseCode})</small></td>
                  <td>{w.expected}</td>
                  <td>{w.received}</td>
                  <td className={w.variance > 0 ? 'text-warn' : 'text-ok'}>{w.variance}</td>
                </tr>
              ))}
              {stats.perWarehouse.length === 0 && (
                <tr><td colSpan="4" className="muted">No data</td></tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="panel">
          <h3>Top Vendors</h3>
          <table className="table">
            <thead>
              <tr><th>Vendor</th><th>Expected</th><th>Received</th><th>Variance</th></tr>
            </thead>
            <tbody>
              {stats.topVendors.map((v) => (
                <tr key={v.vendorName}>
                  <td>{v.vendorName}</td>
                  <td>{v.expected}</td>
                  <td>{v.received}</td>
                  <td className={v.variance > 0 ? 'text-warn' : 'text-ok'}>{v.variance}</td>
                </tr>
              ))}
              {stats.topVendors.length === 0 && (
                <tr><td colSpan="4" className="muted">No data</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
