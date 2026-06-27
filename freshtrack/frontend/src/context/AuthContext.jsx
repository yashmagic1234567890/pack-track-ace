import { createContext, useContext, useEffect, useState } from 'react';
import { login as loginApi } from '../api/endpoints';

const AuthContext = createContext(null);

/** Provides authentication state (user + token) to the whole app. */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = localStorage.getItem('ft_user');
    if (stored) {
      try {
        setUser(JSON.parse(stored));
      } catch {
        localStorage.removeItem('ft_user');
      }
    }
    setLoading(false);
  }, []);

  const login = async (usernameOrEmail, password) => {
    const data = await loginApi(usernameOrEmail, password);
    const profile = {
      userId: data.userId,
      username: data.username,
      fullName: data.fullName,
      role: data.role,
      warehouses: data.warehouses || [],
    };
    localStorage.setItem('ft_token', data.token);
    localStorage.setItem('ft_user', JSON.stringify(profile));
    setUser(profile);
    return profile;
  };

  const logout = () => {
    localStorage.removeItem('ft_token');
    localStorage.removeItem('ft_user');
    setUser(null);
  };

  const isAdmin = user?.role === 'CENTRAL_ADMIN';

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, isAdmin }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
