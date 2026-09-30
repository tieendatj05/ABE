import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { apiFetch, TOKEN_STORAGE_KEY } from '../api/client';
import { useLanguage } from '../i18n/LanguageContext';

const USER_STORAGE_KEY = 'abe_user';

const AuthContext = createContext(null);

const EMPTY_AUTH = {
  token: null,
  userId: null,
  username: null,
  role: null,
  departmentId: null,
  departmentName: null,
};

function readStoredAuth() {
  try {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    const userJson = localStorage.getItem(USER_STORAGE_KEY);
    if (!token || !userJson) {
      return EMPTY_AUTH;
    }
    const user = JSON.parse(userJson);
    return {
      token,
      userId: user.userId,
      username: user.username,
      role: user.role,
      departmentId: user.departmentId ?? null,
      departmentName: user.departmentName ?? null,
    };
  } catch {
    return EMPTY_AUTH;
  }
}

function persistAuth(authResponse) {
  localStorage.setItem(TOKEN_STORAGE_KEY, authResponse.token);
  localStorage.setItem(
    USER_STORAGE_KEY,
    JSON.stringify({
      userId: authResponse.userId,
      username: authResponse.username,
      role: authResponse.role,
      departmentId: authResponse.departmentId ?? null,
      departmentName: authResponse.departmentName ?? null,
    })
  );
}

function clearAuth() {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
  localStorage.removeItem(USER_STORAGE_KEY);
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(readStoredAuth);

  const applyAuthResponse = useCallback((response) => {
    persistAuth(response);
    setAuth({
      token: response.token,
      userId: response.userId,
      username: response.username,
      role: response.role,
      departmentId: response.departmentId ?? null,
      departmentName: response.departmentName ?? null,
    });
  }, []);

  // ADMIN/DEPT_ADMIN bắt buộc 2FA: nếu response có twoFactorChallenge nghĩa là
  // password đúng nhưng CHƯA đăng nhập xong - KHÔNG lưu token (vì chưa có token
  // thật), trả nguyên response để LoginPage tự chuyển sang màn nhập mã 6 số.
  const login = useCallback(
    async (username, password) => {
      const response = await apiFetch('/api/auth/login', {
        method: 'POST',
        body: { username, password },
      });
      if (!response.twoFactorChallenge) {
        applyAuthResponse(response);
      }
      return response;
    },
    [applyAuthResponse]
  );

  // Bước 2 khi 2FA kích hoạt LẦN ĐẦU (kèm QR vừa quét) - xem AuthService.confirmTwoFactorSetup.
  const completeTwoFactorSetup = useCallback(
    async (ticket, code) => {
      const response = await apiFetch('/api/auth/2fa/confirm-setup', {
        method: 'POST',
        body: { ticket, code },
      });
      applyAuthResponse(response);
      return response;
    },
    [applyAuthResponse]
  );

  // Bước 2 khi 2FA đã kích hoạt từ trước - chỉ cần nhập mã hiện tại.
  const verifyTwoFactor = useCallback(
    async (ticket, code) => {
      const response = await apiFetch('/api/auth/2fa/verify', {
        method: 'POST',
        body: { ticket, code },
      });
      applyAuthResponse(response);
      return response;
    },
    [applyAuthResponse]
  );

  const register = useCallback(
    async (payload) => {
      const response = await apiFetch('/api/auth/register', {
        method: 'POST',
        body: payload,
      });
      applyAuthResponse(response);
      return response;
    },
    [applyAuthResponse]
  );

  const logout = useCallback(() => {
    clearAuth();
    setAuth(EMPTY_AUTH);
  }, []);

  const value = useMemo(
    () => ({
      ...auth,
      isAuthenticated: Boolean(auth.token),
      login,
      register,
      logout,
      completeTwoFactorSetup,
      verifyTwoFactor,
    }),
    [auth, login, register, logout, completeTwoFactorSetup, verifyTwoFactor]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth phải được dùng bên trong AuthProvider');
  }
  return ctx;
}

/**
 * Bảo vệ route: yêu cầu đăng nhập, và (tuỳ chọn) đúng role.
 * @param {{roles?: string[], children: React.ReactNode}} props
 */
export function ProtectedRoute({ roles, children }) {
  const { isAuthenticated, role } = useAuth();
  const { t } = useLanguage();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (roles && roles.length > 0 && !roles.includes(role)) {
    return (
      <div className="page">
        <div className="banner banner-error">{t('protectedRoute.forbidden')}</div>
      </div>
    );
  }

  return children;
}
