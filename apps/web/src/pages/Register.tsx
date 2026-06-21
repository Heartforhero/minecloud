import { useState } from 'react';
import { Link } from 'react-router-dom';
import { register } from '../api/auth';
import Logo from '../components/Logo';
import './AuthPage.css';

export default function Register() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [password2, setPassword2] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    if (!username.trim() || !email.trim() || !password) {
      setError('请填写所有必填项');
      return;
    }
    if (password !== password2) {
      setError('两次密码不一致');
      return;
    }
    if (password.length < 8) {
      setError('密码至少8位，需包含大小写字母和数字');
      return;
    }
    setLoading(true);
    try {
      await register(username.trim(), password, email.trim());
      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : '注册失败');
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
            <h1>加入 Minecloud</h1>
          </div>

          <div className="auth-card">
            {success ? (
              <div className="auth-success" style={{ marginBottom: 0 }}>
                注册成功。验证邮件已发送，请查收邮箱完成验证。
              </div>
            ) : (
              <form onSubmit={handleSubmit}>
                {error && <div className="auth-error">{error}</div>}

                <div className="form-group">
                  <label htmlFor="rf1">用户名</label>
                  <input id="rf1" type="text" value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    autoCapitalize="off" autoComplete="username" autoFocus />
                </div>

                <div className="form-group">
                  <label htmlFor="rf2">邮箱</label>
                  <input id="rf2" type="email" value={email}
                    onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
                </div>

                <div className="form-group">
                  <label htmlFor="rf3">密码</label>
                  <input id="rf3" type="password" value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="至少8位，包含大小写字母和数字" autoComplete="new-password" />
                </div>

                <div className="form-group">
                  <label htmlFor="rf4">确认密码</label>
                  <input id="rf4" type="password" value={password2}
                    onChange={(e) => setPassword2(e.target.value)} autoComplete="new-password" />
                </div>

                <button className="btn-primary" type="submit" disabled={loading}>
                  {loading ? '注册中...' : '注册'}
                </button>
              </form>
            )}
          </div>

          <div className="auth-switch">
            已有账号? <Link to="/login">登录</Link>
          </div>
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
