import { useCallback, useEffect, useState } from 'react';
import { apiFetch } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useLanguage } from '../i18n/LanguageContext';
import Banner from '../components/Banner';
import { formatDate } from '../utils/format';

export default function AdminAttributesPage() {
  const { role, departmentName } = useAuth();
  const { t } = useLanguage();
  const isDeptAdmin = role === 'DEPT_ADMIN';
  const isAdmin = role === 'ADMIN';

  const [attributes, setAttributes] = useState([]);
  const [users, setUsers] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const [teacherForm, setTeacherForm] = useState({
    username: '',
    password: '',
    email: '',
    fullName: '',
    departmentId: '',
  });
  const [creatingTeacher, setCreatingTeacher] = useState(false);
  const [teacherSuccess, setTeacherSuccess] = useState('');

  const [newAttrName, setNewAttrName] = useState('');
  const [newAttrDesc, setNewAttrDesc] = useState('');
  const [newAttrDeptId, setNewAttrDeptId] = useState('');
  const [creating, setCreating] = useState(false);

  const [selectedUserId, setSelectedUserId] = useState('');
  const [userAttributes, setUserAttributes] = useState([]);
  const [userAttrLoading, setUserAttrLoading] = useState(false);
  const [assignAttrId, setAssignAttrId] = useState('');
  const [assigning, setAssigning] = useState(false);

  const [userSearch, setUserSearch] = useState('');
  const [bulkSelectedIds, setBulkSelectedIds] = useState(() => new Set());
  const [bulkAttrId, setBulkAttrId] = useState('');
  const [bulkAssigning, setBulkAssigning] = useState(false);
  const [bulkResult, setBulkResult] = useState('');

  const searchTerm = userSearch.trim().toLowerCase();
  const filteredUsers = searchTerm
    ? users.filter(
        (u) =>
          u.username.toLowerCase().includes(searchTerm) ||
          (u.fullName || '').toLowerCase().includes(searchTerm) ||
          (u.email || '').toLowerCase().includes(searchTerm)
      )
    : users;

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
      .catch((err) => setError(err.message || t('adminAttributes.loadFailed')))
      .finally(() => setLoading(false));
  }, [loadAttributes, loadUsers, loadDepartments, t]);

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
      setError(err.message || t('adminAttributes.createFailed'));
    } finally {
      setCreating(false);
    }
  }

  function updateTeacherField(field) {
    return (e) => setTeacherForm((prev) => ({ ...prev, [field]: e.target.value }));
  }

  async function handleCreateTeacher(e) {
    e.preventDefault();
    setError('');
    setTeacherSuccess('');
    setCreatingTeacher(true);
    try {
      const created = await apiFetch('/api/users', {
        method: 'POST',
        body: {
          ...teacherForm,
          role: 'DATA_OWNER',
          departmentId: teacherForm.departmentId ? Number(teacherForm.departmentId) : null,
        },
      });
      setTeacherSuccess(t('adminAttributes.createTeacherSuccess', { username: created?.username || teacherForm.username }));
      setTeacherForm({ username: '', password: '', email: '', fullName: '', departmentId: '' });
      await loadUsers();
    } catch (err) {
      setError(err.message || t('adminAttributes.createTeacherFailed'));
    } finally {
      setCreatingTeacher(false);
    }
  }

  async function handleDeleteAttribute(id) {
    setError('');
    try {
      await apiFetch(`/api/attributes/${id}`, { method: 'DELETE' });
      await loadAttributes();
    } catch (err) {
      setError(err.message || t('adminAttributes.deleteFailed'));
    }
  }

  const loadUserAttributes = useCallback(
    async (userId) => {
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
        setError(err.message || t('adminAttributes.loadUserAttrFailed'));
      } finally {
        setUserAttrLoading(false);
      }
    },
    [t]
  );

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
      setError(err.message || t('adminAttributes.assignFailed'));
    } finally {
      setAssigning(false);
    }
  }

  function toggleBulkUser(userId) {
    setBulkSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(userId)) {
        next.delete(userId);
      } else {
        next.add(userId);
      }
      return next;
    });
  }

  async function handleBulkAssign(e) {
    e.preventDefault();
    if (bulkSelectedIds.size === 0 || !bulkAttrId) return;
    setError('');
    setBulkResult('');
    setBulkAssigning(true);
    const attrId = Number(bulkAttrId);
    const targetIds = [...bulkSelectedIds];
    try {
      const results = await Promise.allSettled(
        targetIds.map((userId) =>
          apiFetch('/api/attributes/assign', { method: 'POST', body: { userId, attributeId: attrId } })
        )
      );
      const successCount = results.filter((r) => r.status === 'fulfilled').length;
      const failCount = results.length - successCount;
      setBulkResult(
        failCount === 0
          ? t('adminAttributes.bulkAssignAllSuccess', { count: successCount })
          : t('adminAttributes.bulkAssignPartial', { success: successCount, fail: failCount })
      );
      setBulkSelectedIds(new Set());
      setBulkAttrId('');
      if (selectedUserId && targetIds.includes(Number(selectedUserId))) {
        await loadUserAttributes(selectedUserId);
      }
    } finally {
      setBulkAssigning(false);
    }
  }

  async function handleRevoke(userAttributeId) {
    setError('');
    try {
      await apiFetch(`/api/attributes/revoke/${userAttributeId}`, { method: 'POST' });
      await loadUserAttributes(selectedUserId);
    } catch (err) {
      setError(err.message || t('adminAttributes.revokeFailed'));
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
      <h1>{t('adminAttributes.title')}</h1>
      <Banner message={error} />

      {isAdmin && (
        <section className="card">
          <h2>{t('adminAttributes.createTeacherTitle')}</h2>
          <p className="hint">{t('adminAttributes.createTeacherHint')}</p>
          <Banner type="success" message={teacherSuccess} />
          <form className="inline-form" onSubmit={handleCreateTeacher}>
            <input
              type="text"
              placeholder={t('register.username')}
              value={teacherForm.username}
              onChange={updateTeacherField('username')}
              required
            />
            <input
              type="password"
              placeholder={t('register.password')}
              value={teacherForm.password}
              onChange={updateTeacherField('password')}
              required
            />
            <input
              type="email"
              placeholder={t('register.email')}
              value={teacherForm.email}
              onChange={updateTeacherField('email')}
              required
            />
            <input
              type="text"
              placeholder={t('register.fullName')}
              value={teacherForm.fullName}
              onChange={updateTeacherField('fullName')}
              required
            />
            <select value={teacherForm.departmentId} onChange={updateTeacherField('departmentId')}>
              <option value="">{t('register.noDepartment')}</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
            <button type="submit" disabled={creatingTeacher}>
              {creatingTeacher ? t('adminAttributes.creating') : t('adminAttributes.createTeacherSubmit')}
            </button>
          </form>
        </section>
      )}

      <section className="card">
        <h2>{t('adminAttributes.listTitle')}</h2>
        {isDeptAdmin && (
          <p>{t('adminAttributes.deptAdminNotice', { department: departmentName })}</p>
        )}
        <form className="inline-form" onSubmit={handleCreateAttribute}>
          <input
            type="text"
            placeholder={t('adminAttributes.namePlaceholder')}
            value={newAttrName}
            onChange={(e) => setNewAttrName(e.target.value)}
            required
          />
          <input
            type="text"
            placeholder={t('adminAttributes.descPlaceholder')}
            value={newAttrDesc}
            onChange={(e) => setNewAttrDesc(e.target.value)}
          />
          {isDeptAdmin ? (
            <input type="text" value={`${t('adminAttributes.deptFieldPrefix')}${departmentName}`} disabled />
          ) : (
            <select value={newAttrDeptId} onChange={(e) => setNewAttrDeptId(e.target.value)}>
              <option value="">{t('adminAttributes.globalOption')}</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          )}
          <button type="submit" disabled={creating}>
            {creating ? t('adminAttributes.creating') : t('adminAttributes.create')}
          </button>
        </form>

        <table className="table">
          <thead>
            <tr>
              <th>{t('adminAttributes.colId')}</th>
              <th>{t('adminAttributes.colName')}</th>
              <th>{t('adminAttributes.colDesc')}</th>
              <th>{t('adminAttributes.colScope')}</th>
              <th>{t('adminAttributes.colDate')}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {attributes.map((attr) => (
              <tr key={attr.id}>
                <td>{attr.id}</td>
                <td>{attr.attributeName}</td>
                <td>{attr.description}</td>
                <td>{attr.issuerDepartmentName || t('common.global')}</td>
                <td>{formatDate(attr.createdAt)}</td>
                <td>
                  <button className="btn-danger" onClick={() => handleDeleteAttribute(attr.id)}>
                    {t('common.delete')}
                  </button>
                </td>
              </tr>
            ))}
            {attributes.length === 0 && (
              <tr>
                <td colSpan={6}>{t('adminAttributes.empty')}</td>
              </tr>
            )}
          </tbody>
        </table>
      </section>

      <section className="card">
        <h2>{t('adminAttributes.assignSectionTitle')}</h2>
        <p className="hint">{t('adminAttributes.bulkAssignHint')}</p>

        <input
          type="text"
          className="user-search-input"
          placeholder={t('adminAttributes.searchUserPlaceholder')}
          value={userSearch}
          onChange={(e) => setUserSearch(e.target.value)}
        />

        <div className="user-picklist">
          {filteredUsers.map((u) => (
            <div className="user-picklist-row" key={u.id}>
              <label className="attr-checkbox user-picklist-checkbox">
                <input
                  type="checkbox"
                  checked={bulkSelectedIds.has(u.id)}
                  onChange={() => toggleBulkUser(u.id)}
                />
                <span>
                  {u.username} <span className="role-badge">{t(`role.${u.role}`)}</span>
                </span>
              </label>
              <button
                type="button"
                className="mode-btn user-picklist-view-btn"
                onClick={() => handleSelectUser({ target: { value: String(u.id) } })}
              >
                {t('adminAttributes.viewDetail')}
              </button>
            </div>
          ))}
          {filteredUsers.length === 0 && <p className="hint">{t('adminAttributes.noUserMatch')}</p>}
        </div>

        <form className="inline-form" onSubmit={handleBulkAssign}>
          <select value={bulkAttrId} onChange={(e) => setBulkAttrId(e.target.value)} required>
            <option value="">{t('adminAttributes.chooseAttrOption')}</option>
            {attributes.map((attr) => (
              <option key={attr.id} value={attr.id}>
                {attr.attributeName}
              </option>
            ))}
          </select>
          <button type="submit" disabled={bulkAssigning || bulkSelectedIds.size === 0}>
            {bulkAssigning
              ? t('adminAttributes.assigning')
              : t('adminAttributes.bulkAssignSubmit', { count: bulkSelectedIds.size })}
          </button>
        </form>
        <Banner type="success" message={bulkResult} />

        {selectedUserId && (
          <>
            <h3 className="user-detail-heading">{t('adminAttributes.detailHeading')}</h3>
            <form className="inline-form" onSubmit={handleAssign}>
              <select value={assignAttrId} onChange={(e) => setAssignAttrId(e.target.value)} required>
                <option value="">{t('adminAttributes.chooseAttrOption')}</option>
                {attributes.map((attr) => (
                  <option key={attr.id} value={attr.id}>
                    {attr.attributeName}
                  </option>
                ))}
              </select>
              <button type="submit" disabled={assigning}>
                {assigning ? t('adminAttributes.assigning') : t('adminAttributes.assign')}
              </button>
            </form>

            {userAttrLoading ? (
              <p>{t('adminAttributes.loadingUserAttrs')}</p>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>{t('adminAttributes.colId')}</th>
                    <th>{t('adminAttributes.colName')}</th>
                    <th>{t('adminAttributes.colAssignedDate')}</th>
                    <th>{t('adminAttributes.colIssuer')}</th>
                    <th>{t('adminAttributes.colStatus')}</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {userAttributes.map((ua) => (
                    <tr key={ua.id}>
                      <td>{ua.id}</td>
                      <td>{ua.attributeName}</td>
                      <td>{formatDate(ua.assignedAt)}</td>
                      <td>{ua.issuedByUsername || t('adminAttributes.unknownIssuer')}</td>
                      <td>
                        {ua.revoked ? (
                          <span className="tag tag-revoked">{t('adminAttributes.statusRevoked')}</span>
                        ) : (
                          <span className="tag tag-active">{t('adminAttributes.statusActive')}</span>
                        )}
                      </td>
                      <td>
                        {!ua.revoked && (
                          <button className="btn-danger" onClick={() => handleRevoke(ua.id)}>
                            {t('common.revoke')}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                  {userAttributes.length === 0 && (
                    <tr>
                      <td colSpan={6}>{t('adminAttributes.noAttrsForUser')}</td>
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
