# ABE System

Hệ thống quản lý chia sẻ file dựa trên thuộc tính (Attribute-Based Access Control), đồ án tốt nghiệp — mô phỏng phân quyền truy cập chi tiết trong hệ thống giáo dục dựa trên **vai trò và khoa/bộ môn** của giảng viên/nhân viên (NT219 — Mật mã học). Data Owner upload tài liệu (đề thi, bảng điểm, tài liệu học vụ...), mã hoá nội dung bằng AES; khoá AES được bảo vệ theo một **chính sách truy cập** (biểu thức AND/OR trên các attribute, ví dụ `department:CNTT AND position:giang_vien`) bằng Shamir Secret Sharing. Chỉ Data User có đủ attribute thoả chính sách mới khôi phục được khoá AES để giải mã file.

Ngoài lõi ABE, hệ thống còn có 2 tính năng mở rộng:

- **Mô hình ABE phi tập trung hoá (decentralized ABE)**: bên cạnh ADMIN toàn cục, mỗi khoa/bộ môn có thể có một `DEPT_ADMIN` — một KGC thu nhỏ chỉ được tạo/xoá attribute và gán/thu hồi attribute đó cho giảng viên/nhân viên **trong phạm vi khoa của chính mình**.
- **Audit log (nhật ký truy cập)**: mọi lần upload/tải xuống (thành công hoặc bị từ chối)/xoá file đều được ghi lại — kể cả sau khi file bị xoá — phục vụ yêu cầu truy vết (compliance), ví dụ chứng minh đề thi chưa từng bị mở trước ngày thi.

## Vai trò (Role)

| Role | Mô tả |
|---|---|
| `ADMIN` | Admin/KGC toàn cục (Key Generation Center) — quản lý phòng ban, kho attribute toàn cục, gán/thu hồi attribute cho user, và phong `DEPT_ADMIN` cho từng phòng ban |
| `DEPT_ADMIN` | KGC của một khoa/phòng ban (mô hình ABE phi tập trung hoá) — chỉ tạo/xoá/gán/thu hồi được attribute do phòng ban của chính mình phát hành, chỉ cho user cùng phòng ban |
| `DATA_OWNER` | Upload và mã hoá file theo access policy |
| `DATA_USER` | Tải và giải mã file nếu attribute của họ thoả policy |

## Công nghệ

