import { useEffect, useState } from 'react';
import { apiFetch } from '../api/client';
import { useLanguage } from '../i18n/LanguageContext';
import Banner from '../components/Banner';
import { formatDate } from '../utils/format';

// Trang cho DATA_OWNER/DATA_USER tự xem attribute (thuộc tính) mình đang được
// KGC (ADMIN/DEPT_ADMIN) cấp - trước đây backend đã có sẵn API này
// (GET /api/attributes/me) nhưng chưa có màn hình nào gọi tới, nên user không
// có cách nào biết mình đang "có quyền gì" ngoài việc thử tải file rồi mới
// biết bị từ chối.
export default function MyAttributesPage() {
  const { t } = useLanguage();
  const [attributes, setAttributes] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    setError('');
    apiFetch('/api/attributes/me')
      .then((data) => setAttributes(data || []))
      .catch((err) => setError(err.message || t('myAttributes.loadFailed')))
      .finally(() => setLoading(false));
  }, [t]);

  const activeCount = attributes.filter((a) => !a.revoked).length;

  return (
    <div className="page">
      <h1>{t('myAttributes.title')}</h1>
      <p className="page-subtitle">{t('myAttributes.subtitle')}</p>
      <Banner message={error} />

      {loading ? (
        <p>{t('common.loading')}</p>
      ) : (
        <>
          <div className="stat-strip">
            <div className="stat-box">
              <span className="stat-number">{activeCount}</span>
              <span className="stat-label">{t('myAttributes.activeCount')}</span>
            </div>
            <div className="stat-box">
              <span className="stat-number">{attributes.length - activeCount}</span>
              <span className="stat-label">{t('myAttributes.revokedCount')}</span>
            </div>
          </div>

          <table className="table">
            <thead>
              <tr>
                <th>{t('myAttributes.colAttribute')}</th>
                <th>{t('myAttributes.colDate')}</th>
                <th>{t('myAttributes.colIssuer')}</th>
                <th>{t('myAttributes.colStatus')}</th>
              </tr>
            </thead>
            <tbody>
              {attributes.map((attr) => (
                <tr key={attr.id}>
                  <td className="cell-policy">{attr.attributeName}</td>
                  <td>{formatDate(attr.assignedAt)}</td>
                  <td>{attr.issuedByUsername || t('myAttributes.unknownIssuer')}</td>
                  <td>
                    <span className={attr.revoked ? 'tag tag-revoked' : 'tag tag-active'}>
                      {attr.revoked ? t('myAttributes.statusRevoked') : t('myAttributes.statusActive')}
                    </span>
                  </td>
                </tr>
              ))}
              {attributes.length === 0 && (
                <tr>
                  <td colSpan={4}>{t('myAttributes.empty')}</td>
                </tr>
              )}
            </tbody>
          </table>
        </>
      )}
    </div>
  );
}
