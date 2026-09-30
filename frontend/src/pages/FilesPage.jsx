import { useCallback, useEffect, useMemo, useState } from 'react';
import { apiDownload, apiFetch } from '../api/client';
import { useLanguage } from '../i18n/LanguageContext';
import Banner from '../components/Banner';
import PolicyTreeDiagram from '../components/PolicyTreeDiagram';
import Pagination from '../components/Pagination';
import { formatBytes, formatDate } from '../utils/format';
import { evaluatePolicy } from '../utils/policyEval';

const PAGE_SIZE = 10;

export default function FilesPage() {
  const { t } = useLanguage();
  const [files, setFiles] = useState([]);
  const [myAttributes, setMyAttributes] = useState(null);
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
      const [filesData, attrData] = await Promise.all([
        apiFetch('/api/files'),
        // /api/attributes/me không bao giờ 403 (mọi role đã đăng nhập đều gọi
        // được) nhưng vẫn bọc catch riêng để 1 lỗi phụ không chặn cả trang.
        apiFetch('/api/attributes/me').catch(() => null),
      ]);
      setFiles(filesData || []);
      setMyAttributes(attrData);
      setPage(1);
    } catch (err) {
      setError(err.message || t('files.loadFailed'));
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    loadFiles();
  }, [loadFiles]);

  const ownedAttributeNames = useMemo(() => {
    if (!myAttributes) return null;
    return new Set(myAttributes.filter((a) => !a.revoked).map((a) => a.attributeName));
  }, [myAttributes]);

  const eligibleCount = useMemo(() => {
    if (!ownedAttributeNames) return null;
    return files.filter((f) => evaluatePolicy(f.accessPolicy, ownedAttributeNames) === true).length;
  }, [files, ownedAttributeNames]);

  const totalPages = Math.max(1, Math.ceil(files.length / PAGE_SIZE));
  const pagedFiles = useMemo(
    () => files.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE),
    [files, page]
  );

  async function handleDownload(file) {
    setError('');
    setBusyId(file.id);
    try {
      await apiDownload(`/api/files/${file.id}/download`, file.fileName);
    } catch (err) {
      setError(err.message || t('files.downloadFailed'));
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="page">
      <h1>{t('files.title')}</h1>
      <Banner message={error} />
      {loading ? (
        <p>{t('common.loading')}</p>
      ) : (
        <>
          {eligibleCount !== null && (
            <p className="page-subtitle">
              {t('files.eligibleSummary', { eligible: eligibleCount, total: files.length })}
            </p>
          )}
          <table className="table">
            <thead>
              <tr>
                <th>{t('files.colName')}</th>
                <th>{t('files.colOwner')}</th>
                <th>{t('files.colPolicy')}</th>
                <th>{t('files.colSize')}</th>
                <th>{t('files.colDate')}</th>
                <th>{t('files.colAccess')}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {pagedFiles.map((file) => {
                const canAccess = ownedAttributeNames
                  ? evaluatePolicy(file.accessPolicy, ownedAttributeNames)
                  : null;
                return (
                  <tr key={file.id} className={canAccess === true ? 'row-accessible' : canAccess === false ? 'row-restricted' : ''}>
                    <td>{file.fileName}</td>
                    <td>{file.ownerUsername}</td>
                    <td className="cell-policy">
                      {file.accessPolicy}
                      <button
                        type="button"
                        className="ptree-toggle"
                        onClick={() => toggleExpanded(file.id)}
                      >
                        {expandedIds.has(file.id) ? t('policyTree.toggleHide') : t('policyTree.toggleShow')}
                      </button>
                      {expandedIds.has(file.id) && <PolicyTreeDiagram policy={file.accessPolicy} />}
                    </td>
                    <td>{formatBytes(file.fileSize)}</td>
                    <td>{formatDate(file.createdAt)}</td>
                    <td>
                      {canAccess === true && <span className="tag tag-active">{t('files.accessGranted')}</span>}
                      {canAccess === false && <span className="tag tag-revoked">{t('files.accessDenied')}</span>}
                      {canAccess === null && <span className="tag">{t('files.accessUnknown')}</span>}
                    </td>
                    <td className="cell-actions">
                      <button disabled={busyId === file.id} onClick={() => handleDownload(file)}>
                        {busyId === file.id ? t('files.downloading') : t('files.download')}
                      </button>
                    </td>
                  </tr>
                );
              })}
              {files.length === 0 && (
                <tr>
                  <td colSpan={7}>{t('files.empty')}</td>
                </tr>
              )}
            </tbody>
          </table>
          <Pagination page={page} totalPages={totalPages} onChange={setPage} />
        </>
      )}
    </div>
  );
}
