# Kế hoạch Pha 1 MVP — 16 task lõi + 3 checkpoint UI, 4 người / 3 tuần

Đây là **kế hoạch triển khai duy nhất** của GDRN: scope, phân công, dependency,
thứ tự task và tiêu chí hoàn thành đều tra tại file này. Không dùng bộ prompt nền
móng hay lịch triển khai riêng để giao việc hoặc tự chia lại task.

Contracts P00 (HTTP/OpenAPI, module contracts, migration ledger) quy định chi tiết
kỹ thuật; ADR ghi quyết định; handoff và phase1-progress ghi bằng chứng/trạng thái.
Các tài liệu đó không phải kế hoạch thay thế. Context dài hạn chỉ là backlog khi
khác scope MVP. Khi có mâu thuẫn, giữ scope/task tại đây và đồng bộ contract có review,
không tự đổi API hoặc migration đã áp dụng để khớp tài liệu cũ.

Việc lưu prompt hay có commit từng phần không chứng minh task đã DONE. Đọc code,
tests và handoff hiện tại trước khi thực thi; không chạy lại phần đã hoàn tất.
Có 16 task lõi và 3 checkpoint UI đã review. Chỉ thực hiện task được giao, không tự
chạy toàn bộ liên tiếp. Prompt UI01 nằm trong phụ lục
`docs/design/GDRN_UI01_PROMPTS.md` nhưng chỉ được chạy theo dependency và trạng thái
catalog tại file này; phụ lục không phải kế hoạch thay thế.

**Trạng thái nền ngày 10/10/2026:** P00, P01, B1, C1 đã DONE và có evidence trong
`docs/phase1-progress.md`; runtime có E01–E09, S01/S05 và migrations V1–V4. Không
chạy lại bốn task này. Công việc mở tiếp theo là UI01-A và B2; chúng có thể chạy song
song trên nhánh/clone riêng vì UI01-A chỉ sở hữu shared frontend + S01, còn B2 sở hữu
Reporting backend/schema. UI01-C1 chỉ chạy sau UI01-A merge.

## Phạm vi, phân công và các mốc MVP

### 1. Mục tiêu và giới hạn thời gian

Hoàn thành một luồng có frontend và backend thật:

> Người dân đăng nhập → gửi báo cáo có tọa độ → điều phối viên duyệt và gắn thảm họa
> → giao đội cứu hộ → cập nhật nhiệm vụ → người dân xem tiến độ.

Giả định 15 ngày làm việc, 4 người, mỗi người khoảng 4–5 giờ tập trung/ngày:
240–300 giờ công tổng. Dành khoảng 25–30% cho review, tích hợp, sửa lỗi, kiểm thử,
benchmark và tài liệu. Đây là ước lượng để lập kế hoạch, không bảo đảm tiến độ chỉ
bằng cách chạy prompt. Nếu chỉ có 1–2 giờ/người/ngày, cần chốt lại phạm vi/thời hạn.

Quy ước D1–D15 là ngày làm việc, không phải ngày lịch. Nhóm tự gán ngày bắt đầu.
Cuối D5 phải có luồng gửi báo cáo trên UI; cuối D10 có demo xuyên suốt và đóng băng
tính năng; D11–D15 chỉ hoàn thiện, đo, sửa lỗi và bàn giao.

Tài liệu này thu hẹp scope dài hạn trong `GDRN_CODEX_PROJECT_CONTEXT.md`: frontend
cơ bản là bắt buộc, nhiều module được hoãn. Không thay đổi kiến trúc Modular Monolith,
DDD/Clean, Java 21, PostgreSQL/PostGIS. P00 đồng bộ cách mô tả phạm vi trong tài liệu
trước khi nhóm bắt đầu code; không xóa lịch sử quyết định hoặc giả vờ đã có tính năng.

### 2. Phạm vi chốt cho kế hoạch

| Phần | Làm trong MVP | Hoãn |
| --- | --- | --- |
| Identity | Login, me, CITIZEN/AUTHORITY, tài khoản demo thật | Register, reset password, refresh token, quản trị user |
| Disaster | Tạo/xem/sửa, ACTIVE → RESOLVED | Lifecycle nhiều bước, tự phát hiện thảm họa |
| Reporting | Gửi/xem/duyệt/từ chối/rút; lọc bán kính | Upload, phát hiện trùng, kiểm duyệt tự động |
| Rescue | Đội được seed, giao nhiệm vụ, cập nhật trạng thái | CRUD đội, tính ưu tiên, phân công tự động, RescueRequest riêng |
| Geo | Điểm tọa độ, truy vấn PostGIS theo mét | Map tương tác, polygon, route, risk engine |
| Resource/Alert | Chưa triển khai | Kho, phân bổ, thông báo/realtime/SMS |

Không thêm broker, cache server, microservices, event bus hoặc abstraction chưa có nhu cầu.
Geo logic đơn giản nằm trong adapter của Reporting; chưa cần một module geo hoàn chỉnh.
Các tính năng hoãn là backlog sản phẩm, không mặc định trở thành Pha 2 kiến trúc.
Pha 2 kiến trúc vẫn phải xuất phát từ benchmark và quality attributes.

### 3. Chính xác 15 endpoint nghiệp vụ

`C`: CITIZEN; `A`: AUTHORITY. Ngoài login, các API dưới đây yêu cầu xác thực.
Health/Swagger và file static frontend không tính vào 15 endpoint.

| ID | Method/path | Quyền và chức năng | Chủ backend | Prompt |
| --- | --- | --- | --- | --- |
| E01 | POST /api/auth/login | Công khai, đăng nhập | Người 1 | P01 |
| E02 | GET /api/auth/me | C/A, thông tin bản thân | Người 1 | P01 |
| E03 | GET /api/disasters | C/A, danh sách phân trang | Người 3 | C1 |
| E04 | GET /api/disasters/{id} | C/A, chi tiết | Người 3 | C1 |
| E05 | POST /api/disasters | A, tạo | Người 3 | C1 |
| E06 | PATCH /api/disasters/{id} | A, cập nhật/chuyển trạng thái | Người 3 | C1 |
| E07 | POST /api/reports | C, gửi | Người 2 | B1 |
| E08 | GET /api/reports | C chỉ của mình; A tất cả; lọc/phân trang/bán kính | Người 2 | B1, B2 |
| E09 | GET /api/reports/{id} | C chủ báo cáo; A | Người 2 | B1 |
| E10 | PATCH /api/reports/{id}/verification | A, duyệt hoặc từ chối | Người 2 | B2 |
| E11 | DELETE /api/reports/{id} | C chủ báo cáo, chỉ khi PENDING; xóa mềm | Người 2 | B2 |
| E12 | GET /api/rescue-teams | A, danh sách đội để phân công | Người 4 | D1 |
| E13 | POST /api/rescue-missions | A, tạo và phân công | Người 4 | D1 |
| E14 | GET /api/rescue-missions | A tất cả; C chỉ nhiệm vụ của báo cáo mình | Người 4 | D1 |
| E15 | PATCH /api/rescue-missions/{id}/status | A, cập nhật trạng thái | Người 4 | D1 |

Không phát sinh endpoint riêng cho dashboard, lookup trạng thái, logout hoặc seed.
Enum cố định được chia sẻ qua hợp đồng. Logout frontend xóa thông tin phiên trong bộ nhớ.

### 4. Nghiệp vụ và hợp đồng phải thống nhất ở D1

- Report: PENDING → VERIFIED hoặc REJECTED, không duyệt lại. Duyệt cần disasterId
  hợp lệ đang ACTIVE; từ chối cần lý do. DELETE chỉ chủ sở hữu + PENDING, soft-delete;
  dữ liệu đã rút không xuất hiện qua GET thông thường.
- Disaster: ACTIVE → RESOLVED, không mở lại trong MVP. Cấm tạo phân công mới cho
  disaster đã RESOLVED tại thời điểm kiểm tra nghiệp vụ. Nhiệm vụ có từ trước vẫn
  được hoàn thành. Không thêm điều kiện đóng thảm họa phụ thuộc vào rescue.
- Mission: ASSIGNED → IN_PROGRESS → COMPLETED. Mỗi report tối đa một mission trong
  MVP, kể cả mission đã hoàn thành. Không có hủy/tái phân công trong scope.
- Một đội chỉ có một mission ASSIGNED/IN_PROGRESS. Chống race ở database bằng
  constraint/locking phù hợp, không chỉ kiểm tra trước rồi insert. Xung đột trả 409.
