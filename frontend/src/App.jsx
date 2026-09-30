import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, ProtectedRoute, useAuth } from './auth/AuthContext';
import { LanguageProvider, useLanguage } from './i18n/LanguageContext';
import LanguageToggle from './components/LanguageToggle';
import UserMenu from './components/UserMenu';
import WelcomePage from './pages/WelcomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import AdminAttributesPage from './pages/AdminAttributesPage';
import DepartmentsPage from './pages/DepartmentsPage';
import AuditLogPage from './pages/AuditLogPage';
import UploadPage from './pages/UploadPage';
import MyFilesPage from './pages/MyFilesPage';
import MyAttributesPage from './pages/MyAttributesPage';
import ChangePasswordPage from './pages/ChangePasswordPage';
import FilesPage from './pages/FilesPage';

const ADMIN_ROLES = ['ADMIN', 'DEPT_ADMIN'];

// ADMIN/DEPT_ADMIN dùng theme tím-magenta gốc (quyền lực, quản trị); DATA_OWNER/
// DATA_USER dùng theme xanh cyan riêng để không bị nhầm 2 loại giao diện khi
// demo (vd quên đang đăng nhập role nào, bấm nhầm trang).
function useRoleThemeClass(role) {
  if (ADMIN_ROLES.includes(role)) return 'theme-admin';
  if (role) return 'theme-user';
  return '';
}

function NavBar() {
  const { isAuthenticated, role } = useAuth();
  const { t } = useLanguage();

  if (!isAuthenticated) {
    return null;
  }

  return (
    <header className="navbar">
      <div className="navbar-brand">{t('appName')}</div>
      <nav className="navbar-links">
        <Link to="/">{t('nav.files')}</Link>
        {(role === 'DATA_OWNER' || role === 'DATA_USER') && (
          <Link to="/my-attributes">{t('nav.myAttributes')}</Link>
        )}
        {role === 'DATA_OWNER' && (
          <>
            <Link to="/upload">{t('nav.upload')}</Link>
            <Link to="/my-files">{t('nav.myFiles')}</Link>
            <Link to="/audit-logs">{t('nav.myAuditLogs')}</Link>
          </>
        )}
        {(role === 'ADMIN' || role === 'DEPT_ADMIN') && (
          <Link to="/admin/attributes">{t('nav.manageAttributes')}</Link>
        )}
        {role === 'ADMIN' && (
          <>
            <Link to="/admin/departments">{t('nav.manageDepartments')}</Link>
            <Link to="/audit-logs">{t('nav.auditLogs')}</Link>
            <Link to="/admin/dashboard">{t('nav.dashboard')}</Link>
          </>
        )}
      </nav>
      <div className="navbar-user">
        <LanguageToggle />
        <UserMenu />
      </div>
    </header>
  );
}

function RootRoute() {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <FilesPage /> : <Navigate to="/welcome" replace />;
}

function WelcomeRoute() {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Navigate to="/" replace /> : <WelcomePage />;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/welcome" element={<WelcomeRoute />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/" element={<RootRoute />} />
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
        path="/admin/dashboard"
        element={
          <ProtectedRoute roles={['ADMIN']}>
            <DashboardPage />
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
      <Route
        path="/my-attributes"
        element={
          <ProtectedRoute roles={['DATA_OWNER', 'DATA_USER']}>
            <MyAttributesPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/change-password"
        element={
          <ProtectedRoute>
            <ChangePasswordPage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function AppShell() {
  const { role } = useAuth();
  const themeClass = useRoleThemeClass(role);

  return (
    <div className={themeClass}>
      <NavBar />
      <AppRoutes />
    </div>
  );
}

export default function App() {
  return (
    <LanguageProvider>
      <AuthProvider>
        <AppShell />
      </AuthProvider>
    </LanguageProvider>
  );
}
