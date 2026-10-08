# Pha 1 MVP — kế hoạch 4 người trong 3 tuần

Ngày lập: 08/10/2026. Đây là kế hoạch triển khai tiếp theo, không phải mô tả tính năng đã có.
Repo hiện có bootstrap và các domain types Identity ban đầu (`User`, `EmailAddress`,
`Role`) cùng unit tests; chưa có login/API nghiệp vụ hoặc frontend hoàn chỉnh.
Bộ prompt tương ứng: [PROMPTS_PHA_1_4_NGUOI.md](PROMPTS_PHA_1_4_NGUOI.md).

## 1. Mục tiêu và giới hạn thời gian

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

## 2. Phạm vi chốt cho kế hoạch

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

## 3. Chính xác 15 endpoint nghiệp vụ

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

## 4. Nghiệp vụ và hợp đồng phải thống nhất ở D1

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

### Đăng nhập và dữ liệu demo

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

## 5. Frontend bắt buộc: 6 màn hình

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

## 6. Phân công và quyền sở hữu file

| Người | Phần sở hữu | Review chéo | Prompt phụ trách |
| --- | --- | --- | --- |
| 1 — tích hợp + Identity | identity; shared technical config; frontend auth/layout/http/router; root config/CI | Người 3 | P00, P01, A1, A2, X1, A3 |
| 2 — Reporting + Geo | reporting; frontend/features/reporting-citizen; E2E sau khi tích hợp | Người 4 | B1, B2, B3, X2 |
| 3 — Disaster + UI duyệt | disaster; frontend/features/disaster và reporting-operations; tài liệu demo | Người 1 | C1, C2, C3 |
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

## 7. Lịch 15 ngày và cổng kiểm tra

| Ngày | Người 1 | Người 2 | Người 3 | Người 4 | Kết quả chung |
| --- | --- | --- | --- | --- | --- |
| D1 | P00, chốt contracts cả nhóm | Review Report/Geo + wireframe | Review Disaster + UI điều phối | Review Rescue + concurrency | API/schema hành vi/ownership rõ |
| D2 | P01 Identity + SPA nền | Chuẩn bị domain/test B1 từ contract | Chuẩn bị domain/test C1 từ contract | Thiết kế D1, rule và concurrency test plan | Không tự tạo security/router riêng |
| D3 | Merge P01 | B1 backend gửi/xem | C1 backend Disaster | Domain Rescue; chờ B2 contract thực thi | Login thật, frontend khởi động |
| D4 | Review/ghép routes theo handoff | B1 hoàn tất, bắt đầu B2 | C1 UI thật, merge trước B2 | D1 chuẩn bị persistence khi migration được cấp | API B1/C1 tích hợp |
| D5 | Kiểm tra mốc tuần 1 | B3 phần gửi/xem; B2 rule verification | Bắt đầu C2 theo E10 đã thống nhất | D1 sau C1/B2 merge | UI gửi và xem báo cáo chạy thật |
| D6 | A1 bảo vệ UI/API, review | B2 hoàn tất/merge | C2 UI duyệt | D1 backend + race tests | Report được duyệt từ UI |
| D7 | A1, hỗ trợ conflict shared | B3 rút/lọc/validation | C2 hoàn tất, kiểm tra C1 | D1 merge | Backend đủ nghiệp vụ |
| D8 | A2 Docker frontend + CI | B3 tiến độ sau D1 | Chuẩn bị C3 artifacts | D2 UI Rescue | Sáu màn hình ghép đủ routes |
| D9 | X1 tích hợp | Sửa Reporting + chuẩn bị X2 | Sửa Disaster/Operations UI | D2 hoàn tất | Demo xuyên suốt lần 1 |
| D10 | X1 chốt tích hợp | B3/X2 smoke | Rà luồng nghiệp vụ và quyền | D3 smoke đo tải, thử Kaggle | Đóng băng tính năng |
| D11 | A2 hoàn tất, hỗ trợ E2E | X2 E2E thật | C3 kiến trúc/README/rubric | D3 workload + môi trường | CI/E2E và fixture ổn định |
| D12 | Sửa vấn đề chung | Sửa lỗi + X2 | C3 rehearsal | D3 đo, lưu dữ liệu thô | Có số liệu benchmark thật |
| D13 | Review bản ứng viên | Kiểm tra hai user/403/409 | Chốt C3 | Phân tích D3, nêu giới hạn | Release candidate |
| D14 | A3 audit; sửa lỗi thuộc mình | Fix/retest thuộc mình | Fix/retest thuộc mình | Fix/retest thuộc mình | Không còn blocker |
| D15 | A3 bàn giao | Hỗ trợ clone sạch/demo | Demo + báo cáo | Benchmark reproducibility | Pha 1 hoàn tất theo DoD |

Các prompt có thể trải qua nhiều ngày. Không chạy prompt tiếp theo chỉ vì agent nói
"xong": kiểm tra handoff, test, review và code đã có trong nhánh tích hợp.

## 8. Thứ tự phụ thuộc của prompt

```text
P00 → P01
P01 → B1, C1, A1                         (có thể song song ở clone/nhánh riêng)
B1 + C1 → B2
B1 → B3 phần gửi/xem; B2 + D1 → B3 hoàn tất
C1 + B2 → C2
B2 + C1 → D1 → D2
P01 → A2; B3 + C2 + D2 + A1 + A2 → X1
X1 → X2, C3, D3                         (có thể song song)
X2 + C3 + D3 → A3
```

Trong thời gian chờ dependency, làm domain analysis, test cases, wireframe hoặc
review. Test doubles chỉ trong test; không thêm fake API/placeholder production để
vượt dependency chưa merge. P00/P01 cần ưu tiên review trong ngày vì chặn cả nhóm.

## 9. Git và cách dùng AI khi làm nhóm

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

## 10. Kiểm thử, benchmark và tiêu chí hoàn thành

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

## 11. Cách ứng phó chậm tiến độ

Nếu D5 chưa gửi/xem report trên UI, người 1 hỗ trợ tích hợp, dừng mọi trang trí hoặc
dependency mới; không tiếp tục chia thêm tính năng. Nếu D10 chưa demo xuyên suốt,
dừng mở rộng và dành người 3 hỗ trợ integration/review.

Có thể cắt animation, theme tùy chỉnh, tìm kiếm nâng cao, dashboard/map (vốn ngoài
scope), dữ liệu demo lớn. Không cắt auth/ownership, kiểm tra race, frontend thật,
migration, tests hoặc bằng chứng benchmark để tuyên bố hoàn tất.
Nếu vẫn quá tải, báo rõ tiêu chí chưa đạt và xin nhóm điều chỉnh scope/thời hạn;
không gắn nhãn hoàn thành bằng việc bỏ qua test hoặc đưa mock vào demo.

## 12. Tham khảo kỹ thuật cho các prompt

- [React: build an app from scratch](https://react.dev/learn/build-a-react-app-from-scratch)
  và [Vite guide](https://vite.dev/guide/) cho cách tạo SPA; xác minh Node/version
  requirements khi thực thi P01 và commit lockfile, không dựa vào `latest` mỗi máy.
- [k6 metrics](https://grafana.com/docs/k6/latest/using-k6/metrics/) cho số liệu chuẩn;
  các mức tải trong kế hoạch là lựa chọn thử nghiệm của nhóm, không số đo sẵn có.