- User ID/role lấy từ danh tính đã xác thực; không tin reporterId/role gửi trong body.
- Tọa độ dùng latitude [-90,90], longitude [-180,180]. Filter cần đủ lat/lon/radius,
  radius tính theo mét, có giới hạn. Dùng PostGIS geography hoặc cách tương đương
  chính xác về đơn vị; không so khoảng cách độ với mét, không lọc toàn bộ bằng Java.
- P00 chốt UUID ID, ISO-8601 UTC, page bắt đầu từ 0, size mặc định/tối đa, sort ổn định,
  các trường DTO và status/error code. Dùng PageResponse riêng, không expose Spring Page.
- 401 cho thiếu/hết hạn token; 403 cho sai vai trò; truy cập đối tượng người khác dùng
  404 nhất quán để không lộ sự tồn tại; 400 validation, 409 xung đột nghiệp vụ.

Hướng gọi liên module:

```text
Rescue → Reporting published application contract → Disaster published application contract
```

Hợp đồng công khai đặt trong `<module>.application.contract`, chỉ gồm interface và
DTO immutable không phụ thuộc framework. P00 mô tả trước; chủ module triển khai khi
use case cần. Không tạo placeholder production classes ở bước lập hợp đồng.
Không đọc bảng/JPA repository của module khác, không tạo vòng phụ thuộc.

Reporting cung cấp trạng thái/owner/disaster cần thiết cho Rescue. Báo cáo chi tiết
không gọi ngược sang Rescue. Frontend xem tiến độ bằng E09 + E14?reportId=...;
backend E14 vẫn kiểm tra ownership. Query list phải có phân trang và tránh N+1;
API contract nội bộ có batch lookup nếu danh sách cần nó.

#### Đăng nhập và dữ liệu demo

JWT Bearer ngắn hạn, không refresh token. Dùng thư viện Spring Security phù hợp,
không tự viết mật mã/JWT parser. Secret/key lấy từ môi trường; validate thuật toán,
chữ ký, issuer/audience và expiration theo ADR. MVP chỉ cấp/sử dụng CITIZEN/AUTHORITY.
Enum hiện có thêm RESPONDER/ADMIN: giữ tương thích code/test đã có, chưa seed, chưa
thêm UI hoặc cấp quyền cho hai vai trò ngoài scope; không tự xóa code để khớp kế hoạch.
Frontend giữ token trong memory, reload trang phải đăng nhập lại; đây là đánh đổi
MVP có chủ ý. Logout chỉ xóa phiên local, token đã phát vẫn sống đến hết hạn.
Không lưu token vào URL/log/localStorage. Không dùng cookie authentication song song.
P01 quyết định cấu hình CSRF phù hợp với Bearer-only stateless và ghi rõ lý do.

Tạo ít nhất hai CITIZEN và một AUTHORITY qua cơ chế bootstrap profile `demo` có
kiểm soát; lấy mật khẩu từ môi trường, hash bằng PasswordEncoder. Không tài khoản
mặc định/secret trong migration hoặc production. Rescue seed đội demo cùng nguyên tắc
idempotent, không endpoint seed. Không đổi dữ liệu của user đã tồn tại khi chạy lại.

### 5. Frontend bắt buộc: 6 màn hình

Một SPA trong `frontend/`: React + TypeScript + Vite, React Router, fetch wrapper,
component/CSS dùng chung. Chủ P01 xác nhận phiên bản tương thích, pin bằng lockfile
và chuẩn hóa Node trong local/CI/Docker. Không làm SSR, Redux hoặc design system lớn.
Nếu repo đã có frontend phù hợp khi chạy prompt, giữ nó thay vì tạo lại.

| ID | Route dự kiến | Chức năng thật | Chủ |
| --- | --- | --- | --- |
| S01 | /login | Login, lỗi, chuyển trang theo role | Người 1 |
| S02 | /reports/new | Form gửi và tọa độ | Người 2 |
| S03 | /my-reports | List, detail panel, rút, xem tiến độ qua E14 | Người 2 |
| S04 | /operations/reports | Lọc, bán kính, chi tiết, duyệt/từ chối, chọn disaster | Người 3 |
| S05 | /operations/disasters | List/detail/create/edit/resolve | Người 3 |
| S06 | /operations/rescue | List, tạo phân công, chọn đội, cập nhật tiến độ | Người 4 |

Mọi màn hình có loading/error/empty, label và validation, responsive cơ bản, keyboard
access; xử lý 401/403/409. Không có nút giả hoặc mock API trong runtime bàn giao.
Người dân không cần dashboard riêng. Sau login chuyển thẳng đến danh sách báo cáo.
Frontend demo production được phục vụ cùng origin qua static server và proxy `/api`;
đây chỉ là cấu hình phục vụ frontend, không thêm hệ thống API gateway.

### 6. Phân công và quyền sở hữu file

| Người | Phần sở hữu | Review chéo | Prompt phụ trách |
| --- | --- | --- | --- |
| 1 — tích hợp + Identity | identity; shared technical config; frontend auth/layout/http/router; root config/CI | Người 3 | P00, P01, UI01-A, A1, A2, UI01-B, X1, A3 |
| 2 — Reporting + Geo | reporting; frontend/features/reporting-citizen; E2E sau khi tích hợp | Người 4 | B1, B2, B3, X2 |
| 3 — Disaster + UI duyệt | disaster; frontend/features/disaster và reporting-operations; tài liệu demo | Người 1 | C1, UI01-C1, C2, C3 |
| 4 — Rescue + đo tải | rescue; frontend/features/rescue; benchmark scripts/results | Người 2 | D1, D2, D3 |

**E10 chỉ người 2 viết backend; người 3 dùng E10 trên UI.** Người 4 không sửa repository
của Reporting. Người 2 không sửa class của Rescue để hiển thị tiến độ.
Người 1 không phải viết tất cả UI; chỉ thiết lập nền tảng dùng chung và ghép router.

File chung do người 1 điều phối: `pom.xml`, `.env.example`, `docker-compose.yml`,
Dockerfiles, CI, `frontend/package*.json`, router, API client, global CSS và shared DTO.
Owner khác cần thay đổi thì ghi yêu cầu trong handoff; người 1 gộp patch nhỏ, không
đợi tới tuần 3. `npm install` chỉ chạy ở nhánh chung hoặc theo thỏa thuận để tránh
lockfile conflict. Khi đã có lockfile, các thành viên dùng `npm ci`.

Migration: P00 lập sổ version và merge theo thứ tự Identity → Disaster → Reporting
→ Rescue. Không phát số thấp rồi merge sau khi DB đã áp dụng số cao. Không bật
out-of-order để chữa workflow; không sửa migration đã merge/áp dụng, dùng migration
mới cho sửa đổi. Không đặt JPA relation xuyên module. P00 phải quy định rõ ownership
của tham chiếu ID và chiến lược toàn vẹn, không tự thêm SQL join xuyên module.

### 7. Lịch 15 ngày và cổng kiểm tra

