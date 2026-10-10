# Chạy auth và frontend local (nền P01)

Đây là runbook đang được cập nhật cho runtime hiện tại, không phải báo cáo trạng thái
lịch sử P01. Trạng thái task/merge xem [progress](phase1-progress.md); evidence riêng
P01 xem [handoff](handoffs/P01.md). Hướng dẫn cài chung ở [runbook](HUONG_DAN_CAI_DAT_VA_CHAY.md).

## Chạy local lâu dài trên Windows

Từ root repo, dùng script chung (Docker Desktop đang chạy, Node 22.14.0):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/local.ps1 -Action setup
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/local.ps1 -Action start
```

`setup` chỉ bổ sung giá trị còn thiếu/placeholder trong `.env`, sinh JWT key và mật
khẩu local ngẫu nhiên; giữ nguyên cấu hình đã có. Email/mật khẩu ở các biến
`DEMO_*_EMAIL`/`DEMO_*_PASSWORD` trong `.env` (Git ignored). Không cần tự tạo database
hay tài khoản bằng SQL. `demo` ở đây chỉ là profile bootstrap tài khoản thử bằng
credentials từ env; dữ liệu lưu thật, không phải database tạm hoặc mock.

`start` build/chạy backend Java21 và PostgreSQL16/PostGIS3.5 bằng Compose, dùng named
volume `database-data` của project; chạy Vite dev local và proxy tới `APP_PORT`.
Sau khi máy khởi động lại, chạy `start` để mở lại. Chạy lại không reset mật khẩu/role
đã lưu. Không đổi Compose project name nếu muốn tiếp tục dùng volume hiện tại.

- Frontend: `http://127.0.0.1:5173/login`.
- Backend/Swagger: host port `APP_PORT` trong `.env` (máy này: 18080).
- Database: `127.0.0.1:DB_PORT` (máy này: 55432), DB/user/password theo `.env`.
- Frontend logs: `.tools/local/frontend.log`, `.tools/local/frontend-error.log`.
- Backend logs: `docker compose logs --tail=100 backend` (không in env/credentials).

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/local.ps1 -Action status
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/local.ps1 -Action stop
```

`stop` dừng frontend do script quản lý và hai service Compose, **giữ volume/database**.
Không dùng `down -v`. Đây là local development: frontend Vite chạy trên host; frontend
container phục vụ production vẫn thuộc A2. Những cách chạy thủ công dưới đây vẫn dùng được.

Runtime hiện có E01–E11, S01 `/login`, S05 `/operations/disasters` và theme UI01-A.
E12–E15/S02/S03/S04/S06 chưa triển khai. Không register/refresh/logout API.
Yêu cầu: Docker đang chạy, Node **22.14.0**, npm đi kèm Node; Java 21 chỉ cần cho
backend host/Maven tests, không cần khi chạy backend bằng Compose. CI dùng cùng
Node từ `frontend/.nvmrc`. [Hướng dẫn DB/Docker](HUONG_DAN_CAI_DAT_VA_CHAY.md) vẫn áp dụng.

## Environment và tài khoản

Chỉ copy `.env.example` sang `.env` nếu chưa có file; không ghi đè file cá nhân.
Điền các biến DB như trước. JWT bắt buộc cho mọi profile chạy app, không có key mặc
định. Sinh key riêng bằng PowerShell rồi lưu vào `.env` local (không commit/log):

```powershell
$jwtBytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($jwtBytes)
$jwtKey = [Convert]::ToBase64String($jwtBytes)
# Dùng giá trị $jwtKey cho JWT_SECRET_BASE64 trong .env, không gửi nó vào chat/log.
```

| Biến | Bắt buộc / ý nghĩa |
| --- | --- |
| JWT_SECRET_BASE64 | Base64 hợp lệ của ít nhất 32 random bytes; thiếu/yếu =>startup fail |
| SPRING_PROFILES_ACTIVE | `demo` cho Compose có seed; host có thể `local,demo`; bỏ demo để không seed |
| DEMO_CITIZEN_ONE_EMAIL / DEMO_CITIZEN_ONE_PASSWORD | Citizen thứ nhất |
| DEMO_CITIZEN_TWO_EMAIL / DEMO_CITIZEN_TWO_PASSWORD | Citizen thứ hai |
| DEMO_AUTHORITY_EMAIL / DEMO_AUTHORITY_PASSWORD | Authority |

Sáu biến DEMO bắt buộc khi bật profile demo; ba email sau normalize phải khác nhau.
Email 3–254, mật khẩu không rỗng, tối đa 128 code points và **72 UTF-8 bytes** (BCrypt).
Thiếu/invalid demo config =>startup fail rõ tên biến/loại lỗi, không in giá trị.
Bootstrap chạy transaction và dùng first-writer-wins theo email: chạy lại không reset
password/role/UUID, kể cả thay env. Không dùng cách sửa env để đổi mật khẩu user đã có.
Không có account/secret trong migration hay runtime profile không demo.

## Backend local + frontend dev

Trong root PowerShell, nạp `.env` do chính bạn kiểm soát:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}
docker compose up -d --wait database
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,demo"
```

