# Bộ 16 prompt triển khai Pha 1 MVP — 4 người / 3 tuần

Đọc [kế hoạch và bảng phân công](KE_HOACH_PHA_1_3_TUAN.md) trước khi chạy.
Đây là prompt cho các phiên phát triển sau; việc lưu tài liệu không có nghĩa code
đã được triển khai. Có 16 prompt chính, không phải chạy toàn bộ liên tiếp trên một máy.

## Cách sử dụng

1. Mỗi người mở clone/nhánh riêng đã cập nhật code từ nhánh tích hợp.
2. Kiểm tra các dependency trong bảng; phụ thuộc phải đã merge, không chỉ "người kia
   đang làm". Không chạy cả 4 người từ cùng một bootstrap chưa có contracts.
3. Copy **nguyên khối text** của đúng ID vào coding agent. Mỗi khối yêu cầu đọc hai
   tài liệu này và quy tắc chung bên dưới, nên không cần dán lại lịch sử chat.
4. Chạy một prompt, đọc diff, xem test và demo rồi mới review/merge/chạy prompt tiếp.
5. Khi cần tiếp tục một task dở, dùng mẫu tiếp tục ở cuối, không tạo lại cả module.

| ID | Người | Thời điểm dự kiến | Dependency đã merge | Bàn giao chính |
| --- | --- | --- | --- | --- |
| P00 | 1, cả nhóm review | D1 | Repo hiện tại | Contracts, scope, ownership, migrations plan |
| P01 | 1 | D2–D3 | P00 | Auth thật, SPA nền và login |
| B1 | 2 | D3–D4 | P01 | Tạo/xem report và ownership |
| C1 | 3 | D3–D5 | P01 | Disaster backend + UI |
| B2 | 2 | D5–D6 | B1, C1 | Verify/delete/spatial + published report contract |
| D1 | 4 | D5–D7 | B2, C1 | Rescue backend + concurrency |
| B3 | 2 | D5–D8, chia hai lượt | B1 cho phần đầu; B2/D1 để hoàn tất | Hai màn hình citizen |
| C2 | 3 | D6–D7 | B2, C1 | UI duyệt báo cáo |
| D2 | 4 | D8–D9 | D1 | UI điều phối |
| A1 | 1 | D5–D7 | P01; cập nhật khi module merge | Security/shared frontend hoàn chỉnh |
| A2 | 1 | D8–D11 | P01; hoàn tất sau các UI merge | Docker frontend và CI |
| X1 | 1, owner hỗ trợ | D9–D10 | B3, C2, D2, A1, A2 chạy được | Luồng thật xuyên suốt |
| X2 | 2 | D10–D12 | X1 | Browser E2E và kiểm tra hồi quy |
| D3 | 4 | Smoke D10, đo D11–D13 | X1 cho số đo chính thức | Script, raw benchmark, báo cáo |
| C3 | 3 | D11–D13 | X1; thêm kết quả X2/D3 khi có | Kiến trúc, rubric, demo, hướng dẫn |
| A3 | 1, cả nhóm review | D14–D15 | X2, C3, D3 | Final audit và bàn giao |

B3/A1/A2/C3 được tiếp tục khi dependency mới sẵn sàng, không đánh dấu hoàn tất ở
lượt đầu nếu còn phần bị chặn. Ngày thực hiện có thể dịch trong tuần nhưng giữ các
cổng D5/D10/D15. Khi P01 chưa merge, người 2–4 chỉ chuẩn bị analysis/test cases/wireframe.

## Quy tắc chung áp dụng cho mọi prompt

Các khối prompt dưới đây viện dẫn mục này như một phần nhiệm vụ:

- Đọc toàn bộ AGENTS.md áp dụng, context dài hạn, architecture overview, kế hoạch
  MVP, contracts đã tồn tại và code thật trước khi sửa. README có thể chậm hơn code;
  repo hiện đã có domain Identity ban đầu và package-info, không được regenerate.
