# Handoff UI01-A — GDRN Crisis Command / shared UI và S01

Ngày 10/10/2026. Owner: Người 1. Nền `main` / `c850103` (P00/P01/B1/C1 đã merge).
**DONE trong working tree, chưa commit/review/merge.** Không chạy UI01-C1/UI01-B.
Không thay API, domain, migration, dependency, auth client hay convention đăng ký route.

Nguồn yêu cầu: [kế hoạch duy nhất](../PROMPTS_PHA_1_4_NGUOI.md),
[design system](../design/GDRN_UI_DESIGN_SYSTEM.md), [P00](../api/phase1-contract.md),
[P01](P01.md), [C1](C1.md). Design system là quy chuẩn UI, không phải kiến trúc mới.

## Baseline và audit trước sửa

`git status`, `git diff`, lịch sử và source thực tế xác nhận E01–E09/S01/S05 đã có.
Trước task đã có thay đổi chưa commit trong README, catalog prompt, progress và
`docs/design/` chưa được track. Đã giữ các thay đổi đó; task chỉ cập nhật thêm trạng thái
UI01-A trong progress. Khi review cần phân biệt phần tài liệu có sẵn với diff UI01-A.

| Thành phần hiện có | Tái sử dụng / vấn đề audit / xử lý |
| --- | --- |
| `App`, `RoleGuard`, `featureRoutes`, `landingPath` | Giữ phân quyền/route; thêm class shell theo role, nav active có `aria-current`, skip link tới main có thể nhận focus |
| `LoginPage`, `login`, `session`, `api` | Giữ E01 → E02, JWT memory-only; Login cũ chỉ có card đơn, nay chia nhận diện/form desktop và xếp dọc mobile |
| `Field` | Giữ signature cũ; thêm hint và ghép `aria-describedby` bên ngoài + hint + error, không đánh mất mô tả |
| `ErrorMessage`, `Loading`, `EmptyState` | Giữ tên export/children cho C1; thêm tokens, error accent, loading indicator giảm chuyển động, empty container |
| Global stylesheet | Màu xanh cũ hardcode, chưa có thang spacing/typography; selector header/nav/footer quá rộng. Thay bằng tokens và selector shell `.app-*` |
| S05 list/detail/form/confirmation | Đã có chức năng thật và responsive riêng, nhưng còn nhiều mã màu cũ, badge chữ nhỏ, controls chưa đồng nhất. Không sửa; đề xuất cho owner ở dưới |

Phân cấp mới: brand → thông điệp nhận diện → tiêu đề form → labels → CTA → thông tin
phiên. Họa tiết nền tự tạo, không phải bản đồ/live data. Mobile rút gọn phần mô tả
hero, giữ form một cột; không thêm dashboard, KPI hoặc nút chưa có chức năng.

## Files UI01-A

- `frontend/src/styles.css`: semantic palette/foreground-background, spacing, type,
  radius, responsive shell, form/state, Login và reduced motion.
- `frontend/src/shared/topography.svg`: contour SVG tự tạo, dùng nội bộ bundle,
  wrapper `aria-hidden`, không nhận pointer events; không CDN/font ngoài.
- `frontend/src/shared/components.tsx`: `PageHeader`, nâng cấp các primitive có consumer.
- `frontend/src/app/App.tsx`: shared header/nav/footer, role styling, skip focus.
- `frontend/src/auth/LoginPage.tsx`: hero/form/account presentation; focus field sai,
  live status trong lúc xác thực. Validation limits/request/auth logic giữ nguyên.
- `frontend/src/auth/LoginPage.test.tsx`, `frontend/src/shared/components.test.tsx`:
  hint/error semantics, pending/network error, duplicate submit, role nav/logout.
- `frontend/e2e/ui01-login.spec.ts`: layout/keyboard/reflow + real citizen route guard.
- Handoff này và `docs/phase1-progress.md`.

Không thay file trong `frontend/src/features/disaster/**`, E2E C1, backend,
`feature-routes.ts`, `session.ts`, `login.ts`, `api.ts`, package/lockfile.
S05 nhận typography/controls/shell chung qua cascade như trước; theme riêng S05 vẫn
còn màu xanh và được chuyển tiếp cho UI01-C1, không tuyên bố S05 đã redesign.

## Sử dụng shared cho B3/C2/D2

Theme được import sẵn tại frontend entry. Dùng `var(--gdrn-ink/surface/canvas/border/muted)`
cho chữ/nền; `--gdrn-action/on-action`, `--gdrn-error-text/error-surface`,
`--gdrn-link`, `--gdrn-input-border`, `--gdrn-focus` cho tương tác. Orange CTA đi với
navy text; không tự dùng white trên orange. Ocean/critical/success/warning là màu
gốc, cần kiểm tra cặp foreground/background khi owner tạo badge theo enum P00.

