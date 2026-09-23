import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { apiFetch, TOKEN_STORAGE_KEY } from '../api/client';

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

  const login = useCallback(async (username, password) => {
    const response = await apiFetch('/api/auth/login', {
      method: 'POST',
      body: { username, password },
    });
    persistAuth(response);
    setAuth({
      token: response.token,
      userId: response.userId,
      username: response.username,
      role: response.role,
      departmentId: response.departmentId ?? null,
      departmentName: response.departmentName ?? null,
    });
    return response;
  }, []);

  const register = useCallback(async (payload) => {
    const response = await apiFetch('/api/auth/register', {
      method: 'POST',
      body: payload,
    });
    persistAuth(response);
    setAuth({
      token: response.token,
      userId: response.userId,
      username: response.username,
      role: response.role,
      departmentId: response.departmentId ?? null,
      departmentName: response.departmentName ?? null,
    });
    return response;
  }, []);

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
    }),
    [auth, login, register, logout]
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
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (roles && roles.length > 0 && !roles.includes(role)) {
    return (
      <div className="page">
        <div className="banner banner-error">
          Bạn không có quyền truy cập trang này.
        </div>
      </div>
    );
  }

  return children;
}
