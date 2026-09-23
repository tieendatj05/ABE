// Lớp gọi API dùng chung cho toàn bộ frontend.
// Tự gắn Authorization header từ token lưu trong localStorage,
// tự parse JSON, và ném Error với message lấy từ body lỗi backend khi request thất bại.

export const TOKEN_STORAGE_KEY = 'abe_token';

function getToken() {
  try {
    return localStorage.getItem(TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
}

function buildHeaders(extraHeaders, isFormData) {
  const headers = { ...(extraHeaders || {}) };
  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  if (!isFormData && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  return headers;
}

async function extractErrorMessage(response) {
  const text = await response.text().catch(() => '');
  if (text) {
    try {
      const body = JSON.parse(text);
      if (body && body.message) {
        return body.message;
      }
    } catch {
      // body không phải JSON, bỏ qua và dùng statusText
    }
  }
  return response.statusText || `Lỗi ${response.status}`;
}

/**
 * Gọi API JSON thông thường.
 * @param {string} path đường dẫn tương đối, ví dụ '/api/auth/login'
 * @param {object} options { method, body, headers } - body là object JS thường (sẽ tự JSON.stringify) hoặc FormData
 * @returns {Promise<any>} dữ liệu JSON đã parse, hoặc null nếu response rỗng (204)
 */
export async function apiFetch(path, options = {}) {
  const { method = 'GET', body, headers, ...rest } = options;
  const isFormData = body instanceof FormData;

  const response = await fetch(path, {
    method,
    headers: buildHeaders(headers, isFormData),
    body: body == null ? undefined : isFormData ? body : JSON.stringify(body),
    ...rest,
  });

  if (!response.ok) {
    const message = await extractErrorMessage(response);
    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }

  const text = await response.text().catch(() => '');
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function parseFileNameFromContentDisposition(contentDisposition) {
  if (!contentDisposition) return null;
  // Hỗ trợ cả filename="..." và filename*=UTF-8''...
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match) {
    try {
      return decodeURIComponent(utf8Match[1]);
    } catch {
      return utf8Match[1];
    }
  }
  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
  if (plainMatch) {
    return plainMatch[1];
  }
  return null;
}

/**
 * Tải file nhị phân về máy người dùng.
 * @param {string} path đường dẫn download, ví dụ `/api/files/1/download`
 * @param {string} fallbackFileName tên file dùng nếu server không trả Content-Disposition
 */
export async function apiDownload(path, fallbackFileName) {
  const response = await fetch(path, {
    method: 'GET',
    headers: buildHeaders(),
  });

  if (!response.ok) {
    const message = await extractErrorMessage(response);
    throw new Error(message);
  }

  const blob = await response.blob();
  const contentDisposition = response.headers.get('Content-Disposition');
  const fileName = parseFileNameFromContentDisposition(contentDisposition) || fallbackFileName || 'download';

  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
