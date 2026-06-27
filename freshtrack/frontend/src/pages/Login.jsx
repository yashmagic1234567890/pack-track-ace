import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../api/client';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [usernameOrEmail, setU] = useState('');
  const [password, setP] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await login(usernameOrEmail, password);
      navigate('/dashboard');
    } catch (err) {
      setError(errorMessage(err, 'Login failed'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={submit}>
        <div className="login-brand">
          <span className="brand-mark big">❄</span>
          <h1>FreshTrack</h1>
          <p className="muted">Inbound Fruit &amp; Vegetable Receiving</p>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        <label>Username or Email</label>
        <input
          autoFocus
          value={usernameOrEmail}
          onChange={(e) => setU(e.target.value)}
          placeholder="admin"
        />

        <label>Password</label>
        <input
          type="password"
          value={password}
          onChange={(e) => setP(e.target.value)}
          placeholder="••••••••"
        />

        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? 'Signing in…' : 'Sign In'}
        </button>

        <div className="login-hint">
          <span>Demo: admin / admin123</span>
          <span>hubdel / hub123</span>
        </div>
      </form>
    </div>
  );
}
