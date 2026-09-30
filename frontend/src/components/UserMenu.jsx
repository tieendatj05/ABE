import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useLanguage } from '../i18n/LanguageContext';

// Gom username/role/đổi mật khẩu/đăng xuất (trước đây nằm thẳng hàng ngang,
// chiếm nhiều chỗ trên navbar) vào 1 avatar tròn + dropdown - đỡ rối khi navbar
// đã có nhiều link điều hướng theo role.
export default function UserMenu() {
  const { username, role, departmentName, logout } = useAuth();
  const { t } = useLanguage();
  const [open, setOpen] = useState(false);
  const containerRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(e) {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setOpen(false);
      }
    }
    function handleEscape(e) {
      if (e.key === 'Escape') setOpen(false);
    }
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, []);

  const initial = (username || '?').trim().charAt(0).toUpperCase();

  return (
    <div className="user-menu" ref={containerRef}>
      <button
        type="button"
        className="user-menu-avatar"
        onClick={() => setOpen((v) => !v)}
        aria-haspopup="true"
        aria-expanded={open}
        aria-label={username}
      >
        {initial}
      </button>

      {open && (
        <div className="user-menu-dropdown" role="menu">
          <div className="user-menu-header">
            <div className="user-menu-name">{username}</div>
            <div className="user-menu-badges">
              <span className="role-badge">{t(`role.${role}`)}</span>
              {departmentName && <span className="role-badge">{departmentName}</span>}
            </div>
          </div>
          <div className="user-menu-divider" />
          <Link to="/change-password" className="user-menu-item" onClick={() => setOpen(false)}>
            {t('nav.changePassword')}
          </Link>
          <button
            type="button"
            className="user-menu-item user-menu-item-danger"
            onClick={() => {
              setOpen(false);
              logout();
            }}
          >
            {t('nav.logout')}
          </button>
        </div>
      )}
    </div>
  );
}
