import { useLanguage } from '../i18n/LanguageContext';

// Nút bật/tắt ngôn ngữ Anh/Việt - đặt ở NavBar (trang đã đăng nhập) và trên
// các trang công khai (Welcome/Login/Register) vì những trang đó không có NavBar.
export default function LanguageToggle({ className = '' }) {
  const { lang, toggleLang, t } = useLanguage();

  return (
    <button
      type="button"
      className={`lang-toggle ${className}`.trim()}
      onClick={toggleLang}
      title={lang === 'vi' ? t('langToggle.switchToEn') : t('langToggle.switchToVi')}
    >
      {lang === 'vi' ? 'VI' : 'EN'}
    </button>
  );
}
