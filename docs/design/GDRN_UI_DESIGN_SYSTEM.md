# HỆ THỐNG THIẾT KẾ GIAO DIỆN GDRN — CRISIS COMMAND

**Phiên bản:** 1.1  
**Trạng thái:** Đã review và được đưa vào quy trình Pha 1 ngày 10/10/2026  
**Phạm vi:** Pha 1 MVP, chỉ 15 endpoint E01–E15 và 6 màn hình S01–S06  
**Vị trí trong repo:** `docs/design/GDRN_UI_DESIGN_SYSTEM.md`

> Tài liệu này quy định **giao diện và trải nghiệm người dùng**, không thay thế
> `AGENTS.md`, kế hoạch duy nhất tại `docs/PROMPTS_PHA_1_4_NGUOI.md`, hợp đồng P00
> hay OpenAPI. Khi có xung đột về nghiệp vụ, API, quyền hoặc route, kế hoạch, hợp đồng
> và code đã merge có ưu tiên cao hơn. Không đổi chức năng chỉ để khớp bản thiết kế.

## 1. Định hướng sản phẩm và bản sắc

**Tên phong cách:** GDRN Crisis Command — hệ thống quản lý sự cố và điều phối cứu hộ đáng tin cậy, rõ ràng, điềm tĩnh khi xử lý tình huống khẩn cấp.

**Không thiết kế theo mẫu SaaS/dashboard đại trà:** tránh sidebar và hàng loạt ô KPI trang trí, gradient vô nghĩa, glassmorphism tràn lan. Khác biệt của GDRN phải đến từ hệ phân cấp thông tin, ngữ nghĩa trạng thái, đường nét địa hình và quy trình hành động thực tế.

**Hai nhóm người dùng, một nhận diện thống nhất:**
- **CITIZEN (người dân):** nền sáng, dễ đọc, ưu tiên điện thoại, biểu mẫu ít áp lực nhận thức, một hành động chính rõ ràng.
- **AUTHORITY (cơ quan chức năng):** giao diện điều hành có mật độ thông tin cao hơn; điều hướng xanh navy; bố cục danh sách–chi tiết–thao tác để xử lý nhiều báo cáo/nhiệm vụ.

**Ngoài phạm vi Pha 1:** bản đồ tương tác, biểu đồ, dashboard số liệu, realtime feed, thông báo, upload, đăng nhập mạng xã hội, API/route mới, phân công tự động, tính năng AI. Không dùng dữ liệu giả trong runtime để làm đẹp giao diện. Hình minh họa chỉ mang tính nhận diện, không ngụ ý tính năng đã tồn tại.

## 2. Nguyên tắc thiết kế

1. **Rõ ràng trong tình huống khẩn cấp:** mỗi khu vực có một hành động chính; không truyền đạt mức độ nghiêm trọng chỉ bằng màu.
2. **Có thể kiểm chứng:** hiển thị mã định danh, thời gian, trạng thái và hành động được phép theo dữ liệu API thật; không tạo chỉ số hoặc mức độ khẩn cấp giả.
3. **Khác biệt nhưng tiết chế:** họa tiết đường đồng mức địa hình, typography sắc nét, khoảng trắng có chủ đích, thanh nhấn trạng thái mảnh, nhóm thông tin đánh số.
4. **Truy cập được:** keyboard, focus dễ thấy, tương phản, nhãn input, thông báo lỗi và confirmation dễ hiểu.
5. **Triển khai tăng dần:** tái sử dụng React/Vite, router và API client đã có từ P01; không thay framework hoặc làm lại frontend.

## 3. Design tokens

Khai báo biến CSS ngữ nghĩa trong shared theme. Không rải mã màu hex trực tiếp ở từng feature, trừ trường hợp có lý do rõ ràng.

