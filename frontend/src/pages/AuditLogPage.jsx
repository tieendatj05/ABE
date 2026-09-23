import { useEffect, useState } from 'react';
import { apiFetch } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import Banner from '../components/Banner';
import { formatDate } from '../utils/format';

const ACTION_LABELS = {
  UPLOAD: 'Upload',
  DOWNLOAD_SUCCESS: 'Tải xuống thành công',
  DOWNLOAD_DENIED: 'Bị từ chối tải xuống',
  DELETE: 'Xoá file',
};

function actionTagClass(action) {
  if (action === 'DOWNLOAD_DENIED') return 'tag tag-revoked';
  if (action === 'UPLOAD' || action === 'DOWNLOAD_SUCCESS') return 'tag tag-active';
  return 'tag';
}

// ADMIN xem toàn bộ nhật ký truy cập file trong hệ thống; các role khác (chủ
// yếu Data Owner) chỉ xem lịch sử truy cập của những file MÌNH SỞ HỮU - phục
// vụ yêu cầu compliance của hệ thống y tế: phải biết ai đã/cố truy cập hồ sơ.
export default function AuditLogPage() {
  const { role } = useAuth();
  const [logs, setLogs] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const path = role === 'ADMIN' ? '/api/audit-logs' : '/api/audit-logs/mine';
    setLoading(true);
    setError('');
    apiFetch(path)
      .then((data) => setLogs(data || []))
      .catch((err) => setError(err.message || 'Không tải được nhật ký truy cập'))
      .finally(() => setLoading(false));
  }, [role]);

  if (loading) {
    return (
      <div className="page">
        <p>Đang tải...</p>
      </div>
    );
  }

  return (
    <div className="page">
      <h1>Nhật ký truy cập file</h1>
      <p>
        {role === 'ADMIN'
          ? 'Toàn bộ sự kiện upload/tải xuống/xoá file trong hệ thống.'
          : 'Lịch sử truy cập (kể cả bị từ chối) đối với các file bạn sở hữu.'}
      </p>
      <Banner message={error} />

      <table className="table">
        <thead>
          <tr>
            <th>Thời gian</th>
            <th>Hành động</th>
            <th>File</th>
            <th>Chủ sở hữu</th>
            <th>Người yêu cầu</th>
            <th>Access Policy</th>
            <th>Chi tiết</th>
          </tr>
        </thead>
        <tbody>
          {logs.map((log) => (
            <tr key={log.id}>
              <td>{formatDate(log.occurredAt)}</td>
              <td>
                <span className={actionTagClass(log.action)}>{ACTION_LABELS[log.action] || log.action}</span>
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
              <td colSpan={7}>Chưa có sự kiện nào.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