Spacing `--gdrn-space-1/2/3/4/6/8/12` = 4/8/12/16/24/32/48px ở root16px;
type `--gdrn-text-xs/sm/base/lg/xl/2xl` = 12/14/16/20/28/36px. System font không tải
CDN. Nội dung line-height1.6; radius control/card/panel =8/12/16px.
Shell max1200px; mobile dưới768px, Login giảm padding dưới1024px. Dùng grid
`minmax(0,1fr)`, min-width0 và wrap ID/text dài; tránh chiều rộng cố định.

```tsx
import { PageHeader, Field, ErrorMessage, Loading, EmptyState } from '../../shared/components'

// Một h1 cho mỗi page; action chỉ truyền khi role và API cho phép.
<PageHeader eyebrow="BÁO CÁO" title="Gửi báo cáo" description="Thông tin sự cố." />
<Field id="latitude" label="Vĩ độ" type="number" hint="Từ -90 đến 90."
  value={latitude} onChange={onLatitudeChange} error={errors.latitude} />
<Loading>Đang tải báo cáo…</Loading>
<ErrorMessage>{errorMessage}</ErrorMessage>
<EmptyState>Chưa có báo cáo phù hợp với bộ lọc.</EmptyState>
```

Ví dụ là cách dùng component, không phải Reporting runtime mới. Render các state
theo request thật, không cùng lúc. `ErrorMessage` có role alert, `Loading` có role
status; không lồng thêm vùng live gây đọc hai lần. `Field` hỗ trợ native input props,
`hint`, `error`, mô tả bên ngoài qua `aria-describedby`; select/textarea chưa có
wrapper chung. Không đổi tên export khiến C1 phải sửa ngay.

`StatusBadge`, `ConfirmDialog`, FilterBar/Pagination chưa đưa vào shared vì S01
không có consumer; C1 đang sở hữu badge/confirmation thực. Bổ sung nhỏ với owner
khi có consumer, không tạo bộ library dự phòng. Status phải có chữ theo P00;
không dùng role hoặc trạng thái kết nối giả làm severity.

Auth/route integration giữ P01: feature export `routes` từ
`frontend/src/features/<feature>/routes.tsx`; không sửa global router. Shell chỉ
hiện các route role được phép. Chưa có S02/S04 nên landing vẫn là card tài khoản
tại `/login`; Authority dùng link Thảm họa trên nav. JWT memory-only, reload login
lại; logout/401/expiry clear session + generation. Cache ngoài component phải đăng ký
`session.onClear`. 403 giữ phiên và hiện lỗi, không coi UI guard là security backend.

## Kiểm chứng thực tế

Môi trường: Windows, Node22.14.0, Chromium Playwright có sẵn, Docker PostGIS.
Maven host dùng JDK23 (compiler release21); backend Docker dùng image Temurin21.
Chưa chạy Maven host bằng JDK21 trên máy này; không gọi JDK23 là JDK21.
Backend/API local `127.0.0.1:18080`, Vite `127.0.0.1:5173`; credentials lấy từ `.env`,
không ghi vào test/report/screenshot. DB dùng named volume hiện có.

| Lệnh / kiểm tra | Output thực tế |
| --- | --- |
| `npm.cmd run typecheck` tại frontend | `tsc --noEmit`, exit0 |
| `npm.cmd test` | 5 test files, **17 passed**, non-watch, 57.29s |
| `npm.cmd run build` | Vite8.3.4, 37 modules, **built in 2.02s**, exit0 |
| `.\mvnw.cmd --no-transfer-progress clean verify` | Chạy trực tiếp **BUILD SUCCESS / exit0**, 37 unit/architecture +25 integration = **62**, failures0/errors0/skipped0, 1:05 |
| `.\scripts\local.ps1 -Action start` | Backend/database healthy, npm ci115 packages, frontend ready |
| `npm.cmd run test:login` với DEMO_* trong process env | **8 passed (14.5s)**, Chromium → API → PostGIS thật |
| `git diff --check` | exit0; chỉ cảnh báo LF→CRLF của Git |
| Diff phạm vi bảo vệ | Không đổi Disaster/client/session/route catalog/lockfile/backend |

Browser suite giữ 4 tests cũ: ba account đăng nhập sai/đúng, `/me`, reload/logout,
storage trống; C1 tạo/sửa/resolve và responsive. Thêm 4 tests UI01: 360/768/1280px,
skip link, Tab/Enter, focus-visible, error gắn field, text enlargement200%, không
tràn ngang; Citizen không thấy nav Disaster, truy cập SPA route bị từ chối và logout
đưa về login. C1 test tạo **một disaster kiểm thử thật đã RESOLVED** trong DB local;
không phải seed/mẫu giả trong frontend và không xóa trực tiếp dữ liệu nghiệp vụ.

