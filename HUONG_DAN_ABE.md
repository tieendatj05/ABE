# ABE System – Hướng dẫn chạy chương trình & vai trò trong hệ thống

Tài liệu này giải thích: (1) cách chạy chương trình, (2) ABE (Attribute-Based Encryption)
được áp dụng vào đồ án như thế nào, và (3) hệ thống hoạt động ra sao với 3 nhóm người dùng
chính: **Admin**, **Giáo viên** (Data Owner) và **Sinh viên** (Data User).

## 1. Tổng quan

Đây là hệ thống quản lý & chia sẻ file có phân quyền chi tiết theo **thuộc tính** (attribute),
mô phỏng bối cảnh trường học: giảng viên upload tài liệu (đề thi, bảng điểm...), hệ thống mã hoá
file, và chỉ người có đúng thuộc tính (khoa, chức vụ...) mới giải mã/tải được — ngay cả khi họ
có tài khoản hợp lệ trong hệ thống.

Gồm 2 phần:
- **Backend**: Java 25 + Spring Boot, PostgreSQL, JWT — nằm ở `src/main/java/...`
- **Frontend**: React (Vite) — nằm ở `frontend/`

## 2. Cách chạy chương trình

### 2.1 Cách nhanh nhất — Docker Compose (khuyên dùng)

Chỉ cần cài Docker, không cần cài JDK/Node/Postgres riêng:

```bash
docker compose up --build
```

Chạy xong sẽ có:
- Backend API: `http://localhost:8080`
- Frontend (giao diện): `http://localhost:5173`
- Postgres: `localhost:5432` (user/pass: `postgres`/`postgres`, database `abe_system`)

Vì không có endpoint đăng ký công khai cho tài khoản `ADMIN` (cố ý chặn trong `AuthService` để
tránh ai cũng tự phong Admin), **lần đầu chạy phải tạo Admin bằng SQL** (dùng pgAdmin/psql kết
nối vào `localhost:5432`):

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (username, password, email, full_name, role, created_at)
VALUES ('admin', crypt('admin123', gen_salt('bf')), 'admin@abe.local', 'System Admin', 'ADMIN', now());
```

Sau đó đăng nhập ở `http://localhost:5173` bằng `admin` / `admin123`.

### 2.2 Chạy thủ công (không dùng Docker)

1. Tạo database `abe_system` trong Postgres (local).
2. Kiểm tra `src/main/resources/application.properties` — mặc định đã trỏ đúng
   `localhost:5432/abe_system`, user/pass `postgres`/`postgres`. Đổi lại nếu Postgres của bạn
   khác.
3. Chạy backend:
   ```bash
   ./mvnw.cmd spring-boot:run
   ```
   Thấy log `Started AbeSystemApplication` là backend đã lên (`http://localhost:8080` — vào
   thẳng URL này bằng browser sẽ thấy lỗi 401/404 JSON, đó là bình thường vì đây chỉ là REST
   API, không có giao diện).
4. Tạo tài khoản Admin đầu tiên bằng SQL (giống mục 2.1).
5. Chạy frontend (giao diện thật để bấm nút test):
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Mở `http://localhost:5173`. **Phải chạy backend trước** vì Vite proxy `/api/**` sang
   `localhost:8080`.

### 2.3 Chạy test

```bash
./mvnw.cmd test
```
Chạy trên H2 in-memory, không cần Postgres đang chạy.

## 3. ABE được áp dụng vào đồ án như thế nào

### 3.1 Ý tưởng chung

ABE (Attribute-Based Encryption) = mã hoá theo thuộc tính: thay vì mã hoá file cho một người cụ
thể, người upload (Data Owner) gắn vào file một **chính sách truy cập** (access policy) viết
bằng biểu thức AND/OR trên các attribute, ví dụ:

```
(department:CNTT AND position:giang_vien) OR role:ADMIN
```

Chỉ ai đang sở hữu **đủ** attribute để thoả policy này mới giải mã được file — bất kể họ có tài
khoản hợp lệ hay không.

### 3.2 Cách hệ thống tự cài đặt (không dùng thư viện CP-ABE pairing-based)

Đồ án không dùng thư viện CP-ABE dựa trên pairing (không có lib Java tốt, quá nặng). Thay vào
đó tự cài đặt lại đúng ý tưởng bằng 2 kỹ thuật kinh điển, nằm trong package
`src/main/java/com/abe/system/abe_system/crypto/`:

