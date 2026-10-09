# Hướng dẫn cài đặt và chạy GDRN sau khi clone

Tài liệu dành cho thành viên mới cài dự án trên máy cá nhân. Chạy các lệnh bên dưới
tại thư mục gốc repo, nơi có `pom.xml` và `docker-compose.yml`.

**Trạng thái hiện tại:** dự án có backend Spring Boot, PostgreSQL/PostGIS, Flyway,
Swagger và health check; P01 đã có login/me, credential persistence và frontend login.
Đọc [P01: JWT/demo env, Node và frontend](P01_LOCAL.md) trước khi chạy backend.
Frontend ở cổng 5173, backend không phục vụ trang HTML `/login`.

## Chạy nhanh bằng PowerShell

Sau khi cài Docker Desktop và bật Linux containers, tại thư mục repo:

```powershell
.\scripts\dev.ps1 init
.\scripts\dev.ps1 up
```

`init` chỉ tạo `.env` nếu thiếu, sinh mật khẩu local ngẫu nhiên dùng chung cho
database và host backend cùng JWT key base64 riêng; không in secret hoặc ghi đè
file cũ. Với `.env` trước P01 thiếu JWT key, chạy `scripts/local.ps1 -Action setup`
để điền cấu hình thiếu và demo accounts, giữ các giá trị đã cấu hình. Nếu đã có volume
database từ lần chạy trước, điền credentials gốc của volume vào `.env` trước `up`.
Thay password trong `.env` không đổi password của database đã khởi tạo.
`up` cũng gọi `init` khi cần, build image, chờ cả hai container healthy và kiểm tra
HTTP thật. Khi thành công, script in đường dẫn Swagger đúng theo port đang publish.
`dev.ps1` quản lý backend/database. Để chạy thêm SPA login, dùng `local.ps1 -Action start`
theo [hướng dẫn P01](P01_LOCAL.md); chưa có frontend production container.

| Lệnh | Kết quả |
| --- | --- |
| `.\scripts\dev.ps1 status` | Trạng thái và port container |
| `.\scripts\dev.ps1 smoke` | Kiểm tra health, liveness, readiness, OpenAPI, Swagger |
| `.\scripts\dev.ps1 smoke -BaseUrl http://localhost:8081` | Kiểm tra backend chạy host hoặc port riêng |
| `.\scripts\dev.ps1 verify` | `mvnw.cmd clean verify`, cần JDK 21 và Docker |
| `.\scripts\dev.ps1 down` | Dừng/xóa container, giữ named volume và `.env` |

Script dùng được trên Windows PowerShell 5.1 và PowerShell 7. Có thể gọi bằng đường
dẫn tuyệt đối từ bất kỳ thư mục nào. Nếu chính sách máy chặn script, dùng quy trình
thủ công bên dưới theo chính sách của máy. Script không tự thay execution policy.
Docker/server/native command lỗi sẽ trả exit code 1; không coi container vừa chạy
là ứng dụng đã sẵn sàng. Health không lộ chi tiết kết nối database.

Xem [prompt chia phần nhỏ](PROMPT_NEN_MONG.md) và [handoff nền tảng](handoffs/F00.md).

## 1. Chuẩn bị công cụ

| Công cụ | Chạy toàn bộ bằng Docker | Chạy backend trực tiếp trên máy |
| --- | --- | --- |
| Git | Cần | Cần |
| Docker Desktop hoặc Docker Engine + Compose v2 | Cần | Cần cho database và integration test |
| JDK 21 | Không cần cài trên máy | Cần; đặt `JAVA_HOME` trỏ đến JDK 21 và thêm Java vào `PATH` |
| Maven cài riêng | Không cần | Không cần; repo có Maven Wrapper |
| PostgreSQL cài riêng | Không cần | Không cần; dùng database trong Docker |

Trên Windows, mở Docker Desktop và dùng **Linux containers**. Với Docker Engine
trên Linux, bảo đảm Docker daemon đang chạy và tài khoản có quyền sử dụng Docker.
Sau khi cài công cụ hoặc sửa `PATH`, mở terminal mới.

Kiểm tra:

```text
git --version
docker --version
docker compose version
docker info
```

Nếu chạy backend trên máy, kiểm tra thêm:

```text
java -version
javac -version
```

