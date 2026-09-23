import { useCallback, useEffect, useState } from 'react';
import { apiFetch } from '../api/client';
import Banner from '../components/Banner';
import { formatDate } from '../utils/format';

// Trang chỉ ADMIN toàn cục truy cập: tạo/xoá phòng ban, và phong 1 user hiện
// có thành DEPT_ADMIN của 1 phòng ban - nền tảng cho mô hình ABE phi tập
// trung hoá (mỗi phòng ban tự quản lý attribute/user của mình).
export default function DepartmentsPage() {
  const [departments, setDepartments] = useState([]);
  const [users, setUsers] = useState([]);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(true);

  const [newName, setNewName] = useState('');
  const [newCode, setNewCode] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [creating, setCreating] = useState(false);

  const [promoteUserId, setPromoteUserId] = useState('');
  const [promoteDeptId, setPromoteDeptId] = useState('');
  const [promoting, setPromoting] = useState(false);

  const loadDepartments = useCallback(async () => {
    const data = await apiFetch('/api/departments');
    setDepartments(data || []);
  }, []);

  const loadUsers = useCallback(async () => {
    const data = await apiFetch('/api/users');
    setUsers(data || []);
  }, []);

  useEffect(() => {
    setLoading(true);
    setError('');
    Promise.all([loadDepartments(), loadUsers()])
      .catch((err) => setError(err.message || 'Không tải được dữ liệu'))
      .finally(() => setLoading(false));
  }, [loadDepartments, loadUsers]);

  async function handleCreate(e) {
    e.preventDefault();
    setError('');
    setSuccess('');
    setCreating(true);
    try {
      await apiFetch('/api/departments', {
        method: 'POST',
        body: { name: newName, code: newCode, description: newDesc },
      });
      setNewName('');
      setNewCode('');
      setNewDesc('');
      await loadDepartments();
    } catch (err) {
      setError(err.message || 'Tạo phòng ban thất bại');
    } finally {
      setCreating(false);
    }
  }

  async function handleDelete(id) {
    setError('');
    setSuccess('');
    try {
      await apiFetch(`/api/departments/${id}`, { method: 'DELETE' });
      await loadDepartments();
    } catch (err) {
      setError(err.message || 'Xoá phòng ban thất bại');
    }
  }

  async function handlePromote(e) {
    e.preventDefault();
    if (!promoteUserId || !promoteDeptId) return;
    setError('');
    setSuccess('');
    setPromoting(true);
    try {
      const promoted = await apiFetch('/api/users/promote-dept-admin', {
        method: 'POST',
        body: { userId: Number(promoteUserId), departmentId: Number(promoteDeptId) },
      });
      setSuccess(`Đã phong "${promoted.username}" làm DEPT_ADMIN của "${promoted.departmentName}"`);
      setPromoteUserId('');
      setPromoteDeptId('');
      await loadUsers();
    } catch (err) {
      setError(err.message || 'Phong DEPT_ADMIN thất bại');
    } finally {
      setPromoting(false);
    }
  }

  if (loading) {
    return (
      <div className="page">
        <p>Đang tải...</p>
      </div>
    );
  }

  return (
    <div className="page">
      <h1>Quản lý phòng ban</h1>
      <Banner message={error} />
      <Banner type="success" message={success} />

      <section className="card">
        <h2>Danh sách khoa / phòng ban</h2>
        <form className="inline-form" onSubmit={handleCreate}>
          <input
            type="text"
            placeholder="Tên, ví dụ Khoa Nội"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            required
          />
          <input
            type="text"
            placeholder="Mã, ví dụ NOI"
            value={newCode}
            onChange={(e) => setNewCode(e.target.value)}
            required
          />
          <input
            type="text"
            placeholder="Mô tả"
            value={newDesc}
            onChange={(e) => setNewDesc(e.target.value)}
          />
          <button type="submit" disabled={creating}>
            {creating ? 'Đang tạo...' : 'Tạo phòng ban'}
          </button>
        </form>

        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tên</th>
              <th>Mã</th>
              <th>Mô tả</th>
              <th>Ngày tạo</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {departments.map((d) => (
              <tr key={d.id}>
                <td>{d.id}</td>
                <td>{d.name}</td>
                <td>{d.code}</td>
                <td>{d.description}</td>
                <td>{formatDate(d.createdAt)}</td>
                <td>
                  <button className="btn-danger" onClick={() => handleDelete(d.id)}>
                    Xoá
                  </button>
                </td>
              </tr>
            ))}
            {departments.length === 0 && (
              <tr>
                <td colSpan={6}>Chưa có phòng ban nào.</td>
              </tr>
            )}
          </tbody>
        </table>
      </section>

      <section className="card">
        <h2>Phong DEPT_ADMIN (KGC phòng ban)</h2>
        <p>
          Chọn 1 user hiện có và 1 phòng ban - user đó sẽ trở thành DEPT_ADMIN của phòng ban này,
          được tự quản lý attribute và user trong phạm vi phòng ban đó (mô hình ABE phi tập trung hoá).
        </p>
        <form className="inline-form" onSubmit={handlePromote}>
          <select value={promoteUserId} onChange={(e) => setPromoteUserId(e.target.value)} required>
            <option value="">-- Chọn user --</option>
            {users.map((u) => (
              <option key={u.id} value={u.id}>
                {u.username} ({u.role}{u.departmentName ? `, ${u.departmentName}` : ''})
              </option>
            ))}
          </select>
          <select value={promoteDeptId} onChange={(e) => setPromoteDeptId(e.target.value)} required>
            <option value="">-- Chọn phòng ban --</option>
            {departments.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
          <button type="submit" disabled={promoting}>
            {promoting ? 'Đang phong...' : 'Phong DEPT_ADMIN'}
          </button>
        </form>
      </section>
    </div>
  );
}
