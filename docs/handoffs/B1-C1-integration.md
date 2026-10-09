# Tích hợp B1/C1 vào kế hoạch duy nhất

Ngày 09/10/2026. Theo yêu cầu người dùng: hòa giải hai nhánh feature và merge main,
chạy lại với code thống nhất theo `docs/PROMPTS_PHA_1_4_NGUOI.md`.

## Nền và xử lý conflict

- Main `a195093`: kế hoạch duy nhất; C1 domain/persistence/API/UI đã có tới `7be3b09`.
- C1 `766cf1f`: thêm handoff và browser E2E. Merge main vào C1 tại `bacf960`, giữ
  kế hoạch duy nhất, không hồi sinh hai file kế hoạch đã xóa; main fast-forward tới đó.
- B1 `5ff284a`: adapters với schema/security test-only. Merge main vào B1 tại
  `7922c02`; giữ các tests Identity/probes/Disaster của main cùng tests Reporting.
  Không coi merge text là đã hoàn thành runtime B1.
- ADR Reporting đổi từ số006 sang007 vì main đã dùng006 cho readiness. Không đổi
  quyết định ADR006 đã merge. Tài liệu, links, ledger và progress đã đồng bộ.

## Code và migration tích hợp

- V4__reporting_reports.sql được cấp sau V3 Disaster. DB local trước nâng cấp có
  V1/V2; repo tích hợp có V1–V3. Chuyển schema nháp B1 sang Flyway thật, xóa bản nháp
  để tránh hướng dẫn chạy SQL thủ công. Không sửa checksum/DDL của V1–V3; không seed.
- Mở đúng E07 POST cho CITIZEN; E08/E09 GET cho CITIZEN/AUTHORITY. Ownership ở
  ReportService và SQL list; không mở PATCH/DELETE hoặc future API. Bỏ test-only
  Reporting security chain, test dùng production security và migrations.
- Bỏ Hidden controller; ghép reporting.json vào runtime Swagger. Chính xác 9
  operations trên 6 paths, Bearer/DTO/error contract P00; E08 không quảng cáo radius.
- Sửa list Disaster thành REPEATABLE READ theo contract count/items cùng snapshot.
  Tests interpose giữa các SELECT và commit insert từ thread/transaction khác,
  kiểm tra cả E03 và E08; không dùng sleep để giả race. Không mở rộng nghiệp vụ C1.
- Upgrade test V3→V4 giữ user/disaster hiện có, không seed Reporting, rerun no-op.
  Bootstrap V2→V3 giữ target3 để tiếp tục test đúng bước upgrade lịch sử.

## Kết quả thực tế

| Gate | Kết quả |
| --- | --- |
| `.\mvnw.cmd --no-transfer-progress clean verify` | BUILD SUCCESS, 01:20; 37 unit/architecture +25 integration =62; 0 failure/error/skipped |
| PostGIS/Testcontainers | Schema sạch V1–V4, V2→V3, V3→V4, preserving rows, mapping, ownership/roles/input/page/filter, concurrent snapshot PASS |
| Frontend `npm run typecheck`, `npm test`, `npm run build` | PASS, Node22.14.0; 15 tests/4 files; không đổi dependencies/lockfile |
| `scripts/local.ps1 -Action start` | Compose backend/database healthy; Vite `/login` hoạt động; build backend và upgrade V2→V3→V4 thành công |
| Account persistence | UUID/email/role/password hash fingerprint của 3 demo accounts không đổi trước/sau upgrade |
| Reporting local HTTP smoke | Login thật; create201/own detail200/cross-citizen404/authority detail200/authority create403/anonymous401; ownerId và radius400; DELETE chưa implement403; list không đọc chéo |
| `npm run test:login` với env demo local | Playwright4/4 PASS (9.8s): login/reload/logout cả 3 accounts; authority create/edit/resolve Disaster desktop/mobile qua API thật |
| Contracts/docs | P00 validator PASS: 15 operations/6 screens/140 examples; Reporting runtime JSON khớp doc slice, OpenAPI hợp lệ; không broken local links hoặc conflict markers |

Log local ignored: `.tools/merge-b1-c1-verify.log`, `merge-b1-c1-local.log`,
`merge-b1-c1-smoke.log`. Một số output stderr Docker/Node được PowerShell gắn nhãn
NativeCommandError khi redirect, nhưng process exit0 và các health/test checks PASS.
Không bỏ qua test hoặc lỗi thực tế để báo xanh. Chưa xác nhận GitHub Actions từ xa.

Smoke đã thêm một report PENDING mô phỏng và browser đã tạo/kết thúc một disaster
kiểm thử trong DB local qua API bình thường; không xóa dữ liệu cũ/volume, không đổi
`.env`. DB local bền vững tiếp tục chạy, không phải container test tạm.

## Thử tại máy hiện tại và scope còn lại

- Frontend: http://127.0.0.1:5173/login — tài khoản DEMO_* trong `.env`.
- Backend: http://127.0.0.1:18080; Swagger `/swagger-ui/index.html`.
- AUTHORITY có S05 `/operations/disasters`; CITIZEN có login và E07–E09 qua API.
  S02/S03 chưa làm vì thuộc B3; không thêm trang/mock để giả hoàn thành.
- B2 được mở khóa sau merge B1/C1: verification, withdrawal, radius, ReportingQuery.
  B3 được bắt đầu phần gửi/xem; hoàn tất B3 vẫn chờ B2/D1. Không bắt đầu các task đó
  trong lần hòa giải này. A1 audit toàn bộ và A2 production frontend vẫn còn theo plan.
- Giữ nguyên một kế hoạch; contracts/ADRs/handoffs ghi chi tiết/evidence, không cấp
  task mới hoặc chạy lại F01–F04. Đây chưa phải toàn bộ 15 API/6 UI hoàn tất.
