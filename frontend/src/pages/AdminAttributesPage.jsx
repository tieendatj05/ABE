import { useCallback, useEffect, useState } from 'react';
import { apiFetch } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import Banner from '../components/Banner';

export default function AdminAttributesPage() {
  const { role, departmentName } = useAuth();
  const isDeptAdmin = role === 'DEPT_ADMIN';

  const [attributes, setAttributes] = useState([]);
  const [users, setUsers] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const [newAttrName, setNewAttrName] = useState('');
  const [newAttrDesc, setNewAttrDesc] = useState('');
  const [newAttrDeptId, setNewAttrDeptId] = useState('');
  const [creating, setCreating] = useState(false);

  const [selectedUserId, setSelectedUserId] = useState('');
  const [userAttributes, setUserAttributes] = useState([]);
  const [userAttrLoading, setUserAttrLoading] = useState(false);
  const [assignAttrId, setAssignAttrId] = useState('');
  const [assigning, setAssigning] = useState(false);

  const loadAttributes = useCallback(async () => {
    const data = await apiFetch('/api/attributes');
    setAttributes(data || []);
  }, []);

  const loadUsers = useCallback(async () => {
    const data = await apiFetch('/api/users');
    setUsers(data || []);
  }, []);

  const loadDepartments = useCallback(async () => {
    const data = await apiFetch('/api/departments');
    setDepartments(data || []);
  }, []);

  useEffect(() => {
    setLoading(true);
    setError('');
    Promise.all([loadAttributes(), loadUsers(), loadDepartments()])
      .catch((err) => setError(err.message || 'Không tải được dữ liệu'))
      .finally(() => setLoading(false));
  }, [loadAttributes, loadUsers, loadDepartments]);

  async function handleCreateAttribute(e) {
    e.preventDefault();
    setError('');
    setCreating(true);
    try {
      await apiFetch('/api/attributes', {
        method: 'POST',
        body: {
          attributeName: newAttrName,
          description: newAttrDesc,
          // DEPT_ADMIN: server luôn ép về đúng phòng ban của caller nên gửi
          // gì cũng không quan trọng, nhưng vẫn gửi null cho rõ ràng.
          departmentId: isDeptAdmin ? null : newAttrDeptId ? Number(newAttrDeptId) : null,
        },
      });
      setNewAttrName('');
      setNewAttrDesc('');
      setNewAttrDeptId('');
      await loadAttributes();
    } catch (err) {
      setError(err.message || 'Tạo attribute thất bại');
    } finally {
      setCreating(false);
    }
  }

  async function handleDeleteAttribute(id) {
    setError('');
    try {
      await apiFetch(`/api/attributes/${id}`, { method: 'DELETE' });
      await loadAttributes();
    } catch (err) {
      setError(err.message || 'Xoá attribute thất bại');
    }
  }

  const loadUserAttributes = useCallback(async (userId) => {
    if (!userId) {
      setUserAttributes([]);
      return;
    }
    setUserAttrLoading(true);
    setError('');
    try {
      const data = await apiFetch(`/api/attributes/user/${userId}`);
      setUserAttributes(data || []);
    } catch (err) {
      setError(err.message || 'Không tải được attribute của user');
    } finally {
      setUserAttrLoading(false);
    }
  }, []);

  function handleSelectUser(e) {
    const userId = e.target.value;
    setSelectedUserId(userId);
    loadUserAttributes(userId);
  }

  async function handleAssign(e) {
    e.preventDefault();
    if (!selectedUserId || !assignAttrId) return;
    setError('');
    setAssigning(true);
    try {
      await apiFetch('/api/attributes/assign', {
        method: 'POST',
        body: { userId: Number(selectedUserId), attributeId: Number(assignAttrId) },
      });
      setAssignAttrId('');
      await loadUserAttributes(selectedUserId);
    } catch (err) {
      setError(err.message || 'Gán attribute thất bại');
    } finally {
      setAssigning(false);
    }
  }

  async function handleRevoke(userAttributeId) {
    setError('');
    try {
      await apiFetch(`/api/attributes/revoke/${userAttributeId}`, { method: 'POST' });
      await loadUserAttributes(selectedUserId);
    } catch (err) {
      setError(err.message || 'Thu hồi attribute thất bại');
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
      <h1>Quản lý Attribute</h1>
      <Banner message={error} />

      <section className="card">
        <h2>Danh sách attribute</h2>
        {isDeptAdmin && (
          <p>
            Bạn đang quản lý attribute với vai trò DEPT_ADMIN của <strong>{departmentName}</strong> - chỉ
            thấy attribute toàn cục (chỉ đọc) và attribute do phòng ban của bạn phát hành.
          </p>
        )}
        <form className="inline-form" onSubmit={handleCreateAttribute}>
          <input
            type="text"
            placeholder="Tên attribute, ví dụ department:CNTT"
            value={newAttrName}
            onChange={(e) => setNewAttrName(e.target.value)}
            required
          />
          <input
            type="text"
            placeholder="Mô tả"
            value={newAttrDesc}
            onChange={(e) => setNewAttrDesc(e.target.value)}
          />
          {isDeptAdmin ? (
            <input type="text" value={`Phòng ban: ${departmentName}`} disabled />
          ) : (
            <select value={newAttrDeptId} onChange={(e) => setNewAttrDeptId(e.target.value)}>
              <option value="">-- Toàn cục (không chọn phòng ban) --</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          )}
          <button type="submit" disabled={creating}>
            {creating ? 'Đang tạo...' : 'Tạo attribute'}
          </button>
        </form>

        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tên attribute</th>
              <th>Mô tả</th>
              <th>Phạm vi</th>
              <th>Ngày tạo</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {attributes.map((attr) => (
              <tr key={attr.id}>
                <td>{attr.id}</td>
                <td>{attr.attributeName}</td>
                <td>{attr.description}</td>
                <td>{attr.issuerDepartmentName || 'Toàn cục'}</td>
                <td>{formatDate(attr.createdAt)}</td>
                <td>
                  <button className="btn-danger" onClick={() => handleDeleteAttribute(attr.id)}>
                    Xoá
                  </button>
                </td>
              </tr>
            ))}
            {attributes.length === 0 && (
              <tr>
                <td colSpan={6}>Chưa có attribute nào.</td>
              </tr>
            )}
          </tbody>
        </table>
      </section>

      <section className="card">
        <h2>Gán / thu hồi attribute cho user</h2>
        <label className="block-label">
          Chọn user
          <select value={selectedUserId} onChange={handleSelectUser}>
            <option value="">-- Chọn user --</option>
            {users.map((u) => (
              <option key={u.id} value={u.id}>
                {u.username} ({u.role})
              </option>
            ))}
          </select>
        </label>

        {selectedUserId && (
          <>
            <form className="inline-form" onSubmit={handleAssign}>
              <select value={assignAttrId} onChange={(e) => setAssignAttrId(e.target.value)} required>
                <option value="">-- Chọn attribute để gán --</option>
                {attributes.map((attr) => (
                  <option key={attr.id} value={attr.id}>
                    {attr.attributeName}
                  </option>
                ))}
              </select>
              <button type="submit" disabled={assigning}>
                {assigning ? 'Đang gán...' : 'Gán attribute'}
              </button>
            </form>

            {userAttrLoading ? (
              <p>Đang tải attribute của user...</p>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Attribute</th>
                    <th>Ngày gán</th>
                    <th>Cấp bởi</th>
                    <th>Trạng thái</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {userAttributes.map((ua) => (
                    <tr key={ua.id}>
                      <td>{ua.id}</td>
                      <td>{ua.attributeName}</td>
                      <td>{formatDate(ua.assignedAt)}</td>
                      <td>{ua.issuedByUsername || 'Không rõ (dữ liệu cũ)'}</td>
                      <td>
                        {ua.revoked ? (
                          <span className="tag tag-revoked">Đã thu hồi</span>
                        ) : (
                          <span className="tag tag-active">Đang hoạt động</span>
                        )}
                      </td>
                      <td>
                        {!ua.revoked && (
                          <button className="btn-danger" onClick={() => handleRevoke(ua.id)}>
                            Thu hồi
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                  {userAttributes.length === 0 && (
                    <tr>
                      <td colSpan={6}>User chưa được gán attribute nào.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            )}
          </>
        )}
      </section>
    </div>
  );
}

function formatDate(value) {
  if (!value) return '';
  try {
    return new Date(value).toLocaleString('vi-VN');
  } catch {
    return value;
  }
}
