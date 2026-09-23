import { useCallback, useEffect, useState } from 'react';
import { apiDownload, apiFetch } from '../api/client';
import Banner from '../components/Banner';
import { formatBytes, formatDate } from '../utils/format';

export default function FilesPage() {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const loadFiles = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await apiFetch('/api/files');
      setFiles(data || []);
    } catch (err) {
      setError(err.message || 'Không tải được danh sách file');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadFiles();
  }, [loadFiles]);

  async function handleDownload(file) {
    setError('');
    setBusyId(file.id);
    try {
      await apiDownload(`/api/files/${file.id}/download`, file.fileName);
    } catch (err) {
      setError(err.message || 'Bạn không đủ thuộc tính để giải mã file này');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="page">
      <h1>Danh sách file</h1>
      <Banner message={error} />
      {loading ? (
        <p>Đang tải...</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Tên file</th>
              <th>Người upload</th>
              <th>Access Policy</th>
              <th>Kích thước</th>
              <th>Ngày tạo</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {files.map((file) => (
              <tr key={file.id}>
                <td>{file.fileName}</td>
                <td>{file.ownerUsername}</td>
                <td className="cell-policy">{file.accessPolicy}</td>
                <td>{formatBytes(file.fileSize)}</td>
                <td>{formatDate(file.createdAt)}</td>
                <td className="cell-actions">
                  <button disabled={busyId === file.id} onClick={() => handleDownload(file)}>
                    {busyId === file.id ? 'Đang tải...' : 'Tải xuống'}
                  </button>
                </td>
              </tr>
            ))}
            {files.length === 0 && (
              <tr>
                <td colSpan={6}>Chưa có file nào trong hệ thống.</td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  );
}