| Token | Giá trị nền sáng | Mục đích |
|---|---|---|
| `--gdrn-ink` | `#10243A` | Tiêu đề, chữ chính, điều hướng |
| `--gdrn-ocean` | `#247BA0` | Liên kết, hành động thông tin |
| `--gdrn-rescue` | `#F47735` | Điểm nhấn cứu hộ/CTA quan trọng, không dùng cho tất cả nút |
| `--gdrn-critical` | `#C83F45` | Cảnh báo nghiêm trọng, lỗi, thao tác xóa |
| `--gdrn-success` | `#207A59` | Thành công, xác minh hoàn tất, nhiệm vụ hoàn tất |
| `--gdrn-warning` | `#A45B12` | Chờ xử lý, cần chú ý |
| `--gdrn-canvas` | `#F4F7FA` | Nền trang |
| `--gdrn-surface` | `#FFFFFF` | Thẻ thông tin, biểu mẫu, panel |
| `--gdrn-border` | `#D6E0E8` | Đường phân cách, viền |
| `--gdrn-muted` | `#506578` | Chữ phụ |

Các màu trên là **giá trị khởi đầu**, không mặc định mọi tổ hợp chữ/nền đều đạt WCAG. Agent phải kiểm tra tương phản thật, bổ sung cặp foreground/background cho badge, button và các trạng thái.

**Trạng thái:** chỉ ánh xạ từ enum thực tế trong P00 và API. Không tự tạo enum mức độ thiên tai hay trạng thái mission mới. Mỗi trạng thái phải có **nhãn chữ + màu + biểu tượng khi cần**.

**Typography:** ưu tiên `Be Vietnam Pro` nếu đã có font hợp pháp hoặc được đóng gói đúng cách; fallback `system-ui, sans-serif`. Không bắt buộc dùng CDN font bên ngoài. Thang cỡ chữ: `12 / 14 / 16 / 20 / 28 / 36px`. Nội dung Citizen ưu tiên 16px; Authority 14–16px. `line-height` nội dung ít nhất 1.45. Số, thời gian và ID dài nên có căn chỉnh/đứt dòng phù hợp.

**Khoảng cách:** lấy 4px làm đơn vị, dùng `4 / 8 / 12 / 16 / 24 / 32 / 48px`.  
**Bo góc:** input 8px, card 12px, panel nổi bật 16px; không bo tròn quá mức.  
**Đổ bóng:** nhẹ, ưu tiên viền phân tách.  
**Chuyển động:** 120–200ms cho tương tác có ý nghĩa; hỗ trợ `prefers-reduced-motion`.

## 4. Nhận diện thị giác và bố cục

### 4.1. Đường đồng mức địa hình (topographic motif)

Dùng họa tiết SVG/CSS tự tạo dạng đường đồng mức rất nhạt ở hero đăng nhập, tiêu đề khu vực hoặc màn hình trống. Họa tiết phải ở nền, không che chữ, không can thiệp thao tác. Không tải hoặc dùng tài nguyên bên ngoài khi chưa kiểm tra quyền sử dụng.

### 4.2. Thanh nhấn trạng thái

Card báo cáo/sự kiện/nhiệm vụ có thể dùng thanh viền trái 3–4px theo trạng thái. Luôn đi cùng nhãn và/hoặc icon; không phụ thuộc riêng màu để phân biệt.

### 4.3. Header nghiệp vụ

Một khối header nhất quán gồm tiêu đề, mô tả ngắn, breadcrumb khi thực sự cần và một hành động chính nếu role được phép. Không tạo nút thao tác mà API chưa hỗ trợ.

### 4.4. Workbench cho Authority

Trên desktop: vùng lọc/danh sách và panel chi tiết/hành động. Trên mobile: chuyển thành các vùng xếp dọc, tránh panel có chiều rộng cố định khiến tràn ngang. Tối ưu thao tác kiểm tra–quyết định thay vì biểu đồ trang trí.

### 4.5. Luồng thao tác Citizen

Biểu mẫu rộng khoảng 640–760px trên desktop; nhóm trường rõ ràng, label dễ hiểu, thông báo sau submit có thể nhận thấy, tránh mất dữ liệu nhập khi báo lỗi.

### 4.6. Trạng thái trống