| Ngày | Người 1 | Người 2 | Người 3 | Người 4 | Kết quả chung |
| --- | --- | --- | --- | --- | --- |
| D1 | P00, chốt contracts cả nhóm | Review Report/Geo + wireframe | Review Disaster + UI điều phối | Review Rescue + concurrency | API/schema hành vi/ownership rõ |
| D2 | P01 Identity + SPA nền | Chuẩn bị domain/test B1 từ contract | Chuẩn bị domain/test C1 từ contract | Thiết kế D1, rule và concurrency test plan | Không tự tạo security/router riêng |
| D3 | Merge P01 | B1 backend gửi/xem | C1 backend Disaster | Domain Rescue; chờ B2 contract thực thi | Login thật, frontend khởi động |
| D4 | Review/ghép routes theo handoff | B1 hoàn tất, bắt đầu B2 | C1 UI thật, merge trước B2 | D1 chuẩn bị persistence khi migration được cấp | API B1/C1 tích hợp |
| D5 | UI01-A shared design + S01 | B2 rule/DB; B3 phần E07–E09 sau UI01-A | Review UI01-A, chuẩn bị C2 | Thiết kế D1/race, chờ B2 | Design system merge; gửi/xem report tiến triển |
| D6 | Hoàn tất UI01-A, bắt đầu A1 security | B2 hoàn tất/merge | UI01-C1 sau UI01-A; C2 sau B2 | D1 backend sau B2 | Report verify được; S01 có design system |
| D7 | A1 security/shared behavior | B3 rút/lọc/validation | UI01-C1 + C2 | D1 race tests/merge | Backend nghiệp vụ đủ, S05 đồng nhất |
| D8 | A2 Docker frontend + CI | B3 tiến độ mission sau D1 | C2 hoàn tất | D2 UI Rescue | Sáu màn hình ghép đủ routes |
| D9 | UI01-B điều phối audit | Sửa Reporting UI theo audit | Sửa S04/S05 theo audit | D2 hoàn tất, sửa S06 | UI01-B đạt trước tích hợp |
| D10 | X1 tích hợp/chốt | B3/X2 smoke | Rà luồng nghiệp vụ/quyền | D3 smoke, thử Kaggle | Demo xuyên suốt, đóng băng tính năng |
| D11 | A2 hoàn tất, hỗ trợ E2E | X2 E2E thật | C3 kiến trúc/README/rubric | D3 workload + môi trường | CI/E2E và fixture ổn định |
| D12 | Sửa vấn đề chung | Sửa lỗi + X2 | C3 rehearsal | D3 đo, lưu dữ liệu thô | Có số liệu benchmark thật |
| D13 | Review bản ứng viên | Kiểm tra hai user/403/409 | Chốt C3 | Phân tích D3, nêu giới hạn | Release candidate |
| D14 | A3 audit; sửa lỗi thuộc mình | Fix/retest thuộc mình | Fix/retest thuộc mình | Fix/retest thuộc mình | Không còn blocker |
| D15 | A3 bàn giao | Hỗ trợ clone sạch/demo | Demo + báo cáo | Benchmark reproducibility | Pha 1 hoàn tất theo DoD |

Các prompt có thể trải qua nhiều ngày. Không chạy prompt tiếp theo chỉ vì agent nói
"xong": kiểm tra handoff, test, review và code đã có trong nhánh tích hợp.

### 8. Thứ tự phụ thuộc của prompt

```text
P00 → P01 → B1 + C1                     (đã hoàn tất/merge)
B1 + C1 → B2
P01 + B1 + C1 → UI01-A                  (song song B2, file ownership khác)
UI01-A → UI01-C1
B1 + UI01-A → B3 phần gửi/xem; B2 + D1 → B3 hoàn tất
C1 + B2 + UI01-A → C2
B2 + C1 → D1 → D2; UI01-A → D2 phần giao diện
UI01-A → A1, A2
B3 + C2 + D2 + UI01-C1 → UI01-B
UI01-B + A1 + A2 → X1
X1 → X2, C3, D3                         (có thể song song)
X2 + C3 + D3 + UI01-B → A3
```

Trong thời gian chờ dependency, làm domain analysis, test cases, wireframe hoặc
review. Test doubles chỉ trong test; không thêm fake API/placeholder production để
vượt dependency chưa merge. P00/P01 cần ưu tiên review trong ngày vì chặn cả nhóm.

### 9. Git và cách dùng AI khi làm nhóm

Mỗi người có clone/worktree riêng; không cho nhiều agent cùng ghi một thư mục.
Một prompt tương ứng một thay đổi nhỏ có thể review, thông thường một PR. Nhánh ví
dụ: `feat/p01-foundation`, `feat/b1-reports`, `feat/c1-disasters`, `feat/d1-rescue`.
Dùng tên default branch thực tế của repo, không giả định luôn là `main`.

1. Pull nhánh tích hợp, xác nhận prerequisite đã merge và working tree không có
   thay đổi cần giữ. Không reset/clean để xóa công việc người khác.
2. Tạo nhánh feature; copy đúng một prompt trong bộ prompt, chạy tới acceptance gate.
3. Agent ghi `docs/handoffs/<ID>.md`: thay đổi, API/routes, migration, lệnh/test thực
   tế, dependency, lỗi còn lại và file chung cần tích hợp. Không ghi secret/token.
4. Người sở hữu đọc diff, thử UI/API thật; reviewer kiểm tra contracts và nghiệp vụ.
5. Merge theo dependency/migration order, rồi người khác cập nhật nhánh của mình.

Không tự push/merge/public/deploy từ prompt nếu người dùng chưa yêu cầu ở phiên đó.
Không đổi hợp đồng đã freeze mà không thông báo owner các bên và cập nhật docs/test.
Mỗi ngày check-in 15 phút: đã merge gì, đang bị chặn ở đâu, demo được bước nào.

### 10. Kiểm thử, benchmark và tiêu chí hoàn thành

Mỗi feature cần domain tests, application tests với ports, integration tests thật
PostGIS/security theo rủi ro. Verify đầy đủ dùng `./mvnw clean verify` hoặc Windows
`.\mvnw.cmd clean verify`, không skip Testcontainers. Frontend cần typecheck, test,
production build; E2E nối browser → API → DB thật. CI phải chạy các gate đã thiết lập.

Mốc kiểm tra quan trọng:

- Hai citizen không đọc/rút báo cáo hay xem mission của nhau; authority thao tác được.
- Duyệt/rút report đồng thời không tạo trạng thái không hợp lệ.
- Hai request phân cùng đội và hai request tạo mission cho cùng report: chỉ một
  thành công; còn lại 409; không làm mất dữ liệu hoặc trả lỗi 500 ngoài ý muốn.
- Domain không phụ thuộc Spring/JPA; không cross-module infrastructure; JPA không
  lộ qua API; published contracts nhỏ và chiều dependency không có cycle.
- Swagger bootstrap test "không có operation" phải được thay bằng kiểm tra chính xác
  hợp đồng đã triển khai; không xóa test chỉ để thêm endpoint không kiểm soát.

Benchmark ít nhất E07 (ghi), E08 (đọc phân trang), E08 với bán kính (spatial):
smoke → tải vừa → tăng tải theo khả năng môi trường. Ví dụ 10/50/100 VU là điểm bắt
đầu, không phải SLO hay yêu cầu đạt 2000 VU. Ghi seed/data size, warm-up/duration,
request mix, token lifetime, máy chạy generator/server/DB, cấu hình phần cứng và commit.
Ghi throughput, p50/p95/p99, error rate, CPU/RAM khi đo được; lưu raw output và script.

Theo tiêu chí hiện ghi trong context, phải có load test trên Kaggle CPU. D3 phải thử
môi trường từ D10; phân biệt rõ Kaggle chạy generator hay cả server/DB. Nếu rubric
yêu cầu server trên Kaggle, không lấy chạy generator-only làm bằng chứng thay thế.
Không giả định Kaggle có Docker daemon; kiểm tra khả năng cài/chạy Java/PostGIS và
network thực tế. Có thể làm local baseline trong lúc bị chặn, nhưng chưa được đánh
dấu đạt tiêu chí Kaggle. Không mở database, tunnel hoặc publish dịch vụ để đo khi
chưa có yêu cầu/ủy quyền cụ thể; không bịa số liệu.

**Definition of Done chung:** 15 API đúng contract; 6 màn hình dùng API thật; luồng
demo hoàn chỉnh; quyền/ownership được test; không sai phân công đồng thời; clone mới
chạy frontend/backend/database bằng hướng dẫn; Docker/CI/ArchUnit/E2E đạt; có dataset
demo và benchmark tái lập; README/C4/ADRs khớp thực tế; có checklist rubric (gồm yêu
cầu public repository do nhóm xử lý, không tự publish từ agent).

### 11. Cách ứng phó chậm tiến độ

Nếu D5 chưa gửi/xem report trên UI, người 1 hỗ trợ tích hợp, dừng mọi trang trí hoặc
dependency mới; không tiếp tục chia thêm tính năng. Nếu D10 chưa demo xuyên suốt,
dừng mở rộng và dành người 3 hỗ trợ integration/review.

Có thể cắt animation, theme tùy chỉnh, tìm kiếm nâng cao, dashboard/map (vốn ngoài
scope), dữ liệu demo lớn. Không cắt auth/ownership, kiểm tra race, frontend thật,
migration, tests hoặc bằng chứng benchmark để tuyên bố hoàn tất.
Nếu vẫn quá tải, báo rõ tiêu chí chưa đạt và xin nhóm điều chỉnh scope/thời hạn;
không gắn nhãn hoàn thành bằng việc bỏ qua test hoặc đưa mock vào demo.

### 12. Tham khảo kỹ thuật cho các prompt

