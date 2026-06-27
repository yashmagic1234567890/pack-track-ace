import { useEffect, useState } from 'react';
import { listUsers, createUser, listWarehouses, mapWarehouses, setUserStatus } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { useToast } from '../components/Toast';

/** Central Admin: user management and user-to-warehouse mapping. */
export default function Users() {
  const { push } = useToast();
  const [users, setUsers] = useState([]);
  const [warehouses, setWarehouses] = useState([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    username: '', email: '', fullName: '', password: '', role: 'HUB_USER', warehouseCodes: [],
  });

  const load = () => {
    listUsers().then(setUsers).catch((e) => setError(errorMessage(e)));
    listWarehouses().then(setWarehouses).catch(() => {});
  };
  useEffect(load, []);

  const submit = async (e) => {
    e.preventDefault();
    try {
      await createUser(form);
      push('User created', 'success');
      setForm({ username: '', email: '', fullName: '', password: '', role: 'HUB_USER', warehouseCodes: [] });
      load();
    } catch (err) {
      push(errorMessage(err, 'Create failed'), 'error');
    }
  };

  const remap = async (user, codes) => {
    try {
      await mapWarehouses(user.id, codes);
      push('Mapping updated', 'success');
      load();
    } catch (err) {
      push(errorMessage(err), 'error');
    }
  };

  const toggle = async (user) => {
    try {
      await setUserStatus(user.id, !user.enabled);
      load();
    } catch (err) {
      push(errorMessage(err), 'error');
    }
  };

  return (
    <div>
      <header className="page-head">
        <h2>Users</h2>
        <p className="muted">Manage users and warehouse assignments</p>
      </header>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="panel">
        <h3>Create User</h3>
        <form className="form-grid" onSubmit={submit}>
          <input placeholder="Username" value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value })} required />
          <input placeholder="Full name" value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
          <input placeholder="Email" type="email" value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })} required />
          <input placeholder="Password" type="password" value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })} required />
          <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
            <option value="HUB_USER">Hub User</option>
            <option value="CENTRAL_ADMIN">Central Admin</option>
          </select>
          <select multiple value={form.warehouseCodes}
            onChange={(e) =>
              setForm({ ...form, warehouseCodes: Array.from(e.target.selectedOptions, (o) => o.value) })
            }>
            {warehouses.map((w) => (
              <option key={w.code} value={w.code}>{w.name} ({w.code})</option>
            ))}
          </select>
          <button className="btn btn-primary">Create</button>
        </form>
      </div>

      <div className="panel">
        <h3>All Users ({users.length})</h3>
        <table className="table">
          <thead>
            <tr><th>User</th><th>Role</th><th>Warehouses</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td><strong>{u.fullName}</strong><br /><small className="muted">{u.username} · {u.email}</small></td>
                <td>{u.role === 'CENTRAL_ADMIN' ? 'Central Admin' : 'Hub User'}</td>
                <td style={{ minWidth: 180 }}>
                  {u.role === 'CENTRAL_ADMIN' ? (
                    <span className="muted">All (global)</span>
                  ) : (
                    <select multiple value={u.warehouseCodes || []}
                      onChange={(e) =>
                        remap(u, Array.from(e.target.selectedOptions, (o) => o.value))
                      }>
                      {warehouses.map((w) => (
                        <option key={w.code} value={w.code}>{w.code}</option>
                      ))}
                    </select>
                  )}
                </td>
                <td><span className={`badge ${u.enabled ? 'badge-completed' : 'badge-pending'}`}>
                  {u.enabled ? 'Active' : 'Disabled'}</span></td>
                <td><button className="btn btn-mini" onClick={() => toggle(u)}>
                  {u.enabled ? 'Disable' : 'Enable'}</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