Kiểm tra riêng bằng Chromium: giữ chậm request E01 thật để quan sát loading, sau đó
cho API trả401 với credentials cố ý sai; error hiển thị, password được xóa. Không
mock response nghiệp vụ. Reduced motion cho button transition0s. Client tests có
401 clear/callback, 403 giữ session, late-response/expiry; backend tests kiểm tra JWT.
Không tuyên bố browser đã kiểm tra đủ lỗi của các màn hình chưa triển khai.

Đã mở và rà ảnh S01 idle360/768/1280, validation, loading/error360, CSS zoom200%
ở1280 và S05 mobile. Artifacts local (ignored, tái tạo khi chạy tests):
`frontend/test-results/ui01-login-{360,768,1280}.png`,
`ui01-login-*-validation.png`, `c1-disaster-{desktop,mobile}.png`;
`.tools/ui01-{loading-360,error-360,zoom200}.png`, `.tools/ui01-verify.log`.
Script kiểm tra bổ sung local: `.tools/ui01-visual-audit.cjs`.

Tương phản từ tokens runtime, tính relative luminance sRGB:

| Cặp | Ratio |
| --- | --- |
| Ink / white | 15.72:1 |
| Muted / canvas, white | 5.62:1 /6.05:1 |
| Link hoặc focus / white | 6.59:1 |
| Ink / orange CTA | 5.64:1 |
| Error text / error surface | 6.41:1 |
| Input border / white | 3.86:1 |
| Muted-on-dark / ink | 10.39:1 |
| Orange focus / ink | 5.64:1 |

Các cặp trên đạt ngưỡng chữ4.5, control3; đây không phải chứng nhận WCAG cho toàn app.
Hero có motif nhạt12%; ảnh đã được rà không che chữ. **NOT RUN:** screen reader thật,
native toolbar zoom200% và Firefox/Safari. Đã chạy text enlargement200% và CSS
zoom200% desktop, không đánh đồng với native zoom. CSS zoom200% tại360px tương
đương180 CSSpx gây tràn dưới min-width320px; không phải viewport mục tiêu thông thường.
Checklist review thủ công còn lại: Ctrl+Plus tới200% trên desktop, Tab/Shift+Tab toàn
form/nav, đọc label/error/loading với NVDA, xác nhận không mất CTA/nội dung.

Lần start đầu khi redirect `*> log` gặp PowerShell NativeCommandError vì Docker in
progress trên stderr; đã chạy lại script trực tiếp thành công. Lần verify redirect
cũng trả exit1 dù log Maven BUILD SUCCESS; đã chạy lại toàn bộ clean verify trực tiếp,
xác nhận exit0 và XML reports đủ62 tests, không skip. Log `.tools/ui01-verify.log`
lưu lần đầu; kết quả trực tiếp cuối cùng ở terminal và `target/*-reports/`.
Không sửa script
ngoài phạm vi. Không còn blocker build/API/browser của UI01-A.

## Đề xuất patch/checklist S05 cho Người 3 (chưa áp dụng)

- Thay header page bằng `PageHeader` với CTA tạo hiện có; giữ route/E03–E06.
- Chuyển hex trong `disaster.css` sang tokens: surface/border/muted/input-border;
  giữ selector `.disaster-*` riêng, tránh override toàn app.
- Ánh xạ badge ACTIVE/RESOLVED và LOW/MODERATE/HIGH/CRITICAL từ enum thật. Giữ nhãn
  chữ; nâng chữ nhỏ từ .72rem, kiểm tra contrast của từng nền/foreground trước dùng.
- Rà select/textarea labels, help/error descriptions, selected row bằng semantics
  ngoài màu; kiểm tra keyboard chọn bản ghi và filter/pagination44px khi khả thi.
- Confirmation resolve đang thuộc C1: kiểm tra nhãn, focus vào/ra, Escape và thao tác
  không thể đảo ngược; chỉ tách shared dialog khi thống nhất consumer/API component.
- Kiểm tra list-detail 360/768/1280, ID/tên dài, empty/loading/request error,
  401/403/404/409; không đổi payload/version/resolve semantics để khớp hình thức.
- Rerun C1 unit/E2E thật sau patch; shared shell đã đổi màu nhưng S05-specific theme
  chưa đồng nhất, đây là scope UI01-C1, không phải lỗi business mới.

Sau review/merge UI01-A, có thể mở UI01-C1 và phần B3 dùng E07–E09. C2/D2 vẫn phải
đợi B2/D1 tương ứng theo catalog. UI01-B chưa được mở khóa; không chạy task tiếp theo.