Nếu chỉ muốn backend không seed, dùng profiles `local`; vẫn phải có JWT key.
Maven/Flyway áp dụng migrations chưa chạy theo thứ tự (nhánh B2: V1–V5), không xóa volume.
Dừng Compose backend trước nếu tranh cổng. Host JDBC URL `DB_URL` phải chứa đúng
host port `DB_PORT`; Spring không tự ghép DB_PORT vào URL. Backend host dùng
`SERVER_PORT`, mặc định8080; `APP_PORT` chỉ là cổng publish của Compose.

Terminal thứ hai:

```powershell
cd frontend
node --version
npm ci
$env:API_PROXY_TARGET = 'http://127.0.0.1:8080'
npm run dev
```

Mở `http://127.0.0.1:5173/login`, nhập một trong ba tài khoản đã cấu hình. Vite proxy
`/api` tới backend; sửa API_PROXY_TARGET nếu backend đổi cổng. Đây là env chỉ dùng
trong Vite server, không `VITE_*`, không bake hostname backend/container vào bundle.
Frontend chỉ gọi relative `/api/...`, không gửi cookie, token chỉ nằm trong memory.
Token TTL 900 giây; reload/new tab phải login lại. Logout clear user/token/cache,
không revoke token cũ. Phiên xác nhận từ `/me`, không tin riêng response login.

Hiện sau login S01 hiển thị tài khoản thật và nút logout; chưa có report pages.
Authority mở S05 bằng link Thảm họa trên nav; Citizen không có link này.
Khi B3/C2 export routes thật, auto redirect theo role và navigation tự đăng ký.
Không tạo placeholder cho các màn hình còn thiếu. Router tự deny role sai; backend vẫn là security gate.

Swagger: `http://localhost:8080/swagger-ui/index.html` (đổi port theo cấu hình), công bố E01–E11;
health: `/actuator/health`. Các API tương lai vẫn deny. Không bật DEBUG/TRACE cho
request body, JWT hoặc Hibernate binds; app đặt mức INFO cho web/security, OFF binds.

## Backend Docker + frontend dev

Trong `.env` đặt `SPRING_PROFILES_ACTIVE=demo` và đủ key/demo credentials, rồi:

```powershell
docker compose up --build -d --wait
```

Chạy Vite như trên, proxy tới host `APP_PORT`. Chưa có frontend container/static
production proxy trong P01; A2 triển khai riêng. Không cần đổi sang hostname `backend`
trong browser. Không chạy `down -v` để reset tài khoản/database cá nhân.

## Checks

```powershell
.\mvnw.cmd clean verify
cd frontend
npm ci
npm run typecheck
npm test
npm run build
```

Backend tests dùng Testcontainers và key/password sinh tạm, không cần `.env`, không
bỏ integration tests khi thiếu Docker. Browser smoke thật (không mock/intercept API):

```powershell
# Backend + Vite đang chạy; nạp lại sáu biến DEMO của cùng backend vào terminal này.
cd frontend
npx playwright install chromium
$env:E2E_BASE_URL = 'http://127.0.0.1:5173'
npm run test:login
```

Tên script `test:login` được giữ tương thích nhưng chạy toàn bộ `frontend/e2e/`:
login ba tài khoản, S05 create/edit/resolve và UI01 keyboard/responsive/role guard.
S05 test tạo một disaster kiểm thử thật đã RESOLVED trong DB đang dùng; không chạy
trên dữ liệu production. Trace tắt để không lưu credentials/token. Full flow E2E
vẫn thuộc X2; CI chưa tự chạy browser suite. `test-results/` ignored.

Chạy `npm ci` khi Vite đã dừng để tránh Windows khóa native dependency. Script local
quản lý Vite thì dùng `-Action stop` trước, không tắt process khác bằng tên chung.
Trên Windows PowerShell, ưu tiên chạy script trực tiếp; redirect `*>` có thể biến
native stderr progress/warning thành lỗi PowerShell. Nếu log báo BUILD SUCCESS nhưng
process exit khác0, chạy lại trực tiếp và đối chiếu reports, không bỏ qua exit code.
