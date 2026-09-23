import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, ProtectedRoute, useAuth } from './auth/AuthContext';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import AdminAttributesPage from './pages/AdminAttributesPage';
import DepartmentsPage from './pages/DepartmentsPage';
import AuditLogPage from './pages/AuditLogPage';
import UploadPage from './pages/UploadPage';
import MyFilesPage from './pages/MyFilesPage';
import FilesPage from './pages/FilesPage';

function NavBar() {
  const { isAuthenticated, username, role, departmentName, logout } = useAuth();

  if (!isAuthenticated) {
    return null;
  }

  return (
    <header className="navbar">
      <div className="navbar-brand">ABE File Sharing</div>
      <nav className="navbar-links">
        <Link to="/">Danh sách file</Link>
        {role === 'DATA_OWNER' && (
          <>
            <Link to="/upload">Upload file</Link>
            <Link to="/my-files">File của tôi</Link>
            <Link to="/audit-logs">Lịch sử truy cập file của tôi</Link>
          </>
        )}
        {(role === 'ADMIN' || role === 'DEPT_ADMIN') && (
          <Link to="/admin/attributes">Quản lý Attribute</Link>
        )}
        {role === 'ADMIN' && (
          <>
            <Link to="/admin/departments">Quản lý phòng ban</Link>
            <Link to="/audit-logs">Nhật ký truy cập</Link>
          </>
        )}
      </nav>
      <div className="navbar-user">
        <span>
          {username} <span className="role-badge">{role}</span>
          {departmentName && <span className="role-badge">{departmentName}</span>}
        </span>
        <button onClick={logout}>Đăng xuất</button>
      </div>
    </header>
  );
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <FilesPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/attributes"
        element={
          <ProtectedRoute roles={['ADMIN', 'DEPT_ADMIN']}>
            <AdminAttributesPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/departments"
        element={
          <ProtectedRoute roles={['ADMIN']}>
            <DepartmentsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/audit-logs"
        element={
          <ProtectedRoute roles={['ADMIN', 'DATA_OWNER']}>
            <AuditLogPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/upload"
        element={
          <ProtectedRoute roles={['DATA_OWNER']}>
            <UploadPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/my-files"
        element={
          <ProtectedRoute roles={['DATA_OWNER']}>
            <MyFilesPage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <NavBar />
      <AppRoutes />
    </AuthProvider>
  );
}
