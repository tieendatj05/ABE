import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useLanguage } from '../i18n/LanguageContext';
import Banner from '../components/Banner';
import LanguageToggle from '../components/LanguageToggle';

// Icon inline (không phụ thuộc thư viện icon ngoài) - flat, 1 màu, kế thừa
// currentColor để tự đổi màu theo context (label vs input).
function IconUser(props) {
  return (
    <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" {...props}>
      <circle cx="10" cy="6.5" r="3.2" />
      <path d="M3.5 17c1.2-3.4 4-5 6.5-5s5.3 1.6 6.5 5" strokeLinecap="round" />
    </svg>
  );
}

function IconLock(props) {
  return (
    <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" {...props}>
      <rect x="4" y="8.5" width="12" height="8" rx="1.8" />
      <path d="M6.5 8.5V6a3.5 3.5 0 0 1 7 0v2.5" strokeLinecap="round" />
    </svg>
  );
}

function IconEye({ off, ...props }) {
  return (
    <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" {...props}>
      <path d="M1.5 10S4.5 4.5 10 4.5 18.5 10 18.5 10 15.5 15.5 10 15.5 1.5 10 1.5 10Z" strokeLinecap="round" />
      <circle cx="10" cy="10" r="2.4" />
      {off && <path d="M2.5 2.5 17.5 17.5" strokeLinecap="round" />}
    </svg>
  );
}

function IconShieldCap(props) {
  return (
    <svg viewBox="0 0 40 40" fill="none" {...props}>
      <path d="M20 3 34 8v10c0 9-6 15.5-14 19C12 33.5 6 27 6 18V8Z" fill="#0b2f6b" />
      <path
        d="M20 3 34 8v10c0 9-6 15.5-14 19"
        fill="none"
        stroke="#dc2626"
        strokeWidth="1.4"
        opacity="0.55"
      />
      <path d="M20 15 27 18.4 20 21.8 13 18.4Z" fill="#fff" />
      <path d="M14.5 19.3V23l5.5 2.4 5.5-2.4v-3.7" fill="none" stroke="#fff" strokeWidth="1.3" />
      <path d="M27 18.4v3.4" stroke="#fff" strokeWidth="1.3" strokeLinecap="round" />
    </svg>
  );
}

// Sơ đồ mạng nút khoá kết nối nhau - ẩn dụ trực quan cho cơ chế ABE (khoá chỉ
// ghép lại được khi đủ "nút thuộc tính" trong cây chính sách), đặt ở banner trái.
function AttributeNetworkGraphic() {
  return (
    <svg viewBox="0 0 260 200" fill="none" className="login-network-graphic" aria-hidden="true">
      <g stroke="rgba(255,255,255,0.35)" strokeWidth="1.4">
        <line x1="40" y1="40" x2="110" y2="90" />
        <line x1="110" y1="90" x2="70" y2="150" />
        <line x1="110" y1="90" x2="190" y2="60" />
        <line x1="190" y1="60" x2="220" y2="130" />
        <line x1="110" y1="90" x2="190" y2="150" />
      </g>
      {[
        [40, 40],
        [110, 90],
        [70, 150],
        [190, 60],
        [220, 130],
        [190, 150],
      ].map(([cx, cy], i) => (
        <g key={i}>
          <circle cx={cx} cy={cy} r="14" fill="rgba(255,255,255,0.12)" />
          <circle cx={cx} cy={cy} r="14" stroke="rgba(255,255,255,0.5)" strokeWidth="1.2" />
          <rect x={cx - 4} y={cy - 2.5} width="8" height="6" rx="1.2" fill="#fff" />
          <path d={`M${cx - 2.4} ${cy - 2.5}v-2a2.4 2.4 0 1 1 4.8 0v2`} stroke="#fff" strokeWidth="1.2" fill="none" />
        </g>
      ))}
    </svg>
  );
}

