import { useEffect, useMemo, useState } from 'react';
import { apiFetch } from '../api/client';
import { useLanguage } from '../i18n/LanguageContext';
import Banner from '../components/Banner';

// Bar ngang 1 hue duy nhất - đây là "1 phép đo (số lượng) chia theo 1 chiều
// phân loại" (nominal categorical), không phải nhiều series riêng biệt, nên
// theo đúng nguyên tắc dataviz KHÔNG tô mỗi thanh 1 màu khác nhau (rainbow bar
// chart) - identity của mỗi thanh đã được thể hiện bằng label, không cần màu.
function BarChart({ data }) {
  const max = Math.max(1, ...data.map((d) => d.value));
  return (
    <div className="bar-chart">
      {data.map((d) => (
        <div className="bar-row" key={d.label} tabIndex={0} title={`${d.label}: ${d.value}`}>
          <span className="bar-row-label">{d.label}</span>
          <div className="bar-track">
            <div className="bar-fill" style={{ width: `${(d.value / max) * 100}%` }} />
          </div>
          <span className="bar-row-value">{d.value}</span>
        </div>
      ))}
    </div>
  );
}

export default function DashboardPage() {
  const { t } = useLanguage();
  const [users, setUsers] = useState(null);
  const [files, setFiles] = useState(null);
  const [departments, setDepartments] = useState(null);
  const [auditLogs, setAuditLogs] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    setError('');
    Promise.all([
      apiFetch('/api/users'),
      apiFetch('/api/files'),
      apiFetch('/api/departments'),
      apiFetch('/api/audit-logs'),
    ])
      .then(([u, f, d, a]) => {
        setUsers(u || []);
        setFiles(f || []);
        setDepartments(d || []);
        setAuditLogs(a || []);
      })
      .catch((err) => setError(err.message || t('dashboard.loadFailed')))
      .finally(() => setLoading(false));
  }, [t]);

  const filesByDept = useMemo(() => {
    if (!users || !files) return [];
    const deptByUsername = new Map(users.map((u) => [u.username, u.departmentName]));
    const counts = new Map();
    files.forEach((f) => {
      const dept = deptByUsername.get(f.ownerUsername) || t('dashboard.noDepartment');
      counts.set(dept, (counts.get(dept) || 0) + 1);
    });
    return [...counts.entries()]
      .map(([label, value]) => ({ label, value }))
      .sort((a, b) => b.value - a.value);
  }, [users, files, t]);

  const actionCounts = useMemo(() => {
    if (!auditLogs) return null;
    const counts = {
      UPLOAD: 0,
      DOWNLOAD_SUCCESS: 0,
      DOWNLOAD_DENIED: 0,
      DELETE: 0,
      INTEGRITY_VIOLATION: 0,
    };
    auditLogs.forEach((log) => {
      if (log.action in counts) counts[log.action]++;
    });
    return counts;
  }, [auditLogs]);

  const actionChartData = useMemo(() => {
    if (!actionCounts) return [];
    return [
      { label: t('auditLog.actionUpload'), value: actionCounts.UPLOAD },
      { label: t('auditLog.actionDownloadSuccess'), value: actionCounts.DOWNLOAD_SUCCESS },
      { label: t('auditLog.actionDownloadDenied'), value: actionCounts.DOWNLOAD_DENIED },
      { label: t('auditLog.actionDelete'), value: actionCounts.DELETE },
      { label: t('auditLog.actionIntegrityViolation'), value: actionCounts.INTEGRITY_VIOLATION },
    ];
  }, [actionCounts, t]);

  const downloadRatio = useMemo(() => {
    if (!actionCounts) return null;
    const total = actionCounts.DOWNLOAD_SUCCESS + actionCounts.DOWNLOAD_DENIED;
    if (total === 0) return null;
    return {
      success: actionCounts.DOWNLOAD_SUCCESS,
      denied: actionCounts.DOWNLOAD_DENIED,
      successPct: Math.round((actionCounts.DOWNLOAD_SUCCESS / total) * 100),
      deniedPct: Math.round((actionCounts.DOWNLOAD_DENIED / total) * 100),
    };
  }, [actionCounts]);

  if (loading) {
    return (
      <div className="page">
        <p>{t('common.loading')}</p>
      </div>
    );
  }

  return (
    <div className="page">
      <h1>{t('dashboard.title')}</h1>
      <Banner message={error} />

      <div className="stat-strip">
        <div className="stat-box">
          <span className="stat-number">{users?.length ?? 0}</span>
          <span className="stat-label">{t('dashboard.statUsers')}</span>
        </div>
        <div className="stat-box">
          <span className="stat-number">{files?.length ?? 0}</span>
          <span className="stat-label">{t('dashboard.statFiles')}</span>
        </div>
        <div className="stat-box">
          <span className="stat-number">{departments?.length ?? 0}</span>
          <span className="stat-label">{t('dashboard.statDepartments')}</span>
        </div>
        <div className="stat-box">
          <span className="stat-number">{auditLogs?.length ?? 0}</span>
          <span className="stat-label">{t('dashboard.statEvents')}</span>
        </div>
      </div>

      <section className="card">
        <h2>{t('dashboard.filesByDeptTitle')}</h2>
        {filesByDept.length === 0 ? <p>{t('dashboard.noData')}</p> : <BarChart data={filesByDept} />}
      </section>

      <section className="card">
        <h2>{t('dashboard.actionsTitle')}</h2>
        {!actionCounts || auditLogs.length === 0 ? (
          <p>{t('dashboard.noData')}</p>
        ) : (
          <BarChart data={actionChartData} />
        )}
      </section>

      <section className="card">
        <h2>{t('dashboard.downloadRatioTitle')}</h2>
        {!downloadRatio ? (
          <p>{t('dashboard.noData')}</p>
        ) : (
          <div className="status-pair">
            <div className="status-tile status-good">
              <span className="status-icon" aria-hidden="true">
                ✓
              </span>
              <div>
                <div className="status-value">
                  {downloadRatio.success} ({downloadRatio.successPct}%)
                </div>
                <div className="status-label">{t('dashboard.downloadSuccess')}</div>
              </div>
            </div>
            <div className="status-tile status-critical">
              <span className="status-icon" aria-hidden="true">
                ✕
              </span>
              <div>
                <div className="status-value">
                  {downloadRatio.denied} ({downloadRatio.deniedPct}%)
                </div>
                <div className="status-label">{t('dashboard.downloadDenied')}</div>
              </div>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