- [React: build an app from scratch](https://react.dev/learn/build-a-react-app-from-scratch)
  và [Vite guide](https://vite.dev/guide/) cho cách tạo SPA; xác minh Node/version
  requirements khi thực thi P01 và commit lockfile, không dựa vào `latest` mỗi máy.
- [k6 metrics](https://grafana.com/docs/k6/latest/using-k6/metrics/) cho số liệu chuẩn;
  các mức tải trong kế hoạch là lựa chọn thử nghiệm của nhóm, không số đo sẵn có.

## Cách sử dụng

1. Mỗi người mở clone/nhánh riêng đã cập nhật code từ nhánh tích hợp.
2. Kiểm tra các dependency trong bảng; phụ thuộc phải đã merge, không chỉ "người kia
   đang làm". Không chạy cả 4 người từ cùng một bootstrap chưa có contracts.
3. Copy **nguyên khối text** của đúng ID vào coding agent. Task lõi lấy prompt trong file này;
   ba checkpoint UI01 lấy prompt trong `docs/design/GDRN_UI01_PROMPTS.md`. Mọi ID đều phải có
   trong bảng catalog bên dưới và cùng áp dụng các contracts, dependency, ownership và quy tắc chung
   của file này; không cần dán lại lịch sử chat.
4. Chạy một prompt, đọc diff, xem test và demo rồi mới review/merge/chạy prompt tiếp.
5. Khi cần tiếp tục một task dở, dùng mẫu tiếp tục ở cuối, không tạo lại cả module.

| ID | Người | Thời điểm dự kiến | Dependency đã merge | Bàn giao chính |
| --- | --- | --- | --- | --- |
| P00 | 1, cả nhóm review | D1 | Repo hiện tại | Contracts, scope, ownership, migrations plan |
| P01 | 1 | D2–D3 | P00 | Auth thật, SPA nền và login |
| B1 | 2 | D3–D4 | P01 | Tạo/xem report và ownership |
| C1 | 3 | D3–D5 | P01 | Disaster backend + UI |
| UI01-A | 1 | D5–D6 | P00, P01, B1, C1 | Shared design system + cải tạo S01; prompt ở phụ lục UI01 |
| B2 | 2 | D5–D6 | B1, C1 | Verify/delete/spatial + published report contract |
| UI01-C1 | 3 | D6–D7 | UI01-A, C1 | Cải tạo S05 theo shared design system; prompt ở phụ lục UI01 |
| D1 | 4 | D5–D7 | B2, C1 | Rescue backend + concurrency |
| B3 | 2 | D5–D8, chia hai lượt | B1 + UI01-A phần đầu; B2/D1 để hoàn tất | S02/S03 theo design system |
| C2 | 3 | D6–D7 | B2, C1, UI01-A | S04 theo design system |
| D2 | 4 | D8–D9 | D1, UI01-A | S06 theo design system |
| A1 | 1 | D6–D8 | UI01-A; audit lại khi B2/D1 merge | Security + shared behavior; không làm lại design system |
| A2 | 1 | D8–D11 | UI01-A; hoàn tất sau UI merge | Docker frontend và CI |
| UI01-B | 1 điều phối + owners | D9 | B3, C2, D2, UI01-C1 | Audit 6 màn hình; gate trước X1; prompt ở phụ lục UI01 |
| X1 | 1, owner hỗ trợ | D9–D10 | UI01-B, A1, A2 chạy được | Luồng thật xuyên suốt |
| X2 | 2 | D10–D12 | X1 | Browser E2E và kiểm tra hồi quy |
| D3 | 4 | Smoke D10, đo D11–D13 | X1 cho số đo chính thức | Script, raw benchmark, báo cáo |
| C3 | 3 | D11–D13 | X1; thêm kết quả X2/D3 khi có | Kiến trúc, rubric, demo, hướng dẫn |
| A3 | 1, cả nhóm review | D14–D15 | X2, C3, D3 | Final audit và bàn giao |

B3/A1/A2/C3 được tiếp tục khi dependency mới sẵn sàng, không đánh dấu hoàn tất ở
lượt đầu nếu còn phần bị chặn. Ngày thực hiện có thể dịch trong tuần nhưng giữ các
cổng D5/D10/D15. P00/P01/B1/C1 đã hoàn tất; không dùng bảng để chạy lại lịch sử.

Ba prompt UI01 nằm trong `docs/design/GDRN_UI01_PROMPTS.md` để giữ chi tiết thiết kế gần
tài liệu UI. Chúng là phần thực thi của catalog này, không phải kế hoạch thứ hai. Không chạy
UI01-A, UI01-C1 hoặc UI01-B nếu dependency/trạng thái trong bảng trên chưa cho phép.

## Quy tắc chung áp dụng cho mọi prompt

Các khối prompt dưới đây viện dẫn mục này như một phần nhiệm vụ:

- Đọc toàn bộ AGENTS.md áp dụng, context dài hạn, architecture overview, kế hoạch
  MVP, contracts đã tồn tại và code thật trước khi sửa. README có thể chậm hơn code;
  repo hiện đã có domain Identity ban đầu và package-info, không được regenerate.
- Scope MVP là 15 endpoint E01–E15 và 6 màn hình S01–S06 trong kế hoạch. Tính năng
  mới chỉ được triển khai bởi prompt sở hữu; không chạy sang prompt tiếp theo.
- Với mọi task sửa frontend, đọc `docs/design/GDRN_UI_DESIGN_SYSTEM.md` và handoff
  UI01 mới nhất. UI01-A là nền bắt buộc cho B3/C2/D2; UI01-C1 thuộc owner Disaster;
  UI01-B là gate trước X1. Design system không được đổi API, route, role hoặc nghiệp vụ.
- Không xóa hoặc ghi đè thay đổi chưa commit của người khác. Không tự push, merge,
  public repo, publish web hay deploy ra bên ngoài. Không tự sinh secret cố định.
- Kiến trúc: domain plain Java; application orchestration + ports; HTTP ở API;
  JPA/PostGIS ở infrastructure; DTO riêng, constructor injection. Không truy cập
  infrastructure module khác. Không dùng shared làm nơi chứa logic nghiệp vụ.
- Tham chiếu module khác qua contracts đã chốt. Dependency chưa có thì mô tả blocker
  cụ thể, làm phần độc lập/test, không tạo fake implementation trong production.
  Không tự thay tên trường/endpoint để làm UI của riêng mình chạy.
- Tuân thủ file ownership và migration order. Thay đổi file chung cần phối hợp người 1;
  nếu phiên hiện tại không sở hữu, ghi patch/đề nghị vào handoff. Chủ tích hợp phải
  áp dụng patch và kiểm tra trong ngày. Không đánh dấu UI hoàn tất khi chưa gắn router.
- Không thêm endpoint seed, sample domain, fake business data trong runtime chính,
  broker, Redis, distributed architecture, AI/risk engine hoặc map ngoài scope.
- Feature thật phải có DB migration cùng code; không ddl-auto update/create. Domain
  model trước persistence model. Không sửa migration đã áp dụng hoặc skip Flyway.
- Backend chạy clean verify đầy đủ với Docker. Frontend chạy scripts typecheck,
  test ở chế độ không watch, build; P01 thiết lập chúng. Thêm tests theo rủi ro,
  không test chỉ để đủ số. Không skip/suppress tests để đạt build xanh.
- Cập nhật bootstrap tests khi semantics thay đổi: ví dụ Swagger không còn trống
  khi có API. Thay assertion bằng hợp đồng đúng, không xóa coverage bừa bãi.
- Kiểm tra UI với API thật; test doubles chỉ trong tests. Không tuyên bố đã kiểm tra
  browser nếu không có công cụ: cung cấp checklist thao tác và ghi rõ chưa kiểm tra.
- Nếu thiếu Docker/network/credentials, báo lệnh và lỗi thật, không giả kết quả hoặc
  tự tắt integration tests. Có thể dùng settings-central.xml/cache riêng như hướng dẫn.
- Mỗi task cập nhật `docs/handoffs/<ID>.md`, ghi commit nền nếu có, file/API/routes,
  migration, dependencies, lệnh và kết quả thực tế, việc còn lại; không log secret.
- Final trả lời: đã sửa gì, tiêu chí nào đạt/chưa đạt, tests và blockers, cách chạy/demo,
  yêu cầu owner khác, prompt tiếp theo được mở khóa. Không tự chạy prompt tiếp theo.

## P00 — Chốt hợp đồng và phạm vi (người 1)

```text
Thực hiện task P00 cho GDRN. Đọc AGENTS.md, phần kế hoạch trong docs/PROMPTS_PHA_1_4_NGUOI.md và mục
"Quy tắc chung" của docs/PROMPTS_PHA_1_4_NGUOI.md; coi chúng là yêu cầu của task này.
Đọc context dài hạn, architecture overview và code hiện có. Chỉ lập tài liệu/contracts,
chưa implement business/frontend. Giữ User, EmailAddress, Role và tests hiện có.

Mục tiêu: bốn người có thể làm độc lập mà không tự đoán API, phân quyền hoặc migration.
Tạo docs/api/phase1-contract.yaml (OpenAPI hợp lệ, chính xác E01–E15),
docs/api/phase1-contract.md và docs/architecture/phase1-module-contracts.md.
Định nghĩa đầy đủ request/response, field limits, required/optional, enums, UUID,
UTC timestamps, pagination/filter/sort, success/error statuses, ownership, ví dụ JSON.
Chốt ma trận endpoint/màn hình/role, state transitions và race expectations theo kế hoạch.
List mission nhận reportId; citizen chỉ xem được mission thuộc report của mình.
API report detail không gọi ngược Rescue; UI dùng hai query để xem tiến độ.

Published contracts: Disaster query dùng cho Reporting; Reporting query dùng cho Rescue.
Chốt interface/DTO dự kiến, missing/deleted behavior, batch query và chiều phụ thuộc
không cycle. Chỉ mô tả signature, chưa tạo placeholder Java classes.
Lập sổ migration: owner, thứ tự merge Identity → Disaster → Reporting → Rescue,
version chưa được tự cấp độc lập, quy tắc không sửa migration đã áp dụng.
Viết ADR mới về scope MVP và lựa chọn auth/frontend dự kiến, dùng số kế tiếp chưa dùng.
JWT in-memory, no refresh, hai role hoạt động; ghi rõ reload cần login lại.

Thêm bảng P00/P01/B1.../A3 trạng thái TODO vào docs/phase1-progress.md; P00 DONE khi
contracts không mâu thuẫn. Đồng bộ README/context/architecture bằng liên kết và scope
note, giữ phần dài hạn là backlog; không ghi các API/UI planned là implemented.
Chuẩn bị dữ liệu mẫu trong tài liệu thôi, không migration business ở task này.
Validate YAML/OpenAPI bằng công cụ sẵn có phù hợp; đối chiếu đủ 15 operation + 6 UI,
không operation nào thiếu người sở hữu hoặc consumer. Chạy kiểm tra theo AGENTS.md.
Ghi docs/handoffs/P00.md gồm các quyết định nhóm cần review và handoff cho P01/B1/C1/D1.
Không bắt đầu P01.
```

## P01 — Identity và frontend nền (người 1)

```text
Thực hiện P01; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung, và contracts P00.
Điều kiện: P00 đã có trong nhánh hiện tại. Sở hữu identity, shared technical config,
frontend nền/login/router/API client và root dependencies. Không implement module khác.

Implement E01 POST /api/auth/login, E02 GET /api/auth/me, S01 /login bằng API thật.
Tái sử dụng domain User/EmailAddress/Role; bổ sung reconstitution/provisioning phù hợp,
không lạm dụng public register endpoint vì endpoint này ngoài scope. Chỉ cấp demo role
CITIZEN/AUTHORITY; giữ enum/test cũ tương thích, không cấp thêm role ngoài MVP.
Tách persistence user/credential và domain; migration được cấp theo sổ P00; password
hash bằng PasswordEncoder. Không hardcode user/password trong migration hoặc production.
Cơ chế seed profile demo idempotent, 2 citizen + 1 authority từ biến môi trường,
chạy lại không reset mật khẩu/role dữ liệu đã có. Thiếu cấu hình demo phải báo rõ.

Dùng Spring Security JWT Bearer stateless với thư viện tương thích Boot hiện có.
Key/secret từ environment, validate signature/algorithm/expiry/issuer/audience theo ADR,
không custom crypto, không log credentials/token. Me lấy principal đã xác thực.
Login lỗi không tiết lộ user tồn tại; đúng status/JSON contract. Không form login/basic
hay generated development user. CSRF/CORS xử lý rõ theo Bearer-only; default deny.
Giữ health/Swagger và bổ sung OpenAPI security scheme. Chỉ mở route của feature đã có.

Nếu chưa có frontend, tạo frontend/ với React + TypeScript + Vite + routing và fetch
wrapper; xác minh version tương thích, pin Node cho local/CI, commit npm lockfile.
Thiết lập scripts dev/typecheck/test/build, tests cho auth/API client; chưa fake các page.
Token chỉ trong memory; /me xác thực phiên sau login; reload cần login lại; logout
xóa state/cache; 401 chuyển login, 403 báo lỗi phù hợp. Không localStorage/URL token.
Tạo layout/nav theo role, component form/loading/error/empty dùng chung và convention
feature route exports để người 2–4 không sửa global router. Vite proxy /api dùng cấu
hình local; không đưa backend/container hostname vào bundle trình duyệt.

Test login đúng/sai, expired/tampered token, me, no-secret seed, hai role và frontend
login thật. Cập nhật bootstrap assertions theo API mới, không xóa security coverage.
Chạy clean verify, frontend typecheck/test/build; cập nhật hướng dẫn env/local.
Ghi handoff P01 với principal contract, route registration, component usage, migrations,
test results và phần A1/A2 còn lại. Không tự mở toàn bộ future API bằng permitAll.
```

## B1 — Báo cáo: tạo và xem (người 2)

```text
Thực hiện B1; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
P01 phải đã merge. Sở hữu com.gdrn.reporting và tests/migration được cấp; không sửa
security/router/package lock chung, gửi yêu cầu tích hợp nhỏ cho người 1 nếu cần.

Implement E07 POST /api/reports, E08 GET /api/reports, E09 GET /api/reports/{id}.
Domain Report plain Java: id, owner, thông tin sự cố, tọa độ, PENDING và timestamps
theo contract. Owner lấy principal, không từ body; không có JPA/HTTP/Spring trong domain.
Thiết kế để B2 bổ sung verification/delete; không implement E10/E11 ở đây.
Application ports → persistence adapter, DTO riêng, migration theo sổ; point storage
phù hợp PostGIS ngay từ đầu. Nếu dùng Hibernate Spatial, phối hợp root dependency.
List phân trang/sort ổn định và filter đã có: citizen chỉ của mình, authority được
đọc tất cả. Detail người khác trả 404; không cho query ownerId vượt quyền. Chặn input
rỗng/quá dài, latitude/longitude ngoài miền, page/size ngoài giới hạn.

Tests: domain validation, use case ownership, PostGIS persistence roundtrip,
anonymous 401, citizen create, authority create bị cấm theo contract, hai citizen
không đọc chéo, authority list/detail, pagination và DTO không lộ persistence.
Đánh dấu filter bán kính ở B2 là chưa thực thi, không fake kết quả hoặc quảng cáo đã có.
Chạy verify thật. Cập nhật OpenAPI/runtime khớp phần đã implement và handoff B1 với
payload ví dụ an toàn, status codes, migration và nhu cầu route policy cho người 1.
Không tạo frontend mock runtime hoặc module Rescue để làm report detail.
```

## C1 — Thảm họa: backend và màn hình (người 3)

```text
Thực hiện C1; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
P01 phải đã merge. Sở hữu com.gdrn.disaster, frontend/src/features/disaster và tests.

Implement E03–E06: list/detail/create/patch disaster. Domain ACTIVE → RESOLVED,
không reopen; field validation theo contract. Citizen chỉ đọc; authority tạo/sửa.
DTO riêng, app use cases/ports, JPA adapter, migration theo sổ, không DB-first generic CRUD.
Implement published Disaster application contract đã chốt ở P00 để B2 kiểm tra ID và
trạng thái; interface/DTO framework-independent. Không expose JPA entity/repository.
Không phụ thuộc Reporting/Rescue để quyết định resolve và không mở endpoint mới.

Implement S05: danh sách, filter/phân trang, detail panel, create/edit/resolve form,
confirmation với thao tác resolve, loading/error/empty/401/403/409, responsive cơ bản.
API thật, dùng fetch/auth/component convention từ P01; export route đúng convention,
gửi handoff người 1 để gắn router nếu cần. Không duplicate global CSS/auth/client.

Tests domain transition/validation, permissions cho tất cả write methods, published
contract missing/resolved, migration/persistence thật và UI form/error phù hợp.
Chạy backend verify + frontend checks, thử UI thật nếu công cụ có sẵn. Handoff C1
phải cho B2 biết chính xác public contract đã merge và cách dùng màn hình tạo disaster.
Không seed user mới hay implement API verification thuộc người 2.
```

## B2 — Duyệt, rút và lọc không gian (người 2)

```text
Thực hiện B2; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
Điều kiện: B1 và C1 đã merge. Sở hữu Reporting; không sửa Disaster infrastructure.

Implement E10 PATCH /api/reports/{id}/verification: authority only; PENDING → VERIFIED
với disaster ACTIVE qua published contract, hoặc REJECTED với lý do; không duyệt lại.
Implement E11 DELETE: citizen chủ báo cáo + PENDING, soft-delete; GET bình thường
không lộ report đã rút. Chốt đúng behavior retry/not found/409 theo P00.
Hai transaction verify/delete đồng thời phải cho trạng thái nhất quán; dùng optimistic
version/locking phù hợp, ánh xạ conflict 409 thay vì 500. Không chỉ kiểm tra UI.

Hoàn thiện E08 filter lat/lon/radiusMeters bằng PostGIS, validate đủ bộ/giới hạn,
khoảng cách theo mét và pagination ở DB. Thêm spatial index đúng query; không load
toàn bộ để lọc trong Java. Test tọa độ gần/xa/biên, invalid/missing params, kết hợp
filter với ownership không lộ dữ liệu citizen khác.
Implement Reporting published contract P00 cho Rescue: owner/status/deleted/disaster
và batch query cần thiết; không trả entity, không gọi ngược Rescue.

Test states/role/ownership, invalid disaster, resolved disaster, verify vs withdraw
concurrency trên PostGIS thật, public contract và regression B1. Không sửa migration
đã áp dụng; thêm migration mới cho trường/index theo sổ.
Chạy verify; handoff B2 cho C2/B3/D1 gồm DTO/error codes/contract signatures và evidence.
Không implement màn hình authority vì C2 sở hữu.
```

## D1 — Rescue backend và chống phân công trùng (người 4)

```text
Thực hiện D1; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
Điều kiện: C1/B2 đã merge; sở hữu com.gdrn.rescue và tests/migrations được cấp.

Implement E12 GET /api/rescue-teams; E13 POST /api/rescue-missions;
E14 GET /api/rescue-missions; E15 PATCH /api/rescue-missions/{id}/status.
RescueTeam seed chỉ profile demo, idempotent; không CRUD team, không seed endpoint.
Mission lấy report/owner/status/disaster qua Reporting public contract; chỉ report
VERIFIED chưa có mission, disaster ACTIVE ở kiểm tra nghiệp vụ mới được phân công.
Không truy cập reporting/disaster table, entity hoặc repository trực tiếp.
Nhiệm vụ đã tồn tại được hoàn thành kể cả disaster sau đó RESOLVED.

Domain ASSIGNED → IN_PROGRESS → COMPLETED, không cancel/reassign. Mỗi report tối đa
một mission; mỗi team chỉ một active mission. Enforce ở DB (unique report reference,
partial unique index cho team đang active hoặc cách locking có bằng chứng tương đương).
Transaction application/service phù hợp với kiến trúc; map conflict thành 409 ổn định.
Không lộ persistence exception. Authorize authority cho team list/create/update;
mission list citizen chỉ theo report owner thực sự, kể cả khi truyền reportId khác.
List có page/filter; avoid N+1 qua contract batch hoặc chiến lược đã thống nhất.

Test domain lifecycle và quyền; integration concurrency bằng hai transaction thực
với barrier/latch: cùng team/khác report, cùng report/khác team, một success/một409;
team dùng lại được sau completed nhưng report cũ không tạo mission mới. Assert DB
không tồn tại assignment bất hợp lệ. Không dùng sleep test không ổn định để giả race.
Test E14 citizen isolation và missing/deleted/rejected reports. Không mở rộng scope
tự động điều phối. Chạy verify, handoff D1 cho D2/B3 và người 1.
```

## B3 — Hai màn hình người dân (người 2)

```text
Thực hiện B3; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
Đọc thêm docs/design/GDRN_UI_DESIGN_SYSTEM.md và handoff UI01-A. Sở hữu
frontend/src/features/reporting-citizen và tests. P01/B1/UI01-A phải đã merge.
Nếu B2 hoặc D1 chưa merge, chỉ làm phần gửi/xem, ghi B3 IN_PROGRESS; không fake
verification/withdraw/mission và không tuyên bố hoàn tất. Tiếp tục khi dependency có.

Implement S02 /reports/new và S03 /my-reports bằng API thật: form có label/validation
tọa độ/nội dung, loading chống gửi lặp từ UI, báo lỗi rõ, thành công chuyển tới report.
Danh sách phân trang/refresh/status, detail panel, rút report PENDING với confirmation.
Tiến độ rescue lấy từ E14?reportId=... khi có D1, không thêm endpoint và không buộc
Reporting gọi ngược Rescue. Hiển thị report chưa có mission là trạng thái bình thường.
Giữ auth/token/client ở module chung, không lưu token hoặc báo cáo riêng vào storage
không cần thiết. Clear private UI state khi logout/401; xử lý 404/409 và refresh.
Không tạo map/upload/dashboard riêng. Đảm bảo dùng được trên màn hình hẹp và bàn phím.
Tái sử dụng semantic tokens/shared components từ UI01-A, thể hiện Citizen flow ít áp
lực nhận thức; không nhân bản theme, global CSS, API client hoặc tự thêm UI library.
Kiểm tra 360/768/1280px, focus-visible, reduced motion và tương phản theo design spec.

Test form invalid, submit success/failure, list empty/loading/error, withdraw conflict,
mission status. Test doubles chỉ cho tests. Browser smoke với hai citizen nếu công cụ
có sẵn, không tuyên bố isolation backend chỉ từ việc ẩn nút.
Export/gắn routes theo ownership, kiểm tra navigation sau tích hợp người 1. Run frontend
typecheck/test/build và backend verify theo quy tắc. Handoff B3 gồm điều kiện hoàn tất
B2/D1, screenshot nếu có, API coverage và luồng còn thiếu. Không sửa auth/router chung
mà không điều phối người 1.
```

## C2 — Màn hình duyệt báo cáo (người 3)

```text
Thực hiện C2; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
Đọc thêm docs/design/GDRN_UI_DESIGN_SYSTEM.md và handoff UI01-A. Điều kiện
P01/B2/C1/UI01-A đã merge. Sở hữu frontend/src/features/reporting-operations.
Không viết lại Reporting backend; mọi lỗi E08/E09/E10 bàn giao người 2 sửa.

Implement S04 /operations/reports cho AUTHORITY: list phân trang/filter trạng thái,
bộ lọc tọa độ + radiusMeters, detail panel, chọn disaster ACTIVE từ E03, approve/reject.
Chọn disaster có search/pagination nếu danh sách phân trang; không giả định trang đầu
chứa mọi disaster. Validate approve phải có disaster, reject phải có lý do theo DTO.
Sau mutation refresh list/detail đúng; hiển thị 409 khi người khác đã xử lý/rút report,
không giữ UI trạng thái thành công giả. Không nút "tự duyệt" hoặc map ngoài scope.

Dùng shared client/layout/auth, route guard AUTHORITY nhưng backend vẫn là cổng quyền.
Loading/error/empty/confirmation/responsive/keyboard labels đầy đủ. Không dùng userId
từ input để vượt quyền, không tự thay response contract.
Tổ chức theo Authority Operational Workbench, tái sử dụng shared tokens/components;
không nhân bản theme/client hoặc tạo dashboard/KPI. Kiểm tra 360/768/1280px,
focus-visible, reduced motion và tương phản theo design spec.
Test approve/reject/invalid/filter/409/403, kiểm tra luồng thật citizen gửi → authority
duyệt → citizen xem VERIFIED. Cập nhật handoff C2 và gửi router registration cho người 1.
Run frontend checks + backend verify; báo rõ nếu công cụ browser không sẵn sàng.
```

## D2 — Màn hình điều phối cứu hộ (người 4)

```text
Thực hiện D2; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
Đọc thêm docs/design/GDRN_UI_DESIGN_SYSTEM.md và handoff UI01-A. Điều kiện
D1/B2/UI01-A đã merge. Sở hữu frontend/src/features/rescue và tests.

Implement S06 /operations/rescue: list missions phân trang/filter, panel tạo từ report
VERIFIED và chọn team qua E12; liên kết từ UI duyệt/report detail nếu owner đã thống nhất.
Dùng APIs thật, lấy danh sách cần chọn đủ pagination. Team/report đủ điều kiện trên
UI chỉ hỗ trợ trải nghiệm, backend D1 vẫn kiểm tra và bảo vệ race.
Cho authority chuyển ASSIGNED → IN_PROGRESS → COMPLETED với trạng thái loading/error,
confirmation phù hợp; không cancel/reassign/CRUD team hoặc endpoint mới.
Khi 409 team/report đã được phân công, refresh dữ liệu và báo rõ, không tự retry tạo
mission hay giả success. Disable double-submit ở UI, xử lý 401/403/404.

Test form, valid transition, invalid action ẩn/disable, empty team list, failed request,
409 conflict. Demo thật hai phiên authority cùng chọn một đội để đối chiếu server
không tạo hai active missions; citizen đọc được tiến độ qua UI B3 sau khi hoàn tất.
Giữ layout/router/client convention và responsive. Chạy frontend checks/backend verify,
ghi handoff D2 cùng routes, actions và kết quả demo. Không tự sửa Reporting internals.
Tổ chức theo Authority Operational Workbench và dùng shared tokens/components; không
nhân bản theme/client hay thêm dashboard/map/drag-drop. Kiểm tra 360/768/1280px,
keyboard/focus, reduced motion và tương phản theo design spec.
```

## A1 — Hoàn thiện security và phần UI dùng chung (người 1)

```text
Thực hiện A1; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
Đọc design system và handoff UI01-A. Điều kiện UI01-A đã merge. Chỉ thay đổi
security/shared frontend/root cần thiết, không thay owner module nghiệp vụ. UI01-A
đã sở hữu theme/primitives/S01; A1 không làm lại hoặc đổi thẩm mỹ design system.
Chạy lại phần audit khi B2/D1 đã merge trước khi chốt A1 DONE.

Đối chiếu từng E01–E15 với role matrix: login công khai, các method/route nghiệp vụ
đúng quyền, ownership do application của module kiểm tra, unknown API default deny.
Kiểm tra token forged/expired/unsupported role, principal không nhận userId từ body,
JSON 401/403 nhất quán, không stacktrace/token/password trong response hoặc log.
Giữ health/Swagger hoạt động; OpenAPI khai báo Bearer. Không authentication giả,
generated user, registration hoặc refresh/logout endpoint ngoài scope.

Hoàn thiện shared frontend behavior: nav theo role, guard, handling 401/403, memory token,
clear user-specific state khi logout; không biến frontend role guard thành security
duy nhất. Ghép feature route exports đã sẵn sàng, thống nhất error/loading/empty/form
behavior trên primitives UI01-A, không copy module screens hoặc viết lại design/CSS
người khác không cần thiết. Thiếu primitive mới thì bổ sung nhỏ, có consumer thật và
document trong handoff; không mở rộng thành UI framework riêng.
Kiểm tra proxy/CORS chỉ cấu hình cần thiết theo môi trường, không mở wildcard credentials.

Thêm tests tập trung các lỗ hổng thực tế; cập nhật security ADR với behavior reload,
token TTL/logout không revoke và CSRF choice. Chạy full backend/frontend checks.
Ghi A1 handoff các endpoints đã kiểm tra và endpoints chưa merge còn cần review;
không đánh dấu toàn bộ security pass nếu chưa có các API để kiểm tra.
```

## A2 — Docker frontend, CI và cách chạy thống nhất (người 1)

```text
Thực hiện A2; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
Đọc design system/handoff UI01-A. P01 và UI01-A đã merge; sở hữu Docker/Compose/CI/
root env/frontend build configuration. Không sửa giao diện feature trong task này.

Thêm frontend container multi-stage: npm ci + production build, phục vụ static SPA,
history fallback và proxy /api tới backend qua Docker network. Không API gateway
platform, không SSR; browser không dùng hostname container như URL public.
Frontend API path tương đối /api. Không bake JWT secret/demo password vào bundle,
chỉ backend giữ secrets. Compose có frontend/backend/database, volume DB và health
checks hợp lý. Giữ các port hiện có configurable; thêm FRONTEND_PORT riêng và cập nhật
env.example, không ghi đè .env cá nhân hoặc publish dịch vụ ra Internet.

Pin Node tương thích P01 và lockfile; CI chạy backend clean verify với Docker,
frontend npm ci/typecheck/test/build, validate Compose và build images. Sau X2 có
script E2E, thêm CI job chạy với API/DB thật, secrets test sinh tạm và cleanup.
Không đánh dấu A2 hoàn tất phần E2E trước khi X2 cung cấp implementation.

Cập nhật hướng dẫn tiếng Việt cho clone sạch, hai cách dev, seed profile demo,
export env, start/stop, health/Swagger/frontend, test; phân biệt cổng host/container.
Thử build/up/health bằng cấu hình local; kiểm tra reload route sâu SPA và /api trả JSON
không bị fallback HTML. Không xóa volume dữ liệu để làm startup xanh.
Ghi handoff A2 với lệnh thực chạy, ports, images, CI coverage và việc cần X2 bổ sung.
```

## X1 — Tích hợp luồng thật, đóng băng tính năng (người 1 điều phối)

```text
Thực hiện X1; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
Đọc design system và handoff UI01-A/UI01-C1/UI01-B. Điều kiện UI01-B, A1 và A2 phần
runtime đã merge; UI01-B đã xác nhận đủ B3/C2/D2/UI01-C1. Đọc handoff từng task và diff trước
khi sửa. Không regenerate module hoặc triển khai tính năng ngoài 15 API/6 màn hình.

Chạy stack thật. Kiểm tra hai citizen + một authority từ demo bootstrap; chuỗi:
citizen gửi report → authority tạo disaster ACTIVE → verify report → phân team →
IN_PROGRESS → COMPLETED → citizen thấy tiến độ. Thử citizen khác đọc/rút report không
thuộc mình, rút report PENDING, verify REJECTED, conflict phân team và expired session.
Đối chiếu OpenAPI runtime với contract và frontend clients; sửa mismatch nhỏ đúng owner,
ghi task rõ cho owner khi liên quan business, không sửa mọi thứ tập trung trong shared.

Kiểm tra routes/nav, error messages, fields/timezones, pagination/dropdown nhiều trang,
không console error/API giả/secret hoặc internal entity trên HTTP. Sửa chặn demo trước,
không thêm charts/map/animation. API chưa có UI phải được hoàn thiện trong screen owner.
Tạo docs/qa/phase1-integration-checklist.md và cập nhật progress/handoff X1 với evidence
thật. Nếu không điều khiển được browser thì không tự đánh dấu UI pass; để checklist
cho nhóm chạy và chỉ báo phần đã kiểm tra bằng công cụ.
Chạy clean verify và frontend checks, Docker config/build/start cần thiết.
Chốt feature freeze chỉ khi flow thật hoạt động; X2/C3/D3 sau đó mới lấy baseline ổn định.
```

## X2 — E2E frontend/backend và hồi quy (người 2, người 1 ghép CI)

```text
Thực hiện X2; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
Đọc design system và handoff UI01-B. Điều kiện X1. Dùng browser test framework đã có;
repo đã có Playwright từ P01/C1, vì vậy tái sử dụng cấu hình hiện tại; chỉ phối hợp
người 1 chỉnh shared lockfile/config khi có nhu cầu thật, không tạo framework test trùng.
Nếu trạng thái code sau merge khác, kiểm tra trước khi thêm dependency.

Viết browser E2E với frontend → backend → PostGIS thật, không intercept/mock API
nghiệp vụ trong các test acceptance. Dataset/credentials test riêng từ env, cleanup
chỉ trên môi trường test, không xóa database local người dùng. Tách contexts cho hai
citizen và authority; selectors theo role/label/testid, không sleep để che flakiness.

Bao phủ 6 màn hình và luồng X1; permission/ownership bằng API và browser, login sai,
session hết hạn, input invalid, pending withdraw, reject, phân team, tiến độ citizen,
409 khi dữ liệu bị thay đổi. Race correctness chính dùng D1 integration tests;
E2E kiểm tra người dùng thấy lỗi và refresh đúng, không thay thế test transaction.
Sau logout không còn dữ liệu riêng từ user trước. Kiểm tra viewport desktop/mobile
cơ bản, thêm 360/768/1280 theo UI acceptance khi khả thi, label/keyboard/focus và
reduced motion flows quan trọng; không đặt yêu cầu pixel-perfect lớn.

Cung cấp scripts chạy/fixture/ports, trace khi fail không lộ token, phối hợp A2 đưa
vào CI với services thật. Chạy suite thật, backend verify và frontend checks.
Phân loại lỗi app vs test vs môi trường; sửa đúng owner/phạm vi, không skip flaky test
để lấy xanh. Handoff X2 ghi số test, command/output thật và gate chưa chạy được.
```

## D3 — Benchmark tái lập và Kaggle CPU (người 4)

```text
Thực hiện D3; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
Làm smoke kiểm tra môi trường từ D10; số đo chính thức chỉ sau X1 trên commit ổn định.
Sở hữu benchmark/ và docs/benchmark/, không tối ưu kiến trúc hoặc thêm Redis/broker.

Viết k6 workloads E07 ghi report, E08 đọc phân trang, E08 filter bán kính. Cấu hình
base URL/credentials/VUs/duration/data seed bằng env; không hardcode production URL
hoặc secret, không benchmark endpoint không tồn tại. Ghi dataset size/distribution,
pagination/query mix, warm-up, repetitions, reset giữa runs trên DB benchmark riêng.
JWT lifetime/login setup phải được tính đúng; không đếm expired-token failures là
bottleneck domain, không bỏ lỗi khỏi báo cáo. Không thêm endpoint seed vào application.

Lập smoke → 10/50/100 VU nếu máy cho phép; nêu lý do thay mức tải khi có evidence.
Ghi throughput, p50/p95/p99/error rate, CPU/RAM khi đo được; lưu raw k6 results,
scripts, cấu hình máy, commit, start/end, generator/server/DB topology và limitations.
Không claim performance benefit nếu chưa có before/after. Không tăng delay giả tạo.

Kiểm tra khả năng chạy Kaggle CPU theo rubric trong context: generator-only khác
server-on-Kaggle. Không giả định có Docker daemon; thử Java/PostGIS/network khả dụng,
viết notebook/hướng dẫn có thể tái lập. Nếu thiếu account/tool/access thì chuẩn bị
artifact và báo đúng bước nhóm cần chạy; local results chỉ là baseline local, không
đánh dấu Kaggle complete. Không tự publish DB/service/tunnel để truy cập từ Kaggle.

Tạo báo cáo với bottleneck quan sát được, dữ liệu thô và candidate Phase 2 dưới dạng
giả thuyết, không triển khai candidate. Chạy scripts smoke và kiểm tra build theo
AGENTS.md; handoff D3 ghi rõ phép đo thực hiện vs chỉ chuẩn bị, mọi tiêu chí chưa đạt.
```

## C3 — Tài liệu, rubric và kịch bản demo (người 3)

```text
Thực hiện C3; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
Điều kiện X1; đọc code/handoff thực tế, bổ sung kết quả X2/D3 khi có. Không implement
feature mới. README/context hiện có thể khác code, kiểm tra trước khi cập nhật.
Đọc design system và UI01 handoffs; mô tả nó là quyết định UX của frontend hiện tại,
không gọi nó là module kiến trúc hoặc tính năng nghiệp vụ mới.

Cập nhật README, hướng dẫn clone/run tiếng Việt, architecture overview, C4 context
và container/component diagram vừa đủ cho modular monolith + SPA + DB. Ghi rõ
implemented/planned, module dependency thực tế, JWT tradeoffs, transactions/concurrency,
PostGIS, Flyway, test/CI, database model phát sinh từ domain; không vẽ microservices.
Cập nhật ADR trạng thái theo quyết định đã làm, không rewrite lịch sử trái thực tế.

Tạo docs/qa/phase1-rubric.md ánh xạ từng tiêu chí context (GET/POST/DELETE, protected
GET/POST, login/filter, ORM, domain isolation, Swagger, Docker, GitHub public, commit
history, Kaggle benchmark) tới file/test/demo evidence. Chưa kiểm tra/chưa đạt ghi rõ,
không tự public repo hay bịa link kết quả/commit. Đối chiếu rubric giảng viên nếu nhóm
đã cung cấp; tài liệu context không thay cho đề bài ngoài repo chưa được đọc.

Viết demo 8–10 phút với tài khoản lấy từ env, bước và expected result trên 6 màn hình;
có case cross-user denied, pending withdraw, assignment conflict và hoàn thành mission.
Hướng dẫn reset chỉ môi trường demo/test có xác nhận dữ liệu disposable, không đưa
down -v thành lệnh khởi động thường ngày. Rehearsal theo tool có sẵn, ghi bước chưa chạy.
Link benchmark D3, không tự điền số. Handoff C3 và progress; chạy checks theo AGENTS.md.
```

## A3 — Audit cuối và bàn giao Pha 1 (người 1)

```text
Thực hiện A3; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung, contracts P00
và mọi handoff P01/B1/B2/B3/C1/C2/C3/D1/D2/D3/A1/A2/UI01-A/UI01-C1/UI01-B/X1/X2.
Prerequisite phải merge. Đọc design system và acceptance UI đã review.
Đây là final audit, không thêm tính năng, không bắt đầu Pha 2, không tự tag/push/deploy.

Kiểm tra từ checkout/clone sạch có thể chạy frontend/backend/PostGIS theo docs với
env tự tạo; không dựa vào ignored cache/secret/artifact của máy cũ. Giữ dữ liệu của
checkout chính, không xóa volume đang dùng. Xác nhận migrations từ DB rỗng và upgrade
đúng thứ tự, demo bootstrap idempotent, 15 endpoint/6 màn hình đúng quyền và đầy đủ UI.

Chạy backend clean verify đầy đủ, frontend npm ci/typecheck/test/build, browser E2E,
Docker config/build/up/health và luồng demo. Audit architecture/JPA leak/module cycle,
ownership, concurrency evidence, tokens/secrets in logs/bundles, security method matrix.
Không bỏ gate hoặc bỏ integration test vì mất Docker; báo blocker và dừng kết luận DONE.
Rà README/rubric/ADRs và raw benchmark, Kaggle requirements vs evidence, public repo
status nếu có quyền đọc, commit history; không nhận "script đã viết" là "đã đo".

Sửa lỗi nhỏ trong phạm vi sở hữu; giao lỗi module cho owner bằng handoff rõ, không
đổi hợp đồng phút cuối không review. Retest phần bị ảnh hưởng sau sửa.
Tạo docs/qa/phase1-final-audit.md: PASS/FAIL/NOT RUN cho từng tiêu chí, commands/results,
commit ứng viên, known limitations, bước chạy lại và nhiệm vụ còn thiếu.
Chỉ đánh dấu Pha 1 DONE nếu toàn bộ acceptance gate bắt buộc có bằng chứng thật.
Nếu bị chặn thì ghi phần đã đạt và blocker; không gọi completion khi hết thời gian.
Ghi handoff A3 và báo cáo bàn giao ngắn cho nhóm, không thực hiện Pha 2.
```

## Mẫu tiếp tục task đang dở (không tính là prompt tính năng mới)

Thay `<ID>` bằng task thật, chỉ dùng trong nhánh của task đó:

```text
Tiếp tục task <ID> của GDRN từ code hiện tại. Đọc AGENTS.md, kế hoạch MVP, đúng khối
prompt <ID> trong docs/PROMPTS_PHA_1_4_NGUOI.md và docs/handoffs/<ID>.md nếu có.
Kiểm tra git diff và dependency mới đã merge. Không regenerate phần đã làm, không
xóa thay đổi người khác, không chạy sang task khác. Hoàn thành acceptance criteria
còn thiếu, chạy tests thực tế, cập nhật handoff/progress và báo rõ blocker còn lại.
```

Nếu gặp lỗi test, gửi thêm **lệnh, output lỗi đã loại secret và commit/nhánh** cho
owner. Không gửi chỉ yêu cầu "làm build xanh" vì agent có thể mất ngữ cảnh nghiệp vụ.
