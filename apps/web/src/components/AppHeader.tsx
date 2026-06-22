import { Link } from 'react-router-dom';
import { getStoredUser } from '../api/auth';
import Logo from './Logo';
import './AppHeader.css';

type Props = {
  title?: string;
  onLogout: () => void;
};

export default function AppHeader({ title, onLogout }: Props) {
  const user = getStoredUser();
  return (
    <header className="app-header">
      <Link to="/" className="app-header-left">
        <Logo />
        <span>minecloud</span>
      </Link>
      {title && (
        <div className="app-header-center">
          <span className="app-header-title">{title}</span>
        </div>
      )}
      <div className="app-header-right">
        <span>{user?.nickname || user?.username}</span>
        <button className="app-header-btn-logout" onClick={onLogout}>退出</button>
      </div>
    </header>
  );
}
