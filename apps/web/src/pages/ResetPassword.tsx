import { useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { resetPassword } from '../api/auth';
import Logo from '../components/Logo';
import './AuthPage.css';

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [np, setNp] = useState('');
  const [np2, setNp2] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    if (!np) { setError('请输入新密码'); return; }
    if (np !== np2) { setError('两次密码不一致'); return; }
    if (np.length < 8) { setError('密码至少8位，需包含大小写字母和数字'); return; }
    setLoading(true);
    try {
      await resetPassword(token!, np);
      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : '重置失败');
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
            <h1>重置密码</h1>
          </div>

          <div className="auth-card">
            {!token ? (
              <div className="auth-error" style={{ marginBottom: 0 }}>无效的重置链接。</div>
            ) : success ? (
              <div className="auth-success" style={{ marginBottom: 0 }}>密码已更新。</div>
            ) : (
              <form onSubmit={handleSubmit}>
                {error && <div className="auth-error">{error}</div>}
                <div className="form-group">
                  <label htmlFor="rp1">新密码</label>
                  <input id="rp1" type="password" value={np}
                    onChange={(e) => setNp(e.target.value)}
                    placeholder="至少8位，包含大小写字母和数字"
                    autoComplete="new-password" autoFocus />
                </div>
                <div className="form-group">
                  <label htmlFor="rp2">确认新密码</label>
                  <input id="rp2" type="password" value={np2}
                    onChange={(e) => setNp2(e.target.value)} autoComplete="new-password" />
                </div>
                <button className="btn-primary" type="submit" disabled={loading}>
                  {loading ? '更新中...' : '更新密码'}
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
