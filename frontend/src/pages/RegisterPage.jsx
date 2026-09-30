import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { apiFetch } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useLanguage } from '../i18n/LanguageContext';
import Banner from '../components/Banner';
import LanguageToggle from '../components/LanguageToggle';
import AuthBackgroundBlobs from '../components/AuthBackgroundBlobs';

const initialForm = {
  username: '',
  password: '',
  email: '',
  fullName: '',
  role: 'DATA_USER',
  departmentId: '',
};

export default function RegisterPage() {
  const { register } = useAuth();
  const { t } = useLanguage();
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [departments, setDepartments] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // GET /api/departments là permitAll (chưa cần đăng nhập) - phục vụ đúng
    // màn hình đăng ký này.
    apiFetch('/api/departments')
      .then((data) => setDepartments(data || []))
      .catch(() => setDepartments([]));
  }, []);

  function updateField(field) {
    return (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await register({
        ...form,
        departmentId: form.departmentId ? Number(form.departmentId) : null,
      });
      navigate('/', { replace: true });
    } catch (err) {
      setError(err.message || t('register.failed'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <AuthBackgroundBlobs />
      <div className="auth-content">
        <LanguageToggle />
        <Link to="/welcome" className="auth-brand-link">
          {t('login.backToWelcome')}
        </Link>
        <h1 className="auth-title">{t('register.title')}</h1>
        <Banner message={error} />
        <form className="card form glass-card" onSubmit={handleSubmit}>
          <label>
            {t('register.username')}
            <input type="text" value={form.username} onChange={updateField('username')} required />
          </label>
          <label>
            {t('register.password')}
            <input type="password" value={form.password} onChange={updateField('password')} required />
            <small className="hint">{t('register.passwordHint')}</small>
          </label>
          <label>
            {t('register.email')}
            <input type="email" value={form.email} onChange={updateField('email')} required />
          </label>
          <label>
            {t('register.fullName')}
            <input type="text" value={form.fullName} onChange={updateField('fullName')} required />
          </label>
          <label>
            {t('register.department')}
            <select value={form.departmentId} onChange={updateField('departmentId')}>
              <option value="">{t('register.noDepartment')}</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          </label>
          <button type="submit" className="btn-neon" disabled={loading}>
            {loading ? t('register.submitting') : t('register.submit')}
          </button>
        </form>
        <p className="auth-footer-link">
          {t('register.haveAccount')} <Link to="/login">{t('register.loginNow')}</Link>
        </p>
      </div>
    </div>
  );
}
