import { useState } from 'react';
import { apiFetch } from '../api/client';
import { useLanguage } from '../i18n/LanguageContext';
import Banner from '../components/Banner';

// Cho phép BẤT KỲ role nào tự đổi mật khẩu của chính mình - gọi
// PATCH /api/auth/change-password (carve-out riêng trong SecurityConfig,
// khác với /api/users/** vốn chỉ ADMIN/DEPT_ADMIN gọi được).
export default function ChangePasswordPage() {
  const { t } = useLanguage();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (newPassword !== confirmPassword) {
      setError(t('changePassword.mismatch'));
      return;
    }

    setSubmitting(true);
    try {
      await apiFetch('/api/auth/change-password', {
        method: 'PATCH',
        body: { currentPassword, newPassword },
      });
      setSuccess(t('changePassword.success'));
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      setError(err.message || t('changePassword.failed'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page page-narrow">
      <h1>{t('changePassword.title')}</h1>
      <Banner message={error} />
      <Banner type="success" message={success} />
      <form className="card form" onSubmit={handleSubmit}>
        <label>
          {t('changePassword.current')}
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            required
          />
        </label>
        <label>
          {t('changePassword.new')}
          <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required />
          <small className="hint">{t('changePassword.hint')}</small>
        </label>
        <label>
          {t('changePassword.confirm')}
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
          />
        </label>
        <button type="submit" disabled={submitting}>
          {submitting ? t('changePassword.submitting') : t('changePassword.submit')}
        </button>
      </form>
    </div>
  );
}