Cả `java` và `javac` nên hiển thị phiên bản 21. Lần chạy đầu cần Internet để tải
Maven, thư viện Java và Docker images; có thể mất vài phút.

## 2. Clone repository

```text
git clone https://github.com/ThePhapp/2627I_INT3105_3.git
cd 2627I_INT3105_3
```

Nếu đã clone, chỉ cần mở terminal tại thư mục repo, không clone lại.

## 3. Tạo cấu hình môi trường

File `.env` không được đưa lên Git nên máy mới phải tự tạo từ `.env.example`.
Chỉ chạy lệnh sao chép khi chưa có `.env`, tránh ghi đè cấu hình đang dùng.

**Windows PowerShell:**

```powershell
Copy-Item .env.example .env
```

**macOS/Linux:**

```bash
cp .env.example .env
```

Mở `.env` bằng trình soạn thảo. Thay hai giá trị `replace-with-a-local-password`
bằng cùng một mật khẩu riêng cho môi trường local; đồng thời cấu hình JWT key và
demo credentials theo [P01 setup](P01_LOCAL.md):

- `POSTGRES_PASSWORD`: mật khẩu khởi tạo database trong Docker.
- `DB_PASSWORD`: mật khẩu backend chạy trên máy dùng để kết nối database.

Để dùng được các lệnh nạp `.env` trong tài liệu này, giữ dạng đơn giản `KEY=value`,
không có khoảng trắng quanh dấu `=` hoặc chú thích cuối dòng. Có thể dùng chuỗi
ngẫu nhiên chỉ gồm chữ và số cho mật khẩu local để tránh khác biệt xử lý ký tự giữa shell.
Không commit `.env` hoặc chia sẻ mật khẩu trong tài liệu.

### Cổng mặc định và cổng thay thế

Repo mặc định dùng backend `8080`, database `5432`. Các cổng `18080` và `55432`
đã dùng trên máy phát triển trước đó là cấu hình riêng trong `.env`, **không tự có
sau khi clone**.

Nếu cổng mặc định bị chiếm hoặc Windows không cho phép bind, sửa/thêm các dòng:

```dotenv
APP_PORT=18080
DB_PORT=55432
DB_URL=jdbc:postgresql://localhost:55432/gdrn
SERVER_PORT=18080
```

| Biến | Ý nghĩa |
| --- | --- |
| `POSTGRES_DB` | Tên database, mặc định trong mẫu là `gdrn` |
| `POSTGRES_USER` | Tài khoản database, mặc định trong mẫu là `gdrn` |
| `POSTGRES_PASSWORD` | Mật khẩu database khi khởi tạo lần đầu |
| `DB_PORT` | Cổng database được công bố trên máy cá nhân |
| `APP_PORT` | Cổng backend được công bố khi chạy bằng Compose |
| `DB_URL` | JDBC URL của backend chạy trực tiếp trên máy |
| `DB_USERNAME`, `DB_PASSWORD` | Tài khoản kết nối khi backend chạy trực tiếp trên máy |
| `SERVER_PORT` | Cổng backend khi chạy trực tiếp trên máy; mặc định 8080 |

Nếu đổi tên database hoặc tài khoản, cập nhật cả `DB_URL` và `DB_USERNAME` tương ứng.
Compose tự kết nối backend với `database:5432` trong mạng Docker, không dùng cổng
host trong `DB_URL` của `.env`.

## 4. Cách A — Chạy toàn bộ bằng Docker

Đây là cách nhanh nhất để kiểm tra dự án sau khi clone; không cần cài JDK trên máy.
Sau khi hoàn thành bước tạo `.env`, chạy:

```text
docker compose config --quiet
docker compose up --build -d --wait
docker compose ps
```

Kết quả mong đợi: dịch vụ `database` và `backend` đều có trạng thái `healthy`.
Flyway chạy khi backend khởi động; không cần tạo bảng hoặc import SQL thủ công.

Với cổng mặc định, mở:

| Chức năng | Địa chỉ |
| --- | --- |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Health check | http://localhost:8080/actuator/health |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

Nếu đã đặt `APP_PORT=18080`, thay `8080` bằng `18080` trong các URL trên.
Health check phải trả về `{"status":"UP"}`. Swagger có đúng E01 login/E02 me.
Các API tương lai vẫn bị deny; thiếu token 401, role/route không được phép 403.

