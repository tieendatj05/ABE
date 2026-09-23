# ABE File Sharing — Frontend

Frontend React (Vite, JavaScript thuần, không TypeScript) cho hệ thống chia sẻ file mã hoá theo
thuộc tính (Attribute Based Encryption).

## Yêu cầu

- Node.js >= 18
- Backend Spring Boot chạy ở `http://localhost:8080` (xem thư mục gốc dự án)

## Chạy dự án

```bash
npm install
npm run dev
```

Ứng dụng chạy tại `http://localhost:5173`. Mọi request tới `/api/...` được Vite dev server proxy
sang backend tại `http://localhost:8080` (cấu hình trong `vite.config.js`), nên không cần cấu hình
CORS ở backend khi phát triển.

## Build production

```bash
npm run build
```

Kết quả build nằm trong thư mục `dist/`.

## Cấu trúc chính

- `src/api/client.js` — hàm gọi API dùng chung (tự gắn JWT, parse lỗi, tải file binary).
- `src/auth/AuthContext.jsx` — quản lý phiên đăng nhập (lưu ở `localStorage`), `ProtectedRoute`
  theo role.
- `src/pages/` — các trang: đăng nhập, đăng ký, danh sách file, upload file, file của tôi, quản lý
  attribute (admin).

## Tài khoản & vai trò

- `ADMIN`: quản lý attribute, gán/thu hồi attribute cho user (tài khoản admin tạo sẵn ở backend,
  không đăng ký được qua form).
- `DATA_OWNER`: upload file kèm access policy, xem/tải/xoá file của mình.
- `DATA_USER`: xem danh sách file, tải file nếu attribute thoả access policy.