Giải thích vì sao chưa có dữ liệu và đưa ra hành động tiếp theo **hợp lệ** theo quyền. Không seed báo cáo giả vào giao diện.

## 5. Shared components cần ưu tiên

Tận dụng các component đã có từ P01 trước khi tạo mới. Chỉ xây component thực sự được dùng:

- `PageHeader`, `SectionHeader`: tiêu đề, mô tả, hành động được phép.
- `StatusBadge`: nhãn trạng thái chính xác theo contract, màu và biểu tượng ngữ nghĩa.
- `OperationalCard`: card nghiệp vụ có thanh nhấn trạng thái khi phù hợp.
- `EmptyState`, `ErrorState`, `LoadingState`: trạng thái thống nhất.
- `ConfirmDialog`: dùng cho hành động không thể đảo ngược hoặc destructive theo prompt nghiệp vụ.
- `FormField`, `Pagination`, `FilterBar`, `DetailPanel`: bổ sung nếu màn hình thật cần.

Phải thiết kế các trạng thái `idle`, `loading`, `success`, `empty`, `validation error`, `request error`, `401`, `403`, `404`, `409`. Khi có thể, giữ kích thước layout ổn định trong lúc tải. Dialog phải có nhãn, điều khiển focus và trả focus hợp lý sau khi đóng.

Không bắt buộc tạo toàn bộ danh sách component trong một task. Không tự chuyển sang UI library mới chỉ để có phong cách đẹp hơn.

## 6. Đặc tả 6 màn hình MVP

| Màn hình | Thiết kế chủ đạo | Điều không được làm |
|---|---|---|
| **S01 — `/login`** | Desktop chia vùng nhận diện/biểu mẫu; họa tiết địa hình riêng; trên mobile một cột, đăng nhập rõ ràng | Không tạo tài khoản, social login, thống kê giả |
| **S02 — `/reports/new`** | Form cho Citizen; nhóm trường đánh số; trợ giúp nhập tọa độ, xác thực input, CTA gửi báo cáo | Không thêm map, upload hoặc trường ngoài API |
| **S03 — `/my-reports`** | Danh sách/card có thời gian, trạng thái xác minh, panel chi tiết; tiến độ mission từ E14 khi có | Không tạo timeline sự kiện hoặc mission giả |
| **S04 — `/operations/reports`** | Workbench duyệt: bộ lọc và danh sách bên trái, chi tiết/quyết định bên phải; chọn disaster qua E03 | Không thêm bước duyệt, tính năng map |
| **S05 — route Disaster đã có trong C1** | Danh sách sự kiện + panel chi tiết; form tạo/sửa/resolve; phân cấp trạng thái rõ ràng | Không đổi route, E03–E06 hoặc semantics resolve |
| **S06 — `/operations/rescue`** | Workbench điều phối: chọn report đã xác minh, chọn team, danh sách mission và thao tác chuyển trạng thái | Không kéo-thả phân công, tự động điều phối, map hay CRUD team |

Route ở bảng chỉ là tham chiếu theo kế hoạch. Với S05 và các feature đã triển khai, **đọc router/exports hiện tại**, không tự đổi tên đường dẫn.

## 7. Responsive, accessibility và UX acceptance

- Kiểm tra viewport **360px, 768px, 1280px**; không tràn ngang đối với nội dung thông thường.
- Có `:focus-visible`; mọi phần tử tương tác dùng keyboard được; dialog có tên truy cập được, quản lý focus và xử lý Escape khi phù hợp.
- Mọi input có label hiển thị. Lỗi gắn với field bằng `aria-describedby` khi thích hợp; lỗi quan trọng có `role="alert"`, thông báo thành công dùng vùng status phù hợp.
- Mục tiêu **WCAG 2.2 AA**: chữ thường tối thiểu 4.5:1, chữ lớn và thành phần UI quan trọng tối thiểu 3:1 khi áp dụng; kiểm tra cặp màu thực tế.
- Kích thước vùng chạm hướng đến 44×44px khi khả thi. Danh sách/bảng vẫn thao tác được trên mobile.
- Thời gian và múi giờ phải tuân thủ API contract; không thể hiện độ chính xác giả hoặc tự đổi timezone nghiệp vụ.
- Kiểm tra zoom 200%, tiếng Việt dài, dữ liệu trống/chậm/lỗi và chế độ giảm chuyển động.