Xem log nếu khởi động lỗi:

```text
docker compose logs --tail=100 backend
docker compose logs --tail=100 database
```

Theo dõi log liên tục bằng `docker compose logs -f backend`; nhấn `Ctrl+C` để thoát
xem log, container vẫn chạy.

## 5. Cách B — Database trong Docker, backend chạy trên máy

Cách này dùng khi chỉnh sửa/debug Java trong IDE. Cần JDK 21. Nếu đã chạy cách A,
dừng backend trong Docker để tránh tranh cổng. Database vẫn dùng chung volume local.

```text
docker compose stop backend
docker compose up -d --wait database
```

**Spring Boot không tự đọc `.env`.** Phải nạp các biến vào đúng terminal dùng để chạy
Maven; mở terminal mới thì cần nạp lại.

### Windows PowerShell

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}
.\mvnw.cmd --version
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

### macOS/Linux

Chỉ nạp file `.env` do bạn kiểm soát, có cú pháp phù hợp với shell:

```bash
chmod +x mvnw
set -a
. ./.env
set +a
./mvnw --version
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Kiểm tra Java 21 trong kết quả `mvnw --version`. Sau khi xuất hiện thông báo
`Started GdrnApplication`, mở các URL ở bước 4 theo cổng `SERVER_PORT` (mặc định
8080). Biến `APP_PORT` không thay đổi cổng của backend chạy trực tiếp trên máy.

Nhấn `Ctrl+C` tại terminal để dừng backend local. Nếu chạy bằng nút Run/Debug của
IDE, cấu hình các biến môi trường tương tự trong run configuration của IDE; IDE
đang mở không tự nhận các biến vừa đặt trong một terminal khác.

## 6. Kết nối và kiểm tra database

Có thể dùng công cụ quản lý PostgreSQL với thông tin trong `.env`:

| Trường | Giá trị |
| --- | --- |
| Host | `localhost` |
| Port | `DB_PORT`, mặc định `5432`, hoặc `55432` nếu đã đổi |
| Database | `POSTGRES_DB`, mặc định `gdrn` |
| Username | `POSTGRES_USER`, mặc định `gdrn` |
| Password | Giá trị `POSTGRES_PASSWORD` bạn đã đặt |

Hoặc mở `psql` có sẵn trong container, với tên mặc định:

```text
docker compose exec database psql -U gdrn -d gdrn
```

Nếu đã đổi user/database, thay hai giá trị `gdrn` trong lệnh. Trong `psql`, chạy:

```sql
SELECT postgis_version();
SELECT version, description, success FROM flyway_schema_history;
```

Gõ `\q` để thoát. Flyway history chỉ xuất hiện sau khi backend đã khởi động và chạy
migration. V1 bật PostGIS, V2 tạo identity_users và identity_credentials. Chỉ profile
`demo` với đủ env mới tạo ba tài khoản; không có dữ liệu mẫu/mật khẩu trong SQL.

## 7. Chạy kiểm thử

Cần JDK 21 và Docker daemon đang chạy. Không bắt buộc bật stack Compose hoặc tạo
`.env` để chạy test: Testcontainers tạo database PostGIS riêng và tự dọn sau khi chạy.

**PowerShell:**

```powershell
.\mvnw.cmd clean verify
```

**macOS/Linux:**

```bash
chmod +x mvnw
./mvnw clean verify
```

Kết quả mong đợi là `BUILD SUCCESS`, không có test bị lỗi hoặc bỏ qua. Lệnh này chạy
architecture tests, unit tests nếu có, integration tests và đóng gói ứng dụng.
Chỉ chạy `mvnw test` sẽ không chạy integration tests nên chưa đủ để xác nhận hoàn tất.

Kiểm tra riêng cấu hình Docker, không cần chứa mật khẩu thật trong output:

```text
docker compose --env-file .env.example config --quiet
```

## 8. Các lệnh dùng hằng ngày

| Mục đích | Lệnh |
| --- | --- |
| Bật lại stack đã build | `docker compose up -d --wait` |
| Build lại và chạy sau khi sửa code | `docker compose up --build -d --wait` |
| Xem trạng thái | `docker compose ps` |
| Dừng dịch vụ, giữ container và dữ liệu | `docker compose stop` |
| Dừng và xóa container/network, giữ volume dữ liệu | `docker compose down` |

Database được lưu trong named volume nên vẫn còn sau khi dừng stack. Không dùng
`docker compose down -v` để dừng thông thường vì tùy chọn `-v` xóa cả volume dữ liệu.

## 9. Lỗi thường gặp

### Docker không kết nối được daemon

Mở Docker Desktop, chờ engine sẵn sàng rồi chạy `docker info`. Trên Windows,
kiểm tra đang dùng Linux containers. Trên Linux, kiểm tra service Docker và quyền
truy cập Docker của tài khoản hiện tại.

### `ports are not available` hoặc `address already in use`

Đổi cổng trong `.env` như bước 3, cập nhật `DB_URL` nếu đổi `DB_PORT`, rồi chạy lại
`docker compose up -d --wait`. Nếu đang chạy backend trực tiếp trên máy, dừng nó
trước khi dùng lại cùng cổng cho Compose. Không cần tắt các ứng dụng khác không liên quan.

### Không tìm thấy `DB_USERNAME`, `DB_PASSWORD` hoặc kết nối database bị từ chối

Khi chạy backend trên máy, nạp `.env` theo bước 5. Kiểm tra database `healthy`,
`DB_URL` trỏ tới đúng cổng host và mật khẩu/tài khoản khớp với database đã khởi tạo.
Không dùng hostname `database` cho backend chạy ngoài Docker.

### Đổi mật khẩu trong `.env` nhưng vẫn báo authentication failed

Các biến `POSTGRES_*` chỉ khởi tạo tài khoản/database khi volume còn mới. Sửa `.env`
không tự đổi mật khẩu trong database đã tồn tại. Dùng lại thông tin khởi tạo hoặc
đổi mật khẩu bằng SQL với tài khoản có quyền, rồi cập nhật `.env` cho khớp.
Không xóa volume nếu cần giữ dữ liệu.

### Maven tải lỗi hoặc mirror trả file hỏng

Repo có cấu hình tùy chọn để dùng Maven Central trực tiếp và cache riêng, không sửa
Maven settings toàn máy. Nếu môi trường cho phép truy cập Maven Central, chạy:

```powershell
.\mvnw.cmd -s .mvn/settings-central.xml "-Dmaven.repo.local=.tools/m2" clean verify
```

Trên macOS/Linux:

```bash
./mvnw -s .mvn/settings-central.xml -Dmaven.repo.local=.tools/m2 clean verify
```

Có thể dùng cùng hai tùy chọn khi chạy backend, ví dụ trong PowerShell sau khi nạp `.env`:

```powershell
.\mvnw.cmd -s .mvn/settings-central.xml "-Dmaven.repo.local=.tools/m2" spring-boot:run "-Dspring-boot.run.profiles=local"
```

### IDE báo unresolved imports hoặc `Unresolved compilation problems`

Mở đúng thư mục repo, chọn JDK 21 và reload/import lại Maven project từ `pom.xml`.
Kiểm tra cấu hình Maven của IDE nếu terminal đang phải dùng mirror/cache thay thế.
Tránh để IDE và Maven cùng biên dịch vào `target`: tạm dừng automatic build của IDE
rồi chạy lại `clean verify` nếu IDE ghi đè class bằng compilation-error stubs.
Không bỏ qua test để che lỗi.

### Trang `/login` trên cổng backend trả lỗi

P01 chạy frontend dev ở cổng 5173, gọi backend qua Vite proxy. Mở
`http://127.0.0.1:5173/login`; không dùng cổng backend cho HTML. Nếu Swagger không có
E01/E02, kiểm tra đang chạy đúng build P01. Xem [P01 setup](P01_LOCAL.md).

## 10. Kiểm tra đã cài thành công

- Chạy Compose: `database` và `backend` đều `healthy`; hoặc database `healthy` và
  backend local có thông báo `Started GdrnApplication`.
- Health trả `UP` và Swagger mở được tại cổng đã cấu hình.
- `clean verify` thành công nếu chuẩn bị môi trường để phát triển/test.
- `.env` không được commit lên Git.

Đọc thêm [README](../README.md), [quy tắc phát triển](../AGENTS.md) và
[tổng quan kiến trúc](architecture/architecture-overview.md) trước khi bổ sung tính năng.
