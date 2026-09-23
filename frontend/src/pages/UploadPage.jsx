import { useState } from 'react';
import { apiFetch } from '../api/client';
import Banner from '../components/Banner';

export default function UploadPage() {
  const [file, setFile] = useState(null);
  const [accessPolicy, setAccessPolicy] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [uploading, setUploading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!file) {
      setError('Vui lòng chọn file cần upload.');
      return;
    }
    if (!accessPolicy.trim()) {
      setError('Vui lòng nhập access policy.');
      return;
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('accessPolicy', accessPolicy);

    setUploading(true);
    try {
      const result = await apiFetch('/api/files/upload', {
        method: 'POST',
        body: formData,
      });
      setSuccess(`Upload thành công: ${result?.fileName || file.name}`);
      setFile(null);
      setAccessPolicy('');
      e.target.reset();
    } catch (err) {
      setError(err.message || 'Upload thất bại');
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="page page-narrow">
      <h1>Upload file</h1>
      <Banner message={error} />
      <Banner type="success" message={success} />
      <form className="card form" onSubmit={handleSubmit}>
        <label>
          Chọn file
          <input type="file" onChange={(e) => setFile(e.target.files[0] || null)} required />
        </label>
        <label>
          Access Policy
          <textarea
            rows={4}
            placeholder='Ví dụ: (department:CNTT AND position:giang_vien) OR role:ADMIN'
            value={accessPolicy}
            onChange={(e) => setAccessPolicy(e.target.value)}
            required
          />
          <small className="hint">
            Cú pháp: dùng <code>AND</code>, <code>OR</code> và dấu ngoặc <code>()</code> để kết hợp các
            attribute dạng <code>tên:giá_trị</code>. Ví dụ: <code>department:CNTT OR role:ADMIN</code>.
          </small>
        </label>
        <button type="submit" disabled={uploading}>
          {uploading ? 'Đang upload...' : 'Upload'}
        </button>
      </form>
    </div>
  );
}