## 8. Giới hạn kỹ thuật và ownership

1. Trước khi sửa, đọc `AGENTS.md`, kế hoạch MVP, P00 contracts, code frontend thật và các handoff liên quan.
2. **Người 1:** shared theme/layout/components/login/router. **Người 2:** Citizen Reporting. **Người 3:** Disaster và Operations Reporting. **Người 4:** Rescue.
3. Không sửa file feature của owner khác khi chưa phối hợp. Nếu muốn chỉnh S05, người 1 chỉ đưa đề xuất; người 3 thực hiện/review phần sửa thuộc Disaster.
4. Giữ nguyên token trong memory, router guards, API client, quyền, lỗi HTTP, request/response, business semantics, tests và backend behavior.
5. Không sửa backend, Flyway migration, OpenAPI hoặc domain chỉ để đổi giao diện.
6. Không bổ sung production dependencies nếu chưa có lý do và owner phê duyệt.
7. Không xóa thay đổi chưa commit. Không tự push, merge, deploy, public repo hoặc sinh secret.

## 9. Trình tự cải tạo sau khi P00/P01/C1/B1 đã hoàn thành

Ba checkpoint UI dưới đây đã được đăng ký trong catalog chính. Prompt thực thi nằm
ở `GDRN_UI01_PROMPTS.md`; file đó là phụ lục prompt, không phải kế hoạch độc lập.
Không chạy lại P00/P01/B1/C1 và không coi việc thêm tài liệu UI là task đã hoàn tất.

**UI01-A — Người 1:** audit frontend hiện tại; thêm tokens/components nền; cải tạo S01; lập checklist đề xuất S05. Chỉ sửa shared UI và phần login thuộc người 1.

**UI01-C1 — Người 3:** sau khi UI01-A merge, cải tạo S05 bằng shared design system, giữ E03–E06 và route hoạt động.

**B3 / C2 / D2:** triển khai những màn hình chưa có trực tiếp theo design system đã
merge; không tạo placeholder hay trang giả để chờ backend. B3 có thể làm phần S02/S03
dùng E07–E09 sau UI01-A, nhưng chỉ DONE khi B2 và D1 đã cung cấp withdraw/mission.

**UI01-B — Người 1 điều phối + các owner:** sau khi S01–S06 và UI01-C1 merge, audit
giao diện và accessibility; lỗi shared do người 1 xử lý, lỗi feature giao owner tương
ứng. UI01-B phải đạt trước X1 feature freeze.

**Điều kiện kiểm tra tại mỗi bước:** chạy frontend `typecheck`, `test` không watch, `build`; backend verify theo `AGENTS.md` nếu được yêu cầu; thử trình duyệt thật nếu có công cụ. Nếu không thử được browser, ghi **NOT RUN**, nêu checklist thủ công, không báo PASS giả. Mỗi task cập nhật `docs/handoffs/UI01-*.md` và tiến độ thật.

## 10. Checklist review thiết kế

- [ ] Có bản sắc GDRN, khác dashboard SaaS đại trà nhưng không thêm chức năng giả.
- [ ] Dùng chung tokens, typography, spacing và component.
- [ ] Citizen và Authority có mức độ tập trung thông tin phù hợp.
- [ ] Bảo toàn 6 màn hình, 15 endpoint, role matrix và route thực tế.
- [ ] Xử lý loading/empty/error/confirmation rõ ràng.
- [ ] Không tràn ngang ở viewport mục tiêu.
- [ ] Đạt mục tiêu tương phản, label, keyboard, focus và reduced motion.
- [ ] Dùng API thật, không mock dữ liệu trong production.
- [ ] Có bằng chứng commands/tests, hình ảnh nếu có và blockers trong handoff.
