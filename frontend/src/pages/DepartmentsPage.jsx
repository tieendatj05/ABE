import { useCallback, useEffect, useState } from 'react';
import { apiFetch } from '../api/client';
import { useLanguage } from '../i18n/LanguageContext';
import Banner from '../components/Banner';
import { formatDate } from '../utils/format';

// Trang chỉ ADMIN toàn cục truy cập: tạo/xoá phòng ban, và phong 1 user hiện
// có thành DEPT_ADMIN của 1 phòng ban - nền tảng cho mô hình ABE phi tập
// trung hoá (mỗi phòng ban tự quản lý attribute/user của mình).
export default function DepartmentsPage() {
  const { t } = useLanguage();
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
      .catch((err) => setError(err.message || t('departments.loadFailed')))
      .finally(() => setLoading(false));
  }, [loadDepartments, loadUsers, t]);

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
      setError(err.message || t('departments.createFailed'));
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
      setError(err.message || t('departments.deleteFailed'));
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
      setSuccess(t('departments.promoteSuccess', { username: promoted.username, department: promoted.departmentName }));
      setPromoteUserId('');
      setPromoteDeptId('');
      await loadUsers();
    } catch (err) {
      setError(err.message || t('departments.promoteFailed'));
    } finally {
      setPromoting(false);
    }
  }

  if (loading) {
    return (
      <div className="page">
        <p>{t('common.loading')}</p>
      </div>
    );
  }

  return (
    <div className="page">
      <h1>{t('departments.title')}</h1>
      <Banner message={error} />
      <Banner type="success" message={success} />

      <section className="card">
        <h2>{t('departments.listTitle')}</h2>
        <form className="inline-form" onSubmit={handleCreate}>
          <input
            type="text"
            placeholder={t('departments.namePlaceholder')}
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            required
          />
          <input
            type="text"
            placeholder={t('departments.codePlaceholder')}
            value={newCode}
            onChange={(e) => setNewCode(e.target.value)}
            required
          />
          <input
            type="text"
            placeholder={t('departments.descPlaceholder')}
            value={newDesc}
            onChange={(e) => setNewDesc(e.target.value)}
          />
          <button type="submit" disabled={creating}>
            {creating ? t('departments.creating') : t('departments.create')}
          </button>
        </form>

        <table className="table">
          <thead>
            <tr>
              <th>{t('departments.colId')}</th>
              <th>{t('departments.colName')}</th>
              <th>{t('departments.colCode')}</th>
              <th>{t('departments.colDesc')}</th>
              <th>{t('departments.colDate')}</th>
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
                    {t('common.delete')}
                  </button>
                </td>
              </tr>
            ))}
            {departments.length === 0 && (
              <tr>
                <td colSpan={6}>{t('departments.empty')}</td>
              </tr>
            )}
          </tbody>
        </table>
      </section>

      <section className="card">
        <h2>{t('departments.promoteTitle')}</h2>
        <p>{t('departments.promoteDesc')}</p>
        <form className="inline-form" onSubmit={handlePromote}>
          <select value={promoteUserId} onChange={(e) => setPromoteUserId(e.target.value)} required>
            <option value="">{t('departments.chooseUserOption')}</option>
            {users.map((u) => (
              <option key={u.id} value={u.id}>
                {u.username} ({t(`role.${u.role}`)}{u.departmentName ? `, ${u.departmentName}` : ''})
              </option>
            ))}
          </select>
          <select value={promoteDeptId} onChange={(e) => setPromoteDeptId(e.target.value)} required>
            <option value="">{t('departments.choosDeptOption')}</option>
            {departments.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
          <button type="submit" disabled={promoting}>
            {promoting ? t('departments.promoting') : t('departments.promote')}
          </button>
        </form>
      </section>
    </div>
  );
}
