// Phân trang client-side đơn giản - phù hợp quy mô đồ án (danh sách file
// không quá lớn). Nếu dữ liệu tăng nhiều, bước tiếp theo là chuyển sang phân
// trang phía backend (Pageable), nhưng chưa cấp thiết ở quy mô hiện tại.
export default function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;

  return (
    <div className="pagination">
      <button type="button" className="mode-btn" disabled={page <= 1} onClick={() => onChange(page - 1)}>
        ‹
      </button>
      <span className="pagination-info">
        {page} / {totalPages}
      </span>
      <button
        type="button"
        className="mode-btn"
        disabled={page >= totalPages}
        onClick={() => onChange(page + 1)}
      >
        ›
      </button>
    </div>
  );
}