- **AES-256/GCM** (`AesFileCipher`) — mã hoá **nội dung file** bằng 1 khoá AES ngẫu nhiên. Nội
  dung file mã hoá lưu trên đĩa (`storage/`), **không bao giờ** đi qua database.
- **Shamir Secret Sharing** (`ShamirSecretSharing`, tự cài bằng `BigInteger`) — dùng để chia
  **khoá AES** đó thành nhiều phần (share) theo đúng cấu trúc cây AND/OR của policy:
  - `PolicyParser` parse chuỗi policy thành cây (Leaf = 1 attribute, node AND, node OR).
  - `PolicyKeyDistributor` đi từ gốc cây xuống: node **OR** → nhân bản y nguyên secret cho mọi
    nhánh con (ai có 1 trong các nhánh cũng ghép được); node **AND** → chia secret bằng Shamir
    với threshold = số nhánh con (phải có **đủ tất cả** nhánh mới ghép lại được).
  - Kết quả cuối cùng là một bảng `attributeName -> share`, lưu dạng JSON vào cột
    `FileMetadata.encryptedAesKey`.

Nói ngắn gọn: **policy AND/OR quyết định cách chia khoá**, còn **attribute mà user đang có**
quyết định họ ghép được bao nhiêu share — ghép đủ theo đúng cấu trúc cây thì ra lại khoá AES gốc.

### 3.3 Luồng khi upload (mã hoá)

1. Data Owner chọn file + nhập/chọn access policy.
2. Backend sinh khoá AES ngẫu nhiên, mã hoá nội dung file (AES-256/GCM), lưu file mã hoá vào
   `storage/`.
3. Backend parse policy → cây AND/OR, dùng Shamir chia khoá AES theo cây → lưu các share vào DB
   (`encryptedAesKey`, dạng JSON).
4. Metadata file (tên, policy, đường dẫn...) lưu vào bảng `file_metadata`.

### 3.4 Luồng khi download (giải mã)

1. Backend lấy toàn bộ attribute **còn hiệu lực** (`revoked=false`) mà user đang đăng nhập được
   cấp.
2. Parse lại policy của file, thử ghép lại share bằng các attribute user có
   (`PolicyKeyDistributor.reconstruct`).
3. Ghép thành công → ra lại khoá AES gốc → giải mã file → trả file gốc về cho user.
4. Ghép thất bại (thiếu attribute) → trả `403 Forbidden` kèm message giải thích rõ lý do.
5. Mọi lần download (thành công hoặc bị từ chối) và upload/xoá đều được ghi vào **audit log**
   (`AuditLogService`) — log này là **snapshot độc lập**, không liên kết khoá ngoại tới
   file/user, nên vẫn còn nguyên vẹn kể cả sau khi file đã bị xoá (phục vụ truy vết, ví dụ chứng
   minh không ai mở đề thi trước ngày thi).

## 4. Vai trò & cách hoạt động

Hệ thống có 4 role trong code (`ADMIN`, `DEPT_ADMIN`, `DATA_OWNER`, `DATA_USER`), nhưng xét theo
bối cảnh trường học thì tương ứng với 3 nhóm người dùng: **Admin**, **Giáo viên**, **Sinh viên**.

### 4.1 Admin

Gồm 2 cấp:

- **ADMIN** (toàn cục — Key Generation Center): 
  - Quản lý phòng ban/khoa (`/admin/departments`): tạo/xoá khoa.
  - Quản lý attribute toàn hệ thống (`/admin/attributes`): tạo attribute (toàn cục hoặc gắn với
    1 khoa cụ thể), gán/thu hồi attribute cho bất kỳ user nào.
  - Phong 1 user thường thành **`DEPT_ADMIN`** cho 1 khoa cụ thể.
  - Xem toàn bộ **audit log** hệ thống (`/audit-logs`) và **dashboard thống kê**
    (`/admin/dashboard`: số file theo khoa, tỉ lệ download thành công/bị từ chối...).
  - Có thể xoá file của bất kỳ ai.
  - Tạo bằng SQL thủ công (mục 2.1), không có endpoint đăng ký public.

