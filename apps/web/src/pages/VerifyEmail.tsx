import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { verifyEmail } from '../api/auth';
import Logo from '../components/Logo';
import './AuthPage.css';

export default function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setMessage('缺少验证令牌');
      return;
    }
    verifyEmail(token)
      .then(() => { setStatus('success'); setMessage('邮箱验证成功。'); })
      .catch((err) => { setStatus('error'); setMessage(err instanceof Error ? err.message : '验证失败'); });
  }, [token]);

  return (
    <div className="auth-page">
      <div className="auth-wrapper">
        <div className="auth-logo">
          <Logo />
          <h1>邮箱验证</h1>
        </div>
        <div className="auth-card">
          {status === 'loading' && <p style={{ margin: 0, textAlign: 'center' }}>验证中...</p>}
          {status === 'success' && <div className="auth-success" style={{ marginBottom: 0 }}>{message}</div>}
          {status === 'error' && <div className="auth-error" style={{ marginBottom: 0 }}>{message}</div>}
        </div>
        <Link to="/login" className="auth-link">返回登录</Link>
      </div>
    </div>
  );
}
