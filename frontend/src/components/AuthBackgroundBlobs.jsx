// Nền trang trí cho các trang Đăng nhập/Đăng ký/Chào mừng - vài khối tròn mờ
// (blur) trôi chậm bằng thuần CSS, tông xanh lá nhạt. Thay cho hiệu ứng
// Three.js cũ (nặng, tối màu) - nhẹ, không cần WebGL, vẫn tạo cảm giác
// chuyển động cho nền thay vì phẳng lặng.
export default function AuthBackgroundBlobs() {
  return (
    <div className="auth-blobs" aria-hidden="true">
      <div className="auth-blob auth-blob-1" />
      <div className="auth-blob auth-blob-2" />
      <div className="auth-blob auth-blob-3" />
    </div>
  );
}
