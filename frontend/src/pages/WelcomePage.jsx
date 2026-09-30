import { Link } from 'react-router-dom';
import LanguageToggle from '../components/LanguageToggle';
import AuthBackgroundBlobs from '../components/AuthBackgroundBlobs';
import { useLanguage } from '../i18n/LanguageContext';

// Trang chào (landing) trước khi vào đăng nhập/đăng ký - giới thiệu ngắn về
// đồ án ABE thay vì đưa thẳng người dùng vào form đăng nhập trống trơn.
export default function WelcomePage() {
  const { t } = useLanguage();

  return (
    <div className="auth-page">
      <AuthBackgroundBlobs />
      <div className="auth-content welcome-hero">
        <LanguageToggle />
        <div className="welcome-brand">{t('welcome.title')}</div>
        <h1 className="welcome-title auth-title">{t('welcome.tagline')}</h1>
        <div className="welcome-actions">
          <Link to="/login" className="contact-btn-primary">
            {t('welcome.login')}
          </Link>
          <Link to="/register" className="contact-btn-secondary">
            {t('welcome.register')}
          </Link>
        </div>
      </div>
    </div>
  );
}