- **DEPT_ADMIN** (mô hình ABE **phi tập trung hoá**): là "KGC thu nhỏ" của 1 khoa — ví dụ
  trưởng khoa CNTT. Chỉ được tạo/xoá/gán/thu hồi các attribute **do khoa mình phát hành**, và
  chỉ cho user **cùng khoa**. Làm ngoài phạm vi đó sẽ bị chặn `403`. Do `ADMIN` phong lên từ 1
  user có sẵn.

### 4.2 Giáo viên (role `DATA_OWNER` trong code)

Là người **tạo & mã hoá tài liệu**:

- Đăng ký tài khoản public (`/register`, chọn role `DATA_OWNER` + khoa của mình).
- Được Admin/DEPT_ADMIN của khoa gán các attribute mô tả mình, ví dụ `position:giang_vien`,
  `department:CNTT` — xem ở trang "Attribute của tôi" (`/my-attributes`).
- **Upload file** (`/upload`): chọn file, đặt access policy — có 2 chế độ:
  - *Chọn nhanh*: build sẵn policy từ chính các attribute mình đang có (không cần gõ tay).
  - *Nâng cao*: tự viết policy phức tạp dạng `(department:CNTT AND position:giang_vien) OR role:ADMIN`.
  - Có sơ đồ trực quan hoá cây chính sách AND/OR khi upload, giúp hiểu rõ ai sẽ mở được file.
- Xem file mình đã upload (`/my-files`).
- Xem **audit log của riêng file mình** (`/audit-logs` — Data Owner chỉ thấy log của file mình
  sở hữu): biết được ai đã tải/cố tải file của mình, thành công hay bị từ chối.
- Có thể xoá file của chính mình.

Ví dụ thực tế: giảng viên Khoa CNTT upload đề thi cuối kỳ, đặt policy
`(department:CNTT AND position:giang_vien) OR role:ADMIN` → chỉ giảng viên Khoa CNTT hoặc Admin
mở được **trước ngày thi**; sinh viên dù có tài khoản hợp lệ cũng không có attribute
`position:giang_vien` nên không tải được (chặn lộ đề).

### 4.3 Sinh viên (role `DATA_USER` trong code)

Là người **chỉ tải & giải mã** (không upload):

- Đăng ký tài khoản public (`/register`, chọn role `DATA_USER` + khoa của mình).
- Được Admin/DEPT_ADMIN gán các attribute phù hợp (ví dụ `department:CNTT`, hoặc một attribute
  riêng như `position:sinh_vien` nếu hệ thống có định nghĩa).
- Xem attribute của mình (`/my-attributes`).
- Xem danh sách toàn bộ file trong hệ thống (`/` — trang Files, có phân trang) và bấm **Tải
  xuống**:
  - Nếu attribute hiện có đủ để thoả policy của file → tải về file **đã giải mã** sẵn.
  - Nếu thiếu attribute → hiện rõ thông báo bị từ chối và lý do (ví dụ thiếu
    `position:giang_vien`), **không** thấy được nội dung file.
- Không thấy menu Upload / My Files / Audit log (chỉ dành cho Data Owner/Admin).

Nói cách khác: Sinh viên trong đồ án này đóng vai một Data User "yếu attribute" — dùng để minh
hoạ rằng có tài khoản hợp lệ **không đồng nghĩa** với việc mở được mọi file, đúng bản chất của
ABE (khác với phân quyền theo role đơn giản).

## 5. Bảng tóm tắt nhanh

| Ai | Role trong code | Làm được gì |
|---|---|---|
| Admin (toàn trường) | `ADMIN` | Quản lý khoa, attribute, phong Dept Admin, xem toàn bộ audit log + dashboard |
| Trưởng khoa | `DEPT_ADMIN` | Quản lý attribute/gán attribute **trong khoa mình** |
| Giáo viên | `DATA_OWNER` | Upload + mã hoá file kèm policy, xem file/audit log của mình |
| Sinh viên | `DATA_USER` | Chỉ xem & tải file nếu attribute của mình thoả policy |

## 6. Đọc thêm

- `README.md` (gốc) — chi tiết API đầy đủ (curl example cho từng endpoint).
- `src/main/java/com/abe/system/abe_system/crypto/` — code lõi ABE (Shamir + AES + parser cây policy).
- `frontend/README.md` — chi tiết giao diện frontend.
