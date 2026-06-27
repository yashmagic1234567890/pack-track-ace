import { useEffect, useState } from 'react';
import { listWarehouses, createWarehouse } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { useToast } from '../components/Toast';

/** Central Admin: warehouse (hub) management. */
export default function Warehouses() {
  const { push } = useToast();
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ code: '', name: '', location: '' });

  const load = () => listWarehouses().then(setItems).catch((e) => setError(errorMessage(e)));
  useEffect(load, []);

  const submit = async (e) => {
    e.preventDefault();
    try {
      await createWarehouse(form);
      push('Warehouse created', 'success');
      setForm({ code: '', name: '', location: '' });
      load();
    } catch (err) {
      push(errorMessage(err, 'Create failed'), 'error');
    }
  };

  return (
    <div>
      <header className="page-head">
        <h2>Warehouses</h2>
        <p className="muted">Manage receiving hubs</p>
      </header>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="panel">
        <h3>Create Warehouse</h3>
        <form className="form-grid" onSubmit={submit}>
          <input placeholder="Code (e.g. WH-DEL)" value={form.code}
            onChange={(e) => setForm({ ...form, code: e.target.value })} required />
          <input placeholder="Name" value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          <input placeholder="Location" value={form.location}
            onChange={(e) => setForm({ ...form, location: e.target.value })} />
          <button className="btn btn-primary">Create</button>
        </form>
      </div>

      <div className="panel">
        <h3>All Warehouses ({items.length})</h3>
        <table className="table">
          <thead><tr><th>Code</th><th>Name</th><th>Location</th></tr></thead>
          <tbody>
            {items.map((w) => (
              <tr key={w.code}>
                <td className="mono">{w.code}</td>
                <td>{w.name}</td>
                <td>{w.location || <span className="muted">—</span>}</td>
              </tr>
            ))}
            {items.length === 0 && <tr><td colSpan="3" className="muted">No warehouses</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
