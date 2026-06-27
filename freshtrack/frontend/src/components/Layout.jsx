import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/** App shell with role-aware navigation. Dark, low-light optimized theme. */
export default function Layout() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">❄</span>
          <div>
            <strong>FreshTrack</strong>
            <small>Inbound Receiving</small>
          </div>
        </div>

        <nav className="nav">
          <NavLink to="/dashboard">📊 Dashboard</NavLink>
          <NavLink to="/receiving">📦 Receiving</NavLink>
          <NavLink to="/reports">📑 Reconciliation</NavLink>
          {isAdmin && (
            <>
              <div className="nav-section">Admin</div>
              <NavLink to="/invoices">⬆ Invoice Upload</NavLink>
              <NavLink to="/warehouses">🏬 Warehouses</NavLink>
              <NavLink to="/users">👥 Users</NavLink>
              <NavLink to="/audit">🕵 Audit Trail</NavLink>
            </>
          )}
        </nav>

        <div className="sidebar-footer">
          <div className="user-chip">
            <div className="avatar">{(user?.fullName || user?.username || '?')[0]}</div>
            <div>
              <strong>{user?.fullName || user?.username}</strong>
              <small>{user?.role === 'CENTRAL_ADMIN' ? 'Central Admin' : 'Hub User'}</small>
            </div>
          </div>
          <button className="btn btn-ghost" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </aside>

      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
