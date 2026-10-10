# Global Disaster Response Network (GDRN)

**Hệ thống Điều phối và Ứng phó Thảm họa Toàn cầu** — mô phỏng phục vụ môn Kiến trúc
phần mềm. Pha 1 dùng một Modular Monolith, DDD và Hexagonal/Clean Architecture,
một PostgreSQL/PostGIS và một React SPA. Đây không phải hệ thống cứu hộ vận hành thực tế.

## Bắt đầu

Cài Docker Desktop (Linux containers) và Node **22.14.0**. Chạy PowerShell tại root repo:

```powershell
.\scripts\local.ps1 -Action setup
.\scripts\local.ps1 -Action start
```

`setup` bổ sung cấu hình thiếu trong `.env`, giữ giá trị đã cấu hình. `start` chạy
backend/database bằng Compose và Vite trên host. Mở [Login](http://127.0.0.1:5173/login),
lấy tài khoản từ các cặp `DEMO_*_EMAIL/PASSWORD` trong `.env` cá nhân. Tài khoản và
BCrypt hash lưu thật trong database; seed chạy lại không reset password/role.
Không commit `.env`. Nếu volume DB đã tồn tại, phải dùng credentials gốc của volume.

```powershell
.\scripts\local.ps1 -Action status
.\scripts\local.ps1 -Action stop  # Giữ database volume
```

Backend dùng `APP_PORT`, DB dùng `DB_PORT` trong `.env` (mặc định 8080/5432).
Swagger ở `http://127.0.0.1:<APP_PORT>/swagger-ui/index.html`. Frontend port 5173
khác backend port; browser chỉ gọi `/api` qua Vite proxy. JWT nằm trong memory,
reload/new tab cần login lại. Frontend production container/proxy thuộc A2, chưa có.

Hướng dẫn đầy đủ: [cài đặt/chạy/debug](docs/HUONG_DAN_CAI_DAT_VA_CHAY.md) và
[env, auth, seed, frontend local](docs/P01_LOCAL.md). `scripts/dev.ps1` chỉ quản lý
backend/database và smoke/verify; dùng `local.ps1` khi muốn chạy thêm frontend.

## Runtime hiện có

Đối chiếu main `78f164b`, ngày 10/10/2026. Trạng thái merge và evidence tiếp tục được
cập nhật tại [progress](docs/phase1-progress.md), không suy ra từ việc prompt đã tồn tại.

| Phần | Đã triển khai | Còn lại theo catalog |
| --- | --- | --- |
| Identity / P01 | E01 login, E02 me; JWT/BCrypt/demo seed; CITIZEN/AUTHORITY | Audit A1 khi các API tiếp theo merge |
| Disaster / C1 | E03–E06; ACTIVE→RESOLVED; DisasterQuery; S05 `/operations/disasters` cho AUTHORITY | UI01-C1 đồng bộ giao diện; C2 triển khai S04 |
| Reporting / B1 | E07–E09; PENDING, ownership, PostGIS geography, filter/page/sort | B2 verify/withdraw/radius/ReportingQuery; B3 S02/S03 |
| Shared UI / UI01-A | Crisis Command tokens/layout/primitives và S01 `/login`, API thật | UI01-B audit đủ 6 màn hình sau các owner |
| Rescue | Chưa có runtime nghiệp vụ | D1 E12–E15 và D2 S06 |

Swagger runtime công bố **E01–E09**. P00 mô tả baseline đích **15 operations/6 screens**;
E10–E15 chưa mở, S02/S03/S04/S06 chưa có. S01 hiện hiển thị tài khoản sau login vì
route đích của B3/C2 chưa đăng ký; Authority mở S05 bằng nav Thảm họa.

Flyway đã có **V1–V4**: PostGIS extension, Identity, Disaster, Reporting. Migration
không chứa credentials/demo accounts; Hibernate validate schema. B2/D1 phải nhận
version mới từ [ledger](docs/architecture/phase1-module-contracts.md), không sửa V1–V4.

## Nguồn tài liệu và cách phát triển

| Nhu cầu | Đọc ở đâu |
| --- | --- |
| Chọn task, owner, dependency, acceptance | [Kế hoạch duy nhất: 16 task lõi + 3 checkpoint UI](docs/PROMPTS_PHA_1_4_NGUOI.md) |
| Tình trạng hiện tại và bằng chứng merge | [Progress](docs/phase1-progress.md), [handoffs](docs/handoffs/) |
| HTTP/API/role/race | [P00 contract](docs/api/phase1-contract.md), [OpenAPI](docs/api/phase1-contract.yaml) |
| Published interfaces và cấp migration | [Module contracts/ledger](docs/architecture/phase1-module-contracts.md) |
| Kiến trúc và quyết định | [Architecture](docs/architecture/architecture-overview.md), [ADRs](docs/adr/) |
| UI chung và cách dùng components | [Design system](docs/design/GDRN_UI_DESIGN_SYSTEM.md), [UI01-A](docs/handoffs/UI01-A.md) |
| Quy tắc phát triển | [AGENTS.md](AGENTS.md) |
| Tầm nhìn dài hạn/backlog | [Project context](GDRN_CODEX_PROJECT_CONTEXT.md) |

Dependency đi vào domain/application; domain plain Java, không JPA/Spring/HTTP.
Không truy cập infrastructure/table của module khác. Chiều published query mục tiêu
là Rescue → Reporting → Disaster; E09 không gọi Rescue, UI tiến độ dùng E09 +E14.
ReportingQuery/Rescue còn chờ B2/D1. Shared chỉ dành cho technical concerns nhỏ có nhu cầu.

Resource/Alert, map/risk, user administration, refresh/register và event bus ngoài
scope MVP. Không đưa microservices, broker, Redis hoặc hạ tầng Pha2 vào Pha1;
Pha2 cần benchmark, vấn đề đo được và ADR. Backlog sản phẩm không tự động là Pha2.

## Stack và kiểm chứng

Java 21 / Spring Boot 3.5.16 / Maven Wrapper 3.9.9, PostgreSQL 16/PostGIS 3.5, Flyway,
JPA cho Identity/Disaster và JDBC data mapper cho Reporting. React/TypeScript/Vite
dùng phiên bản pin trong `frontend/package.json` và lockfile; Node pin tại `.nvmrc`.

```powershell
# Root; cần JDK21 và Docker, Testcontainers dùng DB riêng, không cần .env
.\mvnw.cmd clean verify
docker compose --env-file .env.example config --quiet

# Frontend
cd frontend
npm.cmd ci
npm.cmd run typecheck
npm.cmd test
npm.cmd run build
```

`mvnw test` chỉ unit/architecture, không thay thế full verify. Browser thật dùng
`npm.cmd run test:login` sau khi backend/Vite đã chạy và nạp DEMO_* của cùng DB vào
process env; xem [hướng dẫn checks](docs/P01_LOCAL.md#checks). Suite hiện bao gồm
login, S05 và UI01; không phải toàn bộ luồng X2. C1 E2E tạo một disaster test thật
và kết thúc nó trong DB local, không xóa dữ liệu qua SQL để làm test xanh.

CI hiện chạy backend verify/Compose smoke và frontend typecheck/test/build;
chưa có full browser E2E job (A2/X2). Test counts và giới hạn môi trường được ghi
theo từng lần chạy trong handoff, không dùng con số cũ làm bằng chứng cho commit mới.
Khi Maven mirror/IDE gây lỗi, xem phần xử lý trong [runbook](docs/HUONG_DAN_CAI_DAT_VA_CHAY.md).
