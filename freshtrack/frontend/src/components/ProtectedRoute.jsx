import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/** Guards routes: requires authentication, and optionally CENTRAL_ADMIN role. */
export default function ProtectedRoute({ children, adminOnly = false }) {
  const { user, isAdmin } = useAuth();

  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (adminOnly && !isAdmin) {
    return <Navigate to="/dashboard" replace />;
  }
  return children;
}
