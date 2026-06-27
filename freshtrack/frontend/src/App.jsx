import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Invoices from './pages/Invoices';
import Receiving from './pages/Receiving';
import ScanStation from './pages/ScanStation';
import Reports from './pages/Reports';
import Users from './pages/Users';
import Warehouses from './pages/Warehouses';
import AuditTrail from './pages/AuditTrail';

export default function App() {
  const { loading } = useAuth();
  if (loading) {
    return <div className="full-center">Loading…</div>;
  }

  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/receiving" element={<Receiving />} />
        <Route path="/receiving/:invoiceId" element={<ScanStation />} />
        <Route path="/reports" element={<Reports />} />

        {/* Central Admin only */}
        <Route
          path="/invoices"
          element={
            <ProtectedRoute adminOnly>
              <Invoices />
            </ProtectedRoute>
          }
        />
        <Route
          path="/users"
          element={
            <ProtectedRoute adminOnly>
              <Users />
            </ProtectedRoute>
          }
        />
        <Route
          path="/warehouses"
          element={
            <ProtectedRoute adminOnly>
              <Warehouses />
            </ProtectedRoute>
          }
        />
        <Route
          path="/audit"
          element={
            <ProtectedRoute adminOnly>
              <AuditTrail />
            </ProtectedRoute>
          }
        />
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
