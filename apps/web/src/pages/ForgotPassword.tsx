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
    if (!email.trim()) { setError('请输入邮箱'); return; }
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
        <div className="auth-wrapper-inner">
          <div className="auth-header">
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
                <div className="form-group">
                  <label htmlFor="fe">邮箱</label>
                  <input id="fe" type="email" value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    autoComplete="email" autoFocus />
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

      <div className="auth-footer">
        <ul>
          <li><a href="#">条款</a></li>
          <li><a href="#">隐私</a></li>
          <li><a href="#">文档</a></li>
          <li><a href="#">联系</a></li>
        </ul>
      </div>
    </div>
  );
}