export default function LoginPage() {
  const { login, completeTwoFactorSetup, verifyTwoFactor } = useAuth();
  const { t } = useLanguage();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [code, setCode] = useState('');
  // null = màn hình username/password bình thường; khác null = đang ở bước 2 (2FA)
  // cho ADMIN/DEPT_ADMIN - xem AuthService.login.
  const [challenge, setChallenge] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const from = location.state?.from?.pathname || '/';

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const response = await login(username, password);
      if (response.twoFactorChallenge) {
        setChallenge({
          type: response.twoFactorChallenge,
          ticket: response.twoFactorTicket,
          qrCodeDataUri: response.qrCodeDataUri,
          manualEntryKey: response.manualEntryKey,
        });
      } else {
        navigate(from, { replace: true });
      }
    } catch (err) {
      setError(err.message || t('login.failed'));
    } finally {
      setLoading(false);
    }
  }

  async function handleTwoFactorSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (challenge.type === 'SETUP_REQUIRED') {
        await completeTwoFactorSetup(challenge.ticket, code);
      } else {
        await verifyTwoFactor(challenge.ticket, code);
      }
      navigate(from, { replace: true });
    } catch (err) {
      setError(err.message || t('twoFactor.failed'));
    } finally {
      setLoading(false);
    }
  }

  function backToLogin() {
    setChallenge(null);
    setCode('');
    setError('');
  }

  return (
    <div className="login-split">
      <div className="login-split-left">
        <AttributeNetworkGraphic />
        <div className="login-split-left-content">
          <img src="/hcmute-logo.png" alt="Logo HCMUTE" className="login-hcmute-logo" />
          <div className="login-eyebrow">HỆ THỐNG CHIA SẺ TÀI LIỆU GIÁO DỤC</div>
          <h1 className="login-headline">Mã hoá dựa trên thuộc tính (ABE)</h1>
          <p className="login-subheadline">Bảo mật tài liệu giáo dục · Kết nối tri thức</p>
        </div>
      </div>

      <div className="login-split-right">
        <div className="login-split-right-top">
          <LanguageToggle />
        </div>

        <div className="login-form-wrap">
          <div className="login-brand">
            <IconShieldCap className="login-brand-icon" />
            <div>
              <div className="login-brand-name">MyUTE Vault</div>
              <div className="login-brand-tagline">Nền tảng chia sẻ tài liệu an toàn</div>
            </div>
          </div>

          <h2 className="login-form-title">
            {!challenge
              ? t('login.title')
              : challenge.type === 'SETUP_REQUIRED'
                ? t('twoFactor.titleSetup')
                : t('twoFactor.titleVerify')}
          </h2>
          <Banner message={error} />

          {!challenge ? (
            <form className="login-card" onSubmit={handleSubmit}>
              <label className="login-field">
                {t('login.username')}
                <div className="login-input-group">
                  <IconUser className="login-input-icon" />
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    required
                    autoFocus
                  />
                </div>
              </label>
              <label className="login-field">
                {t('login.password')}
                <div className="login-input-group">
                  <IconLock className="login-input-icon" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                  />
                  <button
                    type="button"
                    className="login-input-action"
                    onClick={() => setShowPassword((v) => !v)}
                    aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    <IconEye off={showPassword} />
                  </button>
                </div>
              </label>
              <button type="submit" className="login-submit-btn" disabled={loading}>
                {loading ? t('login.submitting') : t('login.submit')}
              </button>
            </form>
          ) : (
            <form className="login-card" onSubmit={handleTwoFactorSubmit}>
              {challenge.type === 'SETUP_REQUIRED' ? (
                <>
                  <p className="hint">{t('twoFactor.setupInstructions')}</p>
                  <img src={challenge.qrCodeDataUri} alt="QR code 2FA" className="twofactor-qr" />
                  <p className="hint">
                    {t('twoFactor.manualEntryLabel')}
                    <br />
                    <code>{challenge.manualEntryKey}</code>
                  </p>
                </>
              ) : (
                <p className="hint">{t('twoFactor.verifyInstructions')}</p>
              )}
              <label className="login-field">
                {t('twoFactor.codeLabel')}
                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                  required
                  autoFocus
                />
              </label>
              <button type="submit" className="login-submit-btn" disabled={loading || code.length !== 6}>
                {loading ? t('twoFactor.submitting') : t('twoFactor.submit')}
              </button>
              <button type="button" className="login-secondary-btn" onClick={backToLogin}>
                {t('twoFactor.backToLogin')}
              </button>
            </form>
          )}

          <p className="login-footer-link">
            {t('login.noAccount')} <Link to="/register">{t('login.registerNow')}</Link>
          </p>
        </div>

        <p className="login-powered-by">Powered by ABE Cryptography</p>
      </div>
    </div>
  );
}