- Scope MVP là 15 endpoint E01–E15 và 6 màn hình S01–S06 trong kế hoạch. Tính năng
  mới chỉ được triển khai bởi prompt sở hữu; không chạy sang prompt tiếp theo.
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
Thực hiện task P00 cho GDRN. Đọc AGENTS.md, docs/KE_HOACH_PHA_1_3_TUAN.md và mục
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
Sở hữu frontend/src/features/reporting-citizen và tests. P01/B1 phải đã merge.
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
Điều kiện P01/B2/C1 đã merge. Sở hữu frontend/src/features/reporting-operations.
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
Test approve/reject/invalid/filter/409/403, kiểm tra luồng thật citizen gửi → authority
duyệt → citizen xem VERIFIED. Cập nhật handoff C2 và gửi router registration cho người 1.
Run frontend checks + backend verify; báo rõ nếu công cụ browser không sẵn sàng.
```

## D2 — Màn hình điều phối cứu hộ (người 4)

```text
Thực hiện D2; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
Điều kiện D1/B2 đã merge. Sở hữu frontend/src/features/rescue và tests.

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
```

## A1 — Hoàn thiện security và phần UI dùng chung (người 1)

```text
Thực hiện A1; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
Điều kiện P01. Chỉ thay đổi security/shared frontend/root cần thiết, không thay owner
module nghiệp vụ. Chạy lại phần audit khi B2/D1 đã merge trước khi chốt A1 DONE.

Đối chiếu từng E01–E15 với role matrix: login công khai, các method/route nghiệp vụ
đúng quyền, ownership do application của module kiểm tra, unknown API default deny.
Kiểm tra token forged/expired/unsupported role, principal không nhận userId từ body,
JSON 401/403 nhất quán, không stacktrace/token/password trong response hoặc log.
Giữ health/Swagger hoạt động; OpenAPI khai báo Bearer. Không authentication giả,
generated user, registration hoặc refresh/logout endpoint ngoài scope.

Hoàn thiện shared frontend: nav theo role, guard, handling 401/403, memory token,
clear user-specific state khi logout; không biến frontend role guard thành security
duy nhất. Ghép feature route exports đã sẵn sàng, thống nhất error/loading/empty/form
components, không copy module screens hoặc viết lại CSS người khác không cần thiết.
Kiểm tra proxy/CORS chỉ cấu hình cần thiết theo môi trường, không mở wildcard credentials.

Thêm tests tập trung các lỗ hổng thực tế; cập nhật security ADR với behavior reload,
token TTL/logout không revoke và CSRF choice. Chạy full backend/frontend checks.
Ghi A1 handoff các endpoints đã kiểm tra và endpoints chưa merge còn cần review;
không đánh dấu toàn bộ security pass nếu chưa có các API để kiểm tra.
```

## A2 — Docker frontend, CI và cách chạy thống nhất (người 1)

```text
Thực hiện A2; đọc AGENTS.md, kế hoạch MVP, bộ prompt/mục Quy tắc chung và contracts P00.
P01 đã merge; sở hữu Docker/Compose/CI/root env/frontend build configuration.

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
Điều kiện B3/C2/D2/A1 và A2 phần runtime đã merge. Đọc handoff từng task và diff trước
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
Điều kiện X1. Dùng browser test framework đã có; nếu chưa có, phối hợp người 1 thêm
Playwright làm devDependency trong shared lockfile, không tự tạo hệ thống test trùng.

Viết browser E2E với frontend → backend → PostGIS thật, không intercept/mock API
nghiệp vụ trong các test acceptance. Dataset/credentials test riêng từ env, cleanup
chỉ trên môi trường test, không xóa database local người dùng. Tách contexts cho hai
citizen và authority; selectors theo role/label/testid, không sleep để che flakiness.

Bao phủ 6 màn hình và luồng X1; permission/ownership bằng API và browser, login sai,
session hết hạn, input invalid, pending withdraw, reject, phân team, tiến độ citizen,
409 khi dữ liệu bị thay đổi. Race correctness chính dùng D1 integration tests;
E2E kiểm tra người dùng thấy lỗi và refresh đúng, không thay thế test transaction.
Sau logout không còn dữ liệu riêng từ user trước. Kiểm tra viewport desktop/mobile
cơ bản và label/keyboard flows quan trọng, không đặt yêu cầu pixel-perfect lớn.

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
và mọi handoff P01/B1/B2/B3/C1/C2/C3/D1/D2/D3/A1/A2/X1/X2. Prerequisite phải merge.
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
