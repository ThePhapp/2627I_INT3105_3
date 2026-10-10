# PHỤ LỤC PROMPT UI01 SAU P00, P01, C1, B1

**Ngôn ngữ:** Tiếng Việt. **Phạm vi:** cải tạo frontend theo
`docs/design/GDRN_UI_DESIGN_SYSTEM.md`.

File này là **phụ lục prompt thực thi** được catalog chính tại
`docs/PROMPTS_PHA_1_4_NGUOI.md` viện dẫn. Nó không phải kế hoạch/task catalog thứ hai,
không thay đổi 15 endpoint, 6 màn hình, owner hoặc dependency nghiệp vụ đã chốt.

**Điều kiện bắt đầu:** P00, P01, C1 và B1 đã merge vào nhánh tích hợp (nếu mới hoàn thành trên nhánh riêng, phải review/merge trước). Đặt file design system vào đúng `docs/design/GDRN_UI_DESIGN_SYSTEM.md` và commit để mọi nhánh cùng đọc được.

**Cách dùng:** kiểm tra UI task tương ứng trong catalog chính rồi copy nguyên một khối
prompt vào coding agent trên nhánh của owner. Đọc diff/tests và review trước khi merge;
không chạy các khối liên tục trong cùng một phiên. UI01-A xong và merge rồi mới chạy
UI01-C1 hoặc triển khai UI mới. UI01-B chỉ làm khi đủ 6 màn hình và là gate trước X1.

## UI01-A — Người 1: xây design system dùng chung và cải tạo S01 Login

```text
Thực hiện task UI01-A cho GDRN trên code hiện tại, sau khi P00, P01, C1, B1 đã merge.
Đọc toàn bộ AGENTS.md áp dụng, kế hoạch và mục Quy tắc chung tại
docs/PROMPTS_PHA_1_4_NGUOI.md, hợp đồng P00,
docs/design/GDRN_UI_DESIGN_SYSTEM.md, handoff P01/C1 và code thật.
Kiểm tra git status, git diff, commit nền, cấu trúc frontend/router/components trước khi sửa.

Mục tiêu: xây dựng nhận diện GDRN Crisis Command trên nền React hiện có mà không
regenerate frontend, không đổi API/auth/business logic và không làm màn hình ngoài
phạm vi task. Chỉ sở hữu shared frontend theme/layout/primitives và S01 Login.

1. Audit giao diện hiện tại, ghi danh sách component tái sử dụng, điểm yếu về
   phân cấp thông tin, accessibility, responsive và chỗ cần phối hợp với C1.
2. Thêm semantic CSS tokens, typography, spacing, responsive conventions,
   họa tiết đường đồng mức địa hình nhẹ do dự án tự tạo. Không tạo dependency
   CDN bắt buộc; không đổi API client, router conventions hay framework.
3. Nâng cấp shared components thực sự cần, ưu tiên PageHeader, StatusBadge,
   EmptyState, ErrorState, LoadingState, form controls, ConfirmDialog nếu có.
   Không dựng thư viện UI thừa hoặc sửa feature-owned CSS hàng loạt.
4. Cải tạo S01 /login: nhận diện GDRN rõ ràng, hero desktop và bố cục mobile,
   form dễ đọc, trạng thái loading/error, keyboard/label/focus, đăng nhập API thật.
   Giữ JWT in-memory, /me, logout và hành vi reload như P01.
5. Giữ nguyên feature Disaster S05 thuộc người 3. Tạo checklist/đề xuất patch
   cho người 3 thay vì tự sửa C1-owned files.
6. Kiểm tra nav theo role, 401/403, clear state khi logout; không lưu token
   localStorage/sessionStorage, không sinh dữ liệu demo giả hoặc route mới.
7. Chạy frontend typecheck, test chế độ không watch và build; backend verify
   theo AGENTS.md. Thử browser thật nếu có công cụ; nếu không ghi NOT RUN kèm
   checklist thao tác, tuyệt đối không báo PASS khi chưa kiểm tra.
8. Ghi docs/handoffs/UI01-A.md: files, thay đổi hình thức, lệnh và output test,
   regression, blocker, hướng dẫn dùng tokens/components cho B3/C2/D2 và đề
   xuất dành cho S05. Cập nhật progress phù hợp.

Tiêu chí hoàn tất: S01 vẫn hoạt động với API thật; shared design system dùng được
cho các feature; không regression auth/route; không sửa code owner khác.
Không tự push, merge, deploy hoặc chạy UI01-C1/UI01-B.
```

## UI01-C1 — Người 3: cải tạo S05 Disaster đã triển khai

