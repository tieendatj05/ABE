import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { apiFetch } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import Banner from '../components/Banner';
import MorphingBlobBackground from '../components/MorphingBlobBackground';

const initialForm = {
  username: '',
  password: '',
  email: '',
  fullName: '',
  role: 'DATA_USER',
  departmentId: '',
};

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [departments, setDepartments] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // GET /api/departments là permitAll (chưa cần đăng nhập) - phục vụ đúng
    // màn hình đăng ký này.
    apiFetch('/api/departments')
      .then((data) => setDepartments(data || []))
      .catch(() => setDepartments([]));
  }, []);

  function updateField(field) {
    return (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await register({
        ...form,
        departmentId: form.departmentId ? Number(form.departmentId) : null,
      });
      navigate('/', { replace: true });
    } catch (err) {
      setError(err.message || 'Đăng ký thất bại');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <MorphingBlobBackground />
      <div className="auth-content">
        <h1 className="auth-title">Đăng ký tài khoản</h1>
        <Banner message={error} />
        <form className="card form glass-card" onSubmit={handleSubmit}>
          <label>
            Tên đăng nhập
            <input type="text" value={form.username} onChange={updateField('username')} required />
          </label>
          <label>
            Mật khẩu
            <input type="password" value={form.password} onChange={updateField('password')} required />
          </label>
          <label>
            Email
            <input type="email" value={form.email} onChange={updateField('email')} required />
          </label>
          <label>
            Họ tên
            <input type="text" value={form.fullName} onChange={updateField('fullName')} required />
          </label>
          <label>
            Vai trò
            <select value={form.role} onChange={updateField('role')}>
              <option value="DATA_USER">Data User (tải &amp; giải mã file)</option>
              <option value="DATA_OWNER">Data Owner (upload &amp; chia sẻ file)</option>
            </select>
          </label>
          <label>
            Khoa / Phòng ban (tuỳ chọn)
            <select value={form.departmentId} onChange={updateField('departmentId')}>
              <option value="">-- Không chọn --</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          </label>
          <button type="submit" className="btn-neon" disabled={loading}>
            {loading ? 'Đang đăng ký...' : 'Đăng ký'}
          </button>
        </form>
        <p className="auth-footer-link">
          Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
        </p>
      </div>
    </div>
  );
}
