import { useState } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../api/auth';
import Logo from '../components/Logo';
import './AuthPage.css';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [sent, setSent] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    if (!email.trim()) {
      setError('请输入邮箱地址');
      return;
    }
    setLoading(true);
    try {
      await forgotPassword(email.trim());
      setSent(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : '发送失败');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-wrapper">
        <div className="auth-logo">
          <Logo />
          <h1>找回密码</h1>
        </div>
        <div className="auth-card">
          {sent ? (
            <div className="auth-success" style={{ marginBottom: 0 }}>
              如果该邮箱已注册，我们会发送密码重置链接。
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              {error && <div className="auth-error">{error}</div>}
              <p>输入注册时使用的邮箱地址，我们会向你发送密码重置链接。</p>
              <div className="form-group">
                <label htmlFor="email">邮箱</label>
                <input id="email" type="email" value={email}
                  onChange={(e) => setEmail(e.target.value)} autoComplete="email" autoFocus />
              </div>
              <button className="btn-primary" type="submit" disabled={loading}>
                {loading ? '发送中...' : '发送重置链接'}
              </button>
            </form>
          )}
        </div>
        <Link to="/login" className="auth-link">返回登录</Link>
      </div>
    </div>
  );
}
