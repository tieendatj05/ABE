import { useEffect, useState } from 'react';
import { apiFetch } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useLanguage } from '../i18n/LanguageContext';
import Banner from '../components/Banner';
import { formatDate } from '../utils/format';

function actionTagClass(action) {
  if (action === 'INTEGRITY_VIOLATION') return 'tag tag-critical';
  if (action === 'DOWNLOAD_DENIED') return 'tag tag-revoked';
  if (action === 'UPLOAD' || action === 'DOWNLOAD_SUCCESS') return 'tag tag-active';
  return 'tag';
}

// ADMIN xem toàn bộ nhật ký truy cập file trong hệ thống; các role khác (chủ
// yếu Data Owner) chỉ xem lịch sử truy cập của những file MÌNH SỞ HỮU - phục
// vụ yêu cầu compliance của hệ thống giáo dục: phải biết ai đã/cố truy cập tài liệu.
export default function AuditLogPage() {
  const { role } = useAuth();
  const { t } = useLanguage();
  const [logs, setLogs] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const actionLabels = {
    UPLOAD: t('auditLog.actionUpload'),
    DOWNLOAD_SUCCESS: t('auditLog.actionDownloadSuccess'),
    DOWNLOAD_DENIED: t('auditLog.actionDownloadDenied'),
    DELETE: t('auditLog.actionDelete'),
    INTEGRITY_VIOLATION: t('auditLog.actionIntegrityViolation'),
  };

  useEffect(() => {
    const path = role === 'ADMIN' ? '/api/audit-logs' : '/api/audit-logs/mine';
    setLoading(true);
    setError('');
    apiFetch(path)
      .then((data) => setLogs(data || []))
      .catch((err) => setError(err.message || t('auditLog.loadFailed')))
      .finally(() => setLoading(false));
  }, [role, t]);

  if (loading) {
    return (
      <div className="page">
        <p>{t('common.loading')}</p>
      </div>
    );
  }

  return (
    <div className="page">
      <h1>{t('auditLog.title')}</h1>
      <p>{role === 'ADMIN' ? t('auditLog.subtitleAdmin') : t('auditLog.subtitleOwner')}</p>
      <Banner message={error} />

      <table className="table">
        <thead>
          <tr>
            <th>{t('auditLog.colTime')}</th>
            <th>{t('auditLog.colAction')}</th>
            <th>{t('auditLog.colFile')}</th>
            <th>{t('auditLog.colOwner')}</th>
            <th>{t('auditLog.colRequester')}</th>
            <th>{t('auditLog.colPolicy')}</th>
            <th>{t('auditLog.colDetail')}</th>
          </tr>
        </thead>
        <tbody>
          {logs.map((log) => (
            <tr key={log.id}>
              <td>{formatDate(log.occurredAt)}</td>
              <td>
                <span className={actionTagClass(log.action)}>{actionLabels[log.action] || log.action}</span>
              </td>
              <td>{log.fileName}</td>
              <td>{log.ownerUsername}</td>
              <td>{log.requesterUsername}</td>
              <td className="cell-policy">{log.accessPolicy}</td>
              <td>{log.detail}</td>
            </tr>
          ))}
          {logs.length === 0 && (
            <tr>
              <td colSpan={7}>{t('auditLog.empty')}</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
