import { useNavigate } from 'react-router-dom';
import { getStoredUser, logout } from '../api/auth';
import Logo from '../components/Logo';
import './Home.css';

function RepoIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="currentColor">
      <path d="M2 2.5A2.5 2.5 0 014.5 0h8.75a.75.75 0 01.75.75v12.5a.75.75 0 01-.75.75h-2.5a.75.75 0 01-.75-.75v-2.5h-1v2.5a.75.75 0 01-.75.75h-2.5a.75.75 0 01-.75-.75v-3.5H4.5A2.5 2.5 0 012 6.75v-4.25z" />
    </svg>
  );
}

export default function Home() {
  const navigate = useNavigate();
  const user = getStoredUser();

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="home">
      <header className="home-header">
        <div className="home-header-left">
          <Logo />
          <span>minecloud</span>
        </div>
        <div className="home-header-right">
          <span>{user?.nickname || user?.username}</span>
          <button className="btn-logout" onClick={handleLogout}>退出</button>
        </div>
      </header>
      <main className="home-body">
        <div className="home-empty">
          <RepoIcon />
          <h2>欢迎，{user?.nickname || user?.username}</h2>
          <p>文件管理功能即将上线</p>
        </div>
      </main>
    </div>
  );
}