- Backend: Java 25, Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation)
- PostgreSQL 18, quản lý schema bằng **Flyway** (`src/main/resources/db/migration/`)
- JWT (jjwt) cho xác thực stateless, BCrypt cho hash password
- Lombok, Maven (dùng qua `mvnw`/`mvnw.cmd`, không cần cài Maven riêng)
- Lõi ABE tự cài đặt (không dùng thư viện pairing-based CP-ABE): AES-256/GCM mã hoá nội dung file
  + Shamir Secret Sharing (tự cài bằng `BigInteger`, xem gói `crypto/`) chia khoá AES theo cây
  chính sách AND/OR — xem chi tiết ở mục [Lõi mã hoá (ABE)](#lõi-mã-hoá-abe) bên dưới.
- Frontend: React (Vite, JavaScript thuần) trong thư mục [frontend/](frontend/), gọi REST API ở trên,
  đa ngôn ngữ Việt/Anh.
- Docker Compose để chạy cả hệ thống (Postgres + backend + frontend) bằng 1 lệnh — xem mục
  [Chạy bằng Docker Compose](#chạy-bằng-docker-compose) bên dưới.

## Yêu cầu môi trường

- JDK 25
- PostgreSQL đang chạy (khuyến nghị dùng pgAdmin để quản lý) — hoặc dùng Docker Compose để khỏi cần
  cài Postgres/Node riêng, xem mục [Chạy bằng Docker Compose](#chạy-bằng-docker-compose)

## Chạy bằng Docker Compose

Cách nhanh nhất để chạy thử toàn bộ hệ thống (không cần cài JDK/Node/Postgres riêng), chỉ cần Docker:

```bash
docker compose up --build
```

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`
- Postgres: `localhost:5432` (user/password `postgres`/`postgres`, database `abe_system`)

Lần đầu chạy trên database rỗng, Flyway tự tạo toàn bộ bảng theo
[V1__init.sql](src/main/resources/db/migration/V1__init.sql). Dữ liệu Postgres và file đã upload
được lưu ở Docker volume (`db_data`, `storage_data`) nên `docker compose down` không mất dữ liệu (chỉ
mất khi thêm cờ `-v`). Vẫn cần tạo tài khoản ADMIN đầu tiên bằng SQL như mục
[Tạo tài khoản ADMIN đầu tiên](#4-tạo-tài-khoản-admin-đầu-tiên) bên dưới (kết nối vào Postgres ở
`localhost:5432` bằng psql/pgAdmin).

**Lưu ý:** cấu hình Docker Compose này (bao gồm mật khẩu Postgres đặt cứng) chỉ dùng để chạy thử/demo
local, chưa validate cho production.

## Cài đặt & chạy (không dùng Docker)

### 1. Tạo database

Trong pgAdmin (hoặc `psql`), tạo database tên `abe_system` trên server local (`localhost:5432`).

### 2. Cấu hình kết nối

File [src/main/resources/application.properties](src/main/resources/application.properties) đã có sẵn cấu hình cho môi trường local:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/abe_system
spring.datasource.username=postgres
spring.datasource.password=postgres
```

Đổi `username`/`password` nếu Postgres của bạn khác. `spring.jpa.hibernate.ddl-auto=update` nghĩa là Hibernate tự tạo/cập nhật bảng theo entity mỗi lần chạy app — không cần chạy migration thủ công.

`jwt.secret`/`jwt.expiration-ms` cũng nằm trong file này, chỉ dùng cho local/đồ án. Nếu triển khai thật, phải chuyển secret ra biến môi trường.

### 3. Chạy ứng dụng

```bash
./mvnw.cmd spring-boot:run
```

App chạy ở `http://localhost:8080`. Thấy dòng `Started AbeSystemApplication` trong log nghĩa là kết nối DB thành công.

**Lưu ý:** `http://localhost:8080` chỉ là REST API thuần (dán vào trình duyệt sẽ không thấy giao diện,
chỉ thấy lỗi 401/404 JSON — đó là bình thường). Giao diện để bấm nút test nằm ở `http://localhost:5173`
sau khi chạy frontend, xem mục [Chạy frontend (React)](#chạy-frontend-react) bên dưới.

### 4. Tạo tài khoản ADMIN đầu tiên

Không có endpoint public để tự đăng ký ADMIN (chặn cố ý trong `AuthService`, tránh ai cũng tự phong ADMIN được). Tạo thủ công bằng SQL:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (username, password, email, full_name, role, created_at)
VALUES ('admin', crypt('admin123', gen_salt('bf')), 'admin@abe.local', 'System Admin', 'ADMIN', now());
```

Chạy trong Query Tool của pgAdmin (chọn đúng database `abe_system`), hoặc qua `psql`. Sau đó đăng nhập bằng `admin` / `admin123` (đổi mật khẩu này trước khi dùng thật).

### Lưu ý khi nâng cấp từ CSDL cũ (đã tạo trước khi có role `DEPT_ADMIN`)

`spring.jpa.hibernate.ddl-auto=update` tự thêm cột/bảng mới, nhưng **không** tự cập nhật lại
CHECK constraint của cột enum (`users.role`) đã tồn tại từ trước — constraint đó vẫn chỉ cho phép
3 giá trị cũ (`ADMIN`, `DATA_OWNER`, `DATA_USER`) và sẽ chặn mọi thao tác gán role `DEPT_ADMIN` với
lỗi `violates check constraint "users_role_check"`. Nếu gặp lỗi này (chỉ xảy ra với DB đã tồn tại từ
trước, DB tạo mới hoàn toàn không bị), chạy 1 lần:

```sql
ALTER TABLE users DROP CONSTRAINT users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('ADMIN','DEPT_ADMIN','DATA_OWNER','DATA_USER'));
```

## API hiện có

Base URL: `http://localhost:8080`

### Auth (`/api/auth`) — public, không cần token

**Đăng ký** (chỉ được chọn role `DATA_OWNER` hoặc `DATA_USER`; `departmentId` tuỳ chọn; `password`
phải ≥ 8 ký tự và có đủ chữ hoa/chữ thường/số/ký tự đặc biệt):
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"owner1","password":"Secret123!","email":"owner1@test.com","fullName":"Data Owner","role":"DATA_OWNER","departmentId":1}'
```

**Đăng nhập** — trả về JWT:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"owner1","password":"Secret123!"}'
```

Response mẫu:
```json
{"token":"eyJhbGciOi...", "tokenType":"Bearer", "userId":1, "username":"owner1", "role":"DATA_OWNER", "departmentId":1, "departmentName":"Khoa CNTT"}
```

Các API bên dưới đều cần header `Authorization: Bearer <token>`.

### Department (`/api/departments`) — khoa/phòng ban

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/api/departments` | public (không cần token) | Liệt kê phòng ban — dùng cho dropdown ở trang đăng ký |
| POST | `/api/departments` | `ADMIN` | Tạo phòng ban — body `{"name":"Khoa CNTT","code":"CNTT","description":"..."}` |
| DELETE | `/api/departments/{id}` | `ADMIN` | Xoá phòng ban (báo lỗi 400 nếu còn user/attribute đang tham chiếu) |

### Attribute (`/api/attributes`) — `ADMIN` hoặc `DEPT_ADMIN`

`ADMIN` có toàn quyền trên mọi attribute (toàn cục lẫn của bất kỳ phòng ban nào). `DEPT_ADMIN` chỉ
tạo/xoá/gán/thu hồi được attribute do **chính phòng ban của mình** phát hành, và chỉ cho user cùng
phòng ban — vi phạm sẽ nhận `403` (mô hình ABE phi tập trung hoá).

| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/attributes` | Tạo attribute mới — body `{"attributeName":"department:CNTT","description":"...","departmentId":1}`. `departmentId` bỏ trống = attribute toàn cục (chỉ `ADMIN` làm được); `DEPT_ADMIN` luôn bị ép về phòng ban của chính mình bất kể gửi gì |
| GET | `/api/attributes` | `ADMIN` thấy tất cả; `DEPT_ADMIN` thấy attribute toàn cục + của phòng ban mình |
| DELETE | `/api/attributes/{id}` | Xoá attribute |
| POST | `/api/attributes/assign` | Gán attribute cho user — body `{"userId":1,"attributeId":1}` |
| POST | `/api/attributes/revoke/{userAttributeId}` | Thu hồi attribute đã gán (không xoá bản ghi, chỉ đánh dấu `revoked=true`) |
| GET | `/api/attributes/user/{userId}` | Xem attribute (kèm trạng thái revoked, người cấp `issuedByUsername`) của 1 user |

### User (`/api/users`) — `ADMIN` hoặc `DEPT_ADMIN`

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/api/users` | `ADMIN`/`DEPT_ADMIN` | `ADMIN` thấy tất cả user; `DEPT_ADMIN` chỉ thấy user cùng phòng ban với mình |
| POST | `/api/users/promote-dept-admin` | `ADMIN` | Phong 1 user hiện có thành `DEPT_ADMIN` của 1 phòng ban — body `{"userId":5,"departmentId":1}` (đặt `role` và `department` đồng thời) |

### Audit log (`/api/audit-logs`) — nhật ký truy cập file

| Method | Path | Role | Mô tả |
|---|---|---|---|
| GET | `/api/audit-logs` | `ADMIN` | Toàn bộ sự kiện `UPLOAD`/`DOWNLOAD_SUCCESS`/`DOWNLOAD_DENIED`/`DELETE` trong hệ thống, mới nhất trước |
| GET | `/api/audit-logs/mine` | đã đăng nhập | Sự kiện của các file **mình sở hữu** (Data Owner theo dõi ai đã/cố truy cập hồ sơ của mình) |

Mỗi bản ghi audit log là **snapshot độc lập** (không FK tới file/user) nên vẫn còn nguyên vẹn kể cả
sau khi file bị xoá — đúng tinh thần audit trail cho hệ thống giáo dục (vd chứng minh không ai mở đề
thi trước ngày thi).

### File (`/api/files`) — lõi ABE

| Method | Path | Role | Mô tả |
|---|---|---|---|
| POST | `/api/files/upload` | `DATA_OWNER` | multipart: field `file` (binary) + `accessPolicy` (text, vd `"(department:CNTT AND position:giang_vien) OR role:ADMIN"`) |
| GET | `/api/files` | đã đăng nhập | Liệt kê toàn bộ file trong hệ thống (metadata, không phải nội dung) |
| GET | `/api/files/mine` | `DATA_OWNER` | Liệt kê file của chính owner đang gọi |
| GET | `/api/files/{id}/download` | đã đăng nhập | Trả về nội dung file **đã giải mã** nếu attribute (còn hiệu lực) của user thoả `accessPolicy`; `403` kèm message rõ lý do nếu không |
| DELETE | `/api/files/{id}` | đã đăng nhập | Chỉ owner hoặc `ADMIN` mới xoá được |

Ví dụ upload + download bằng curl:
```bash
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -F "file=@bao_cao.pdf" \
  -F "accessPolicy=department:CNTT AND position:giang_vien"

curl -o bao_cao.pdf http://localhost:8080/api/files/1/download \
  -H "Authorization: Bearer $USER_TOKEN"
```

Ví dụ theo kịch bản giáo dục: Data Owner (giảng viên Khoa CNTT) upload đề thi cuối kỳ, chỉ giảng viên
Khoa CNTT hoặc ADMIN mới giải mã được **trước ngày thi** (sinh viên dù có tài khoản hợp lệ cũng không
đủ attribute `position:giang_vien` nên không mở được, ngăn lộ đề):
```bash
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -F "file=@de_thi_cuoi_ky.pdf" \
  -F "accessPolicy=(department:CNTT AND position:giang_vien) OR role:ADMIN"
```

Gọi sai role hoặc thiếu token sẽ nhận `403 Forbidden`; sai username/password khi login nhận `401 Unauthorized`.

## Lõi mã hoá (ABE)

Không dùng CP-ABE dạng pairing-based (không có thư viện Java tốt, quá nặng cho đồ án). Thay vào đó
tự cài đặt đúng theo mô tả: **AES-256/GCM mã hoá nội dung file + Shamir Secret Sharing chia khoá AES
theo cây chính sách AND/OR** (Shamir's construction cho general access structure, 1979). Toàn bộ nằm
trong gói `src/main/java/com/abe/system/abe_system/crypto/`:

- `PolicyParser` — parse chuỗi `accessPolicy` (vd `"(department:CNTT AND position:giang_vien) OR role:ADMIN"`) thành cây AND/OR/Leaf. AND chặt hơn OR (giống `&&`/`||`), ngoặc đơn ghi đè precedence.
- `ShamirSecretSharing` — Shamir thuần trên `BigInteger` (GF(P), P là số nguyên tố ~261-bit cố định vĩnh viễn trong code).
- `PolicyKeyDistributor` — đi xuống cây: node `OR` nhân bản secret cho mọi nhánh (1-trong-n), node `AND` chia secret bằng Shamir threshold = số nhánh con (cần đủ tất cả). Kết quả (`attributeName` -> share) lưu JSON vào `FileMetadata.encryptedAesKey`.
- `AesFileCipher` — mã hoá/giải mã nội dung file bằng AES-256/GCM (IV ngẫu nhiên prepend vào ciphertext lưu trên đĩa ở `file.storage-dir`, mặc định thư mục `storage/` — nội dung file **không bao giờ** đi qua DB).

Lúc download, `FileService` lấy tập attribute còn hiệu lực (`revoked=false`) của user, parse lại
`accessPolicy`, thử ghép lại share (`PolicyKeyDistributor.reconstruct`) — thành công thì giải mã file,
thất bại thì trả `403` kèm message rõ lý do.

## Chạy frontend (React)

```bash
cd frontend
npm install
npm run dev
```

Mở `http://localhost:5173`. Vite dev server tự proxy các request `/api/**` sang backend ở
`http://localhost:8080` (xem `frontend/vite.config.js`), nên **phải chạy backend trước**. Giao diện có:
đăng nhập/đăng ký (kèm chọn khoa/phòng ban tuỳ chọn), trang quản lý Attribute (dùng chung cho
`ADMIN`/`DEPT_ADMIN`, tự động giới hạn phạm vi theo phòng ban), trang **Quản lý phòng ban** (`ADMIN`:
tạo/xoá phòng ban, phong `DEPT_ADMIN`), trang Upload file (Data Owner), trang "File của tôi", trang
danh sách toàn bộ file kèm nút Tải xuống (hiện rõ thông báo khi bị từ chối vì thiếu attribute), và
trang **Nhật ký truy cập** (`ADMIN` xem toàn hệ thống, Data Owner xem lịch sử truy cập file của
mình). Xem thêm [frontend/README.md](frontend/README.md).

## Cấu trúc project

```
src/main/java/com/abe/system/abe_system/
├── entity/        # User, Role, Department, Attribute, UserAttribute, FileMetadata, AuditLog, AuditAction
├── repository/    # Spring Data JPA repository cho từng entity
├── dto/           # Request/Response cho API (không bao giờ trả entity thô có chứa password)
├── security/      # JwtService, JwtAuthenticationFilter, CustomUserDetailsService
├── config/        # SecurityConfig (phân quyền theo role, JWT stateless, CORS cho frontend dev)
├── crypto/        # PolicyParser, ShamirSecretSharing, PolicyKeyDistributor, AesFileCipher — lõi ABE (không đổi)
├── service/       # AuthService, AttributeService (scoping theo phòng ban), DepartmentService,
│                  # UserAdminService, FileService, AuditLogService — nghiệp vụ chính
├── exception/     # Exception nghiệp vụ + GlobalExceptionHandler (trả JSON lỗi thống nhất)
└── controller/    # AuthController, AttributeController, DepartmentController, UserController,
                   # FileController, AuditLogController

frontend/          # React (Vite) - giao diện test toàn bộ API trên
```

## Đã hoàn thành

- [x] Upload file (`FileController`) — mã hoá nội dung file bằng AES-256/GCM trước khi lưu
- [x] Sinh/parse biểu thức `accessPolicy` (AND/OR trên attribute, có ngoặc đơn)
- [x] Chia sẻ khoá AES theo policy bằng **Shamir Secret Sharing**, lưu vào `encryptedAesKey`
- [x] Download file — kiểm tra attribute còn hiệu lực (`revoked=false`), ghép lại share, giải mã file
- [x] **Mô hình ABE phi tập trung hoá**: entity `Department`, role `DEPT_ADMIN`, `AttributeService`
  giới hạn phạm vi tạo/gán/thu hồi attribute theo phòng ban, endpoint phong `DEPT_ADMIN`
- [x] **Audit log**: ghi nhận upload/download (thành công/bị từ chối)/xoá file, sống sót qua việc xoá
  file (snapshot, không FK), xem được qua `/api/audit-logs` (ADMIN) và `/api/audit-logs/mine` (chủ file)
- [x] Test tự động cho crypto core + service + security (`ShamirSecretSharingTest`, `PolicyParserTest`,
  `PolicyKeyDistributorTest`, `AesFileCipherTest`, `FileServiceIntegrationTest`, `AttributeServiceTest`,
  `DepartmentServiceTest`, `AuthServiceTest`, `JwtServiceTest`, `SecurityAuthorizationIntegrationTest` —
  chạy trên H2, không cần Postgres)
- [x] Trang/giao diện frontend (React, xem mục trên), đa ngôn ngữ Việt/Anh
- [x] **Chống dò mật khẩu (brute-force)**: khoá tạm tài khoản sau 5 lần đăng nhập sai liên tiếp trong
  15 phút (`LoginAttemptService`, cấu hình qua `security.login.max-attempts`/`security.login.lockout-minutes`)
- [x] **Mật khẩu mạnh bắt buộc khi đăng ký**: ≥ 8 ký tự, có đủ chữ hoa/chữ thường/số/ký tự đặc biệt
- [x] **Chọn thuộc tính bằng giao diện** thay vì gõ tay cú pháp policy ở trang Upload (chế độ "Chọn
  nhanh" build sẵn từ attribute của chính owner, hoặc "Nâng cao" để tự viết policy phức tạp)
- [x] **Trực quan hoá cây chính sách AND/OR** (`PolicyTreeDiagram`) ở trang Upload và trong danh sách
  file - cùng cấu trúc cây mà `PolicyKeyDistributor` dùng thật để chia khoá Shamir
- [x] **Dashboard thống kê cho ADMIN** (`/admin/dashboard`): số file theo khoa/phòng ban, số sự kiện
  theo loại hành động, tỉ lệ tải xuống thành công/bị từ chối
- [x] **Đổi mật khẩu tự phục vụ** (`PATCH /api/auth/change-password`, mọi role) và **phân trang**
  danh sách file (client-side, 10 dòng/trang)
- [x] **Flyway migration** thay `ddl-auto=update` (`db/migration/V1__init.sql`, xem mục
  [Công nghệ](#công-nghệ)) — Hibernate chỉ validate schema, không tự sinh DDL nữa
- [x] **Docker Compose** chạy cả hệ thống bằng 1 lệnh — xem mục
  [Chạy bằng Docker Compose](#chạy-bằng-docker-compose)

## Có thể làm thêm (không thuộc phạm vi lõi ABE)

- [ ] Đổi mật khẩu / quản lý hồ sơ user
- [ ] Phân trang cho danh sách file khi số lượng lớn
- [ ] Refresh token cho JWT (hiện token sống 24h, không thu hồi được giữa chừng) — cân nhắc kỹ vì đây
  là thay đổi kiến trúc lớn hơn (ảnh hưởng cả luồng lưu token ở frontend), chưa cấp thiết cho quy mô đồ án
- [ ] Deploy production thật (hiện `jwt.secret`, mật khẩu DB đang để plaintext trong `application.properties`, chỉ dùng cho local/đồ án)

## Test

```bash
./mvnw.cmd test
```

Chạy trên H2 in-memory (`src/test/resources/application.properties`), không cần Postgres đang chạy.
