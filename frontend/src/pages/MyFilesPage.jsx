import { useCallback, useEffect, useState } from 'react';
import { apiDownload, apiFetch } from '../api/client';
import Banner from '../components/Banner';
import { formatBytes, formatDate } from '../utils/format';

export default function MyFilesPage() {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const loadFiles = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await apiFetch('/api/files/mine');
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
      setError(err.message || 'Tải file thất bại');
    } finally {
      setBusyId(null);
    }
  }

  async function handleDelete(file) {
    setError('');
    setBusyId(file.id);
    try {
      await apiFetch(`/api/files/${file.id}`, { method: 'DELETE' });
      await loadFiles();
    } catch (err) {
      setError(err.message || 'Xoá file thất bại');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="page">
      <h1>File của tôi</h1>
      <Banner message={error} />
      {loading ? (
        <p>Đang tải...</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Tên file</th>
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
                <td className="cell-policy">{file.accessPolicy}</td>
                <td>{formatBytes(file.fileSize)}</td>
                <td>{formatDate(file.createdAt)}</td>
                <td className="cell-actions">
                  <button disabled={busyId === file.id} onClick={() => handleDownload(file)}>
                    Tải xuống
                  </button>
                  <button
                    className="btn-danger"
                    disabled={busyId === file.id}
                    onClick={() => handleDelete(file)}
                  >
                    Xoá
                  </button>
                </td>
              </tr>
            ))}
            {files.length === 0 && (
              <tr>
                <td colSpan={5}>Bạn chưa upload file nào.</td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  );
}