```text
Thực hiện UI01-C1 sau khi UI01-A đã merge vào nhánh tích hợp.
Đọc AGENTS.md, kế hoạch MVP, Quy tắc chung, P00 contracts,
docs/design/GDRN_UI_DESIGN_SYSTEM.md, handoff C1/UI01-A và code thật.
Kiểm tra git status/diff và route export S05 hiện tại.

Chỉ sở hữu frontend/src/features/disaster cùng tests liên quan. Cải tạo S05 theo
phong cách Authority Operational Workbench: tiêu đề nghiệp vụ, bộ lọc/danh sách,
StatusBadge, panel chi tiết, biểu mẫu tạo/sửa/resolve, confirmation rõ ràng,
loading/empty/error, responsive ở 360/768/1280px, keyboard và focus.

Tái sử dụng shared components và tokens từ UI01-A; không nhân bản CSS/client.
Giữ nguyên E03–E06, route thực tế, role permissions, trạng thái ACTIVE → RESOLVED,
request/response, dữ liệu API thật và error semantics. Không thêm map, dashboard,
API, dữ liệu giả hoặc module mới. Không sửa shared router/theme nếu chưa phối hợp
người 1. Không thay backend, security, migration, OpenAPI.

Chạy frontend typecheck, test không watch, build và backend verify theo AGENTS.md.
Thử UI trên browser thật nếu có công cụ; nếu không ghi NOT RUN và checklist thủ công.
Ghi docs/handoffs/UI01-C1.md gồm file sửa, test/output thật, screenshot nếu có,
kiểm tra bảo toàn nghiệp vụ và blockers. Cập nhật tiến độ. Không tự merge/push.
```

## Hướng dẫn bổ sung cho B3, C2, D2

```text
Khi thực hiện task B3/C2/D2 theo prompt trong catalog chính, đọc thêm
`docs/design/GDRN_UI_DESIGN_SYSTEM.md` và `docs/handoffs/UI01-A.md`.
Xây màn hình mới theo đúng design tokens, shared components và phân biệt
Citizen/Authority của GDRN Crisis Command. Giữ nguyên API, role, route,
file ownership, acceptance và dependency của prompt gốc.
Không thêm map/dashboard/số liệu giả chỉ để tăng độ đẹp. Không tự sửa theme,
router hoặc feature người khác. Nếu phát hiện thiếu shared component, gửi
đề xuất cụ thể cho người 1 và ghi blocker/handoff. Test giao diện và API thật.
```

## UI01-B — Người 1 điều phối: kiểm tra đồng nhất cả 6 màn hình

```text
Thực hiện UI01-B chỉ sau khi S01–S06, UI01-C1 và các task UI có liên quan đã merge.
Đọc AGENTS.md, P00, Quy tắc chung, docs/design/GDRN_UI_DESIGN_SYSTEM.md,
handoffs UI01-A/UI01-C1 và handoffs của B3/C2/D2. Kiểm tra code thật.

Audit tất cả 6 màn hình về màu, typography, spacing, bố cục Citizen/Authority,
responsive 360/768/1280px, focus/keyboard, mục tiêu WCAG 2.2 AA,
loading/empty/error/401/403/404/409, xác nhận thao tác và luồng API thật.
Không thêm tính năng, không đổi workflow/role/API/route. Chỉ sửa phần shared UI
thuộc người 1; issue thuộc feature phải bàn giao patch cho owner và retest sau merge.

Chạy frontend typecheck/test không watch/build, backend verify theo AGENTS.md và
browser smoke nếu có công cụ. Ghi PASS/FAIL/NOT RUN theo bằng chứng thật, không
đánh dấu PASS cho bước chưa chạy. Ghi docs/handoffs/UI01-B.md, danh sách các
màn hình đã kiểm tra, commands/output, screenshots nếu có, blockers và điểm còn lại.
Không tự push/merge/deploy hoặc bắt đầu Pha 2.
```

## Mẫu tiếp tục task UI đang dở

```text
Tiếp tục UI01-<ID> từ code và nhánh hiện tại. Đọc AGENTS.md,
docs/design/GDRN_UI_DESIGN_SYSTEM.md, đúng prompt UI01-<ID> và
handoff docs/handoffs/UI01-<ID>.md nếu có. Kiểm tra git diff và dependency
đã merge. Không regenerate frontend, không ghi đè thay đổi chưa commit,
không chạy sang task khác. Hoàn thiện các tiêu chí chưa đạt, chạy test thật,
cập nhật handoff và báo rõ blocker.
```
