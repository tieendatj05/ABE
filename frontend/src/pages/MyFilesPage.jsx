import { useCallback, useEffect, useMemo, useState } from 'react';
import { apiDownload, apiFetch } from '../api/client';
import { useLanguage } from '../i18n/LanguageContext';
import Banner from '../components/Banner';
import PolicyTreeDiagram from '../components/PolicyTreeDiagram';
import Pagination from '../components/Pagination';
import { formatBytes, formatDate } from '../utils/format';

const PAGE_SIZE = 10;

export default function MyFilesPage() {
  const { t } = useLanguage();
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);
  const [expandedIds, setExpandedIds] = useState(() => new Set());
  const [page, setPage] = useState(1);

  function toggleExpanded(fileId) {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(fileId)) {
        next.delete(fileId);
      } else {
        next.add(fileId);
      }
      return next;
    });
  }

  const loadFiles = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await apiFetch('/api/files/mine');
      setFiles(data || []);
      setPage(1);
    } catch (err) {
      setError(err.message || t('myFiles.loadFailed'));
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    loadFiles();
  }, [loadFiles]);

  async function handleDownload(file) {
    setError('');
    setBusyId(file.id);
    try {
      await apiDownload(`/api/files/${file.id}/download`, file.fileName);
    } catch (err) {
      setError(err.message || t('myFiles.downloadFailed'));
    } finally {
      setBusyId(null);
    }
  }

  const totalPages = Math.max(1, Math.ceil(files.length / PAGE_SIZE));
  const pagedFiles = useMemo(() => files.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE), [files, page]);

  async function handleDelete(file) {
    setError('');
    setBusyId(file.id);
    try {
      await apiFetch(`/api/files/${file.id}`, { method: 'DELETE' });
      await loadFiles();
    } catch (err) {
      setError(err.message || t('myFiles.deleteFailed'));
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="page">
      <h1>{t('myFiles.title')}</h1>
      <Banner message={error} />
      {loading ? (
        <p>{t('common.loading')}</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>{t('myFiles.colName')}</th>
              <th>{t('myFiles.colPolicy')}</th>
              <th>{t('myFiles.colSize')}</th>
              <th>{t('myFiles.colDate')}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {pagedFiles.map((file) => (
              <tr key={file.id}>
                <td>{file.fileName}</td>
                <td className="cell-policy">
                  {file.accessPolicy}
                  <button type="button" className="ptree-toggle" onClick={() => toggleExpanded(file.id)}>
                    {expandedIds.has(file.id) ? t('policyTree.toggleHide') : t('policyTree.toggleShow')}
                  </button>
                  {expandedIds.has(file.id) && <PolicyTreeDiagram policy={file.accessPolicy} />}
                </td>
                <td>{formatBytes(file.fileSize)}</td>
                <td>{formatDate(file.createdAt)}</td>
                <td className="cell-actions">
                  <button disabled={busyId === file.id} onClick={() => handleDownload(file)}>
                    {t('myFiles.download')}
                  </button>
                  <button
                    className="btn-danger"
                    disabled={busyId === file.id}
                    onClick={() => handleDelete(file)}
                  >
                    {t('myFiles.delete')}
                  </button>
                </td>
              </tr>
            ))}
            {files.length === 0 && (
              <tr>
                <td colSpan={5}>{t('myFiles.empty')}</td>
              </tr>
            )}
          </tbody>
        </table>
      )}
      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </div>
  );
}
