# Hợp đồng HTTP/UI Pha 1 — P00

P01 status: E01/E02 đã implemented, S01 dùng API thật; các operation/screen còn lại
vẫn planned. YAML dưới đây giữ baseline đích P00, không đại diện runtime Swagger.
Runtime chỉ công bố slice Identity; xem [handoff P01](../handoffs/P01.md).

B1 update 09/10/2026: E07–E09 adapters có tests, nhưng chưa tích hợp migration và
central route policy. [B1 adapter OpenAPI](b1-adapter-contract.json) mô tả chính xác
phần đã viết; **không phải runtime Swagger đã mở**. E08 radius vẫn B2, hiện nhận
lat/lon/radiusMeters sẽ bị 400 trong adapter. Xem [handoff B1](../handoffs/B1.md).

Ngày chốt thiết kế: 08/10/2026. **Planned, chưa implemented.** Chuẩn máy đọc là
[OpenAPI 3.0.3](phase1-contract.yaml); quy tắc có điều kiện dưới đây cũng là yêu cầu
bắt buộc. Nếu cần đổi, cập nhật cả YAML, tài liệu và handoff, để producer/consumer
review trước khi merge. Không tự đổi tên trường để ghép UI.

Nguồn scope: [kế hoạch](../KE_HOACH_PHA_1_3_TUAN.md),
[quy tắc chung](../PROMPTS_PHA_1_4_NGUOI.md#quy-tắc-chung-áp-dụng-cho-mọi-prompt),
[contracts module và sổ migration](../architecture/phase1-module-contracts.md),
[ADR 005](../adr/005-phase1-mvp-auth-frontend.md).
P00 giữ nguyên User, EmailAddress, Role và tests. Chỉ CITIZEN/AUTHORITY hoạt động;
RESPONDER/ADMIN vẫn có trong enum Java, không được seed/cấp quyền/hiển thị UI MVP.

## Quy ước chung

- Base `/api`, JSON UTF-8. Mọi body là object, trường dư/null bị từ chối 400.
  YAML `required` là bắt buộc; trường response tùy trạng thái được **bỏ hẳn**, không null.
  Không nhận body ở GET/DELETE. Query lạ, query lặp, enum sai case, UUID sai,
  JSON lỗi hoặc thiếu body bắt buộc =>400. Body sai Content-Type =>415.
- UUID canonical lowercase có dấu gạch; server sinh ID. ID/filter/body reference
  không phải bằng chứng quyền. `reporterId`, role, trạng thái khởi tạo, thời gian
  đều do server quyết định; gửi thêm các trường này bị 400.
- Timestamp ISO-8601 UTC `Z`, độ chính xác tối đa 9 chữ số phần giây; client hiển thị
  theo múi giờ người dùng. Không nhận timestamp từ client. `updatedAt` bằng thời điểm
  tạo khi chưa sửa. Không có timestamp tương lai do client đặt.
- Text trim hai đầu trước kiểm tra length, không blank; đếm Unicode code points.
  Name 1–120, description 1–2000, rejectionReason 1–500. Không render như HTML.
  Email trim/lowercase `Locale.ROOT`, 3–254, format đúng EmailAddress hiện có.
  Password không trim, 1–128 code points và tối đa 72 UTF-8 bytes; limit là HTTP/
  credential concern, không thay đổi domain tests. Login response không có password.
- Disaster type/report type: EARTHQUAKE, FLOOD, TYPHOON, WILDFIRE, TSUNAMI.
  Severity chỉ thuộc Disaster: LOW, MODERATE, HIGH, CRITICAL. Không bắt buộc report
  type bằng disaster type; authority chịu trách nhiệm chọn liên kết.
- Latitude [-90,90], longitude [-180,180], số hữu hạn WGS84. Không đảo thứ tự;
  PostGIS point constructor nhận longitude trước latitude.
- Không có upload, register, refresh, logout API, CRUD team, dashboard API, separate
  RescueRequest, alert/resource, event bus, map hay thuật toán ưu tiên.

## Phân trang, filter và sort

Tất cả collection E03/E08/E12/E14 dùng `page=0`, `size=20`; page 0–1000000,
size 1–100. Không tự clamp giá trị sai. Trả `{items,page,size,totalElements,totalPages}`,
không expose Spring Page. Empty list và page vượt cuối trả 200, `items: []`;
`totalPages=ceil(totalElements/size)` (0 nếu không có phần tử).
Count và items phải dùng cùng quyền/filter và cùng snapshot của mỗi request.
Không cam kết snapshot xuyên nhiều request/phân trang khi có write đồng thời.

Sort một giá trị: E03/E08/E14 `createdAt,desc` mặc định, hoặc `createdAt,asc`;
với mission, createdAt là alias của assignedAt (không thêm trường response).
E12 `name,asc` mặc định hoặc `name,desc`, so sánh Unicode code point của name đã trim.
Luôn thêm `id ASC` khi bằng nhau. Không sort theo trường tùy ý, không repeated sort.

| API | Filter tùy chọn (AND) | Không tìm thấy reference |
| --- | --- | --- |
| E03 | status, type | Không áp dụng |
| E08 | status, type, disasterId, lat/lon/radiusMeters | disasterId không tồn tại =>200 rỗng |
| E12 | availability=AVAILABLE/BUSY | Không áp dụng |
| E14 | reportId, status, teamId | reportId thiếu thực thể/đã rút/khác owner =>404; teamId không tồn tại =>200 rỗng |

E08 chỉ nhận đủ cả lat/lon/radiusMeters hoặc không nhận cả ba; bán kính integer 1–100000
mét, biên `distance <= radiusMeters`. Tên wire là `radiusMeters` như prompt B2/C2;
`radius` trong kế hoạch là cách gọi khái niệm, không phải alias query được chấp nhận.
PostGIS geography/SRID 4326, lọc tại DB trước page/
count; không lọc Java hay dùng độ thay mét. Không filter reporterId. Citizen luôn
chỉ thấy report của mình và không bao giờ thấy report đã rút.

E14: `reportId` bắt buộc khi CITIZEN, thiếu =>400; AUTHORITY có thể bỏ. Kiểm tra
report qua Reporting **trước** filter mission, kể cả report chưa có mission. Report
hợp lệ của chính mình chưa được phân công =>200 rỗng; report người khác =>404 giống
report không tồn tại. Không trả danh sách toàn bộ mission cho citizen. S03 dùng
E09 và E14?reportId=... riêng biệt; lỗi/empty của tiến độ không làm giả dữ liệu report.
E09 không chứa mission và tuyệt đối không gọi ngược Rescue.

## Ma trận endpoint / owner / consumer

Person tương ứng Người trong kế hoạch. Consumer là screen dùng trực tiếp hoặc auth
shell của screen. Cả hai role được gọi E03/E04 dù UI MVP chỉ dùng chúng trên S04/S05.
E10 backend Người 2; UI S04 Người 3. HTTP success dưới đây là cố định, PATCH trả entity
mới nhất sau commit. POST trả entity, không yêu cầu Location header (mission chưa có
detail route). Không có endpoint thứ 16.

| ID | Method / path | Backend owner / task | Role | Consumer | Success |
| --- | --- | --- | --- | --- | --- |
| E01 | POST /api/auth/login | 1 / P01 | PUBLIC | S01 | 200 |
| E02 | GET /api/auth/me | 1 / P01 | CITIZEN, AUTHORITY | S01, S02, S03, S04, S05, S06 | 200 |
| E03 | GET /api/disasters | 3 / C1 | CITIZEN, AUTHORITY | S04, S05 | 200 |
| E04 | GET /api/disasters/{id} | 3 / C1 | CITIZEN, AUTHORITY | S04, S05 | 200 |
| E05 | POST /api/disasters | 3 / C1 | AUTHORITY | S05 | 201 |
| E06 | PATCH /api/disasters/{id} | 3 / C1 | AUTHORITY | S05 | 200 |
| E07 | POST /api/reports | 2 / B1 | CITIZEN | S02 | 201 |
| E08 | GET /api/reports | 2 / B1/B2 | CITIZEN, AUTHORITY | S03, S04, S06 | 200 |
| E09 | GET /api/reports/{id} | 2 / B1 | CITIZEN, AUTHORITY | S03, S04, S06 | 200 |
| E10 | PATCH /api/reports/{id}/verification | 2 / B2 | AUTHORITY | S04 | 200 |
| E11 | DELETE /api/reports/{id} | 2 / B2 | CITIZEN | S03 | 204 |
| E12 | GET /api/rescue-teams | 4 / D1 | AUTHORITY | S06 | 200 |
| E13 | POST /api/rescue-missions | 4 / D1 | AUTHORITY | S06 | 201 |
| E14 | GET /api/rescue-missions | 4 / D1 | CITIZEN, AUTHORITY | S03, S06 | 200 |
| E15 | PATCH /api/rescue-missions/{id}/status | 4 / D1 | AUTHORITY | S06 | 200 |

## Sáu màn hình và thao tác

| ID / route | Owner / task | Role | APIs / hành vi |
| --- | --- | --- | --- |
| S01 `/login` | 1 / P01 | Public | E01, E02; citizen đến S03, authority đến S04 |
| S02 `/reports/new` | 2 / B3 | CITIZEN | E02 shell, E07; nhập type/description/latitude/longitude, thành công đến S03 chọn report mới |
| S03 `/my-reports` | 2 / B3 | CITIZEN | E02, E08, E09, E11, E14; list/filter/detail panel/rút PENDING/progress |
| S04 `/operations/reports` | 3 / C2 | AUTHORITY | E02, E08, E09, E03, E04, E10; filter bán kính, duyệt/chọn ACTIVE disaster hoặc từ chối có lý do |
| S05 `/operations/disasters` | 3 / C1 | AUTHORITY | E02, E03, E04, E05, E06; list/detail/create/edit/resolve bằng version hiện tại |
| S06 `/operations/rescue` | 4 / D2 | AUTHORITY | E02, E08, E09, E12, E13, E14, E15; chọn VERIFIED report, AVAILABLE team, giao và cập nhật mission |

E02 được auth shell gọi sau login; không bắt buộc gọi lại ở mỗi render. UI guard chỉ
hỗ trợ trải nghiệm, backend vẫn kiểm tra role/ownership. Selector disaster/team/report
phải có phân trang để chọn cả dữ liệu ngoài trang đầu; điều kiện UI không thay server.
S04 có thể điều hướng S06 với query `reportId` (UUID), S06 tải E09 rồi mới điền form;
đây là trạng thái UI, không phải màn hình/API mới. Các detail là panel cùng route.

Mọi màn hình có loading/error/empty, labels, keyboard, responsive và form validation.
401: xóa token/user/cache, về login; 403: thông báo không đủ quyền; 404: không có dữ
liệu/không còn truy cập, không suy đoán chủ sở hữu; 409: thông báo, refetch phần liên
quan rồi để người dùng quyết định. Disable double-submit; không tự retry write.
Không nút giả, không mock runtime. Reload phải login lại. Logout xóa memory/session
UI và state của user trước; không revoke JWT đã cấp.

## Trạng thái và cạnh tranh

| Aggregate | Khởi tạo | Cho phép | Cấm / kết quả |
| --- | --- | --- | --- |
| Disaster | ACTIVE, version=0 | Sửa trường khi ACTIVE; ACTIVE → RESOLVED | RESOLVED immutable, không reopen; explicit cùng status =>409 |
| Report | PENDING | PENDING → VERIFIED hoặc REJECTED; owner rút PENDING | Không duyệt lại; không sửa report; rút không phải status mới |
| Mission | ASSIGNED | ASSIGNED → IN_PROGRESS → COMPLETED | Không skip, lặp, cancel, reassign; 409 |
| Team | AVAILABLE được suy ra | BUSY khi có ASSIGNED/IN_PROGRESS; AVAILABLE khi hoàn tất | Không status write riêng |

E06 cần `expectedVersion` và ít nhất một trường sửa; không nhận null. Missing ID =>404,
version cũ =>409 STALE_VERSION, rồi kiểm tra transition. Mỗi successful patch tăng
version 1, kể cả ghi cùng nội dung nhưng bỏ status. Hai patch cùng version: chỉ một
thành công, cái còn lại 409; không lost update.

E10/E11: atomic conditional write/version trong Reporting, không check rồi save thiếu
bảo vệ. Nếu cả hai đã đọc được PENDING thì hai verify: một thành công, một 409;
verify và withdraw: một thành công, bên thua 409 INVALID_TRANSITION; hai withdraw:
một 204, một 409. Nếu lần đọc đầu đã thấy missing/soft-deleted (kể cả retry sau
withdraw) thì 404. Đọc đầu thấy VERIFIED/REJECTED thì 409. Vì vậy request đến sau
commit withdraw có thể nhận 404; test race dùng barrier sau khi cả hai đọc PENDING
để assert 409. Với owner khác luôn 404 trước khi xét trạng thái. Hai command
mission cùng trạng thái đích: một 200, một 409; không ghi đè timestamps.

E13 bảo vệ bằng DB unique trên reportId (kể cả completed) và partial unique teamId
cho status ASSIGNED/IN_PROGRESS, hoặc giải pháp locking chứng minh tương đương.
Chỉ precheck không đủ. Race cùng report =>1 x 201 +1 x 409 REPORT_ALREADY_ASSIGNED;
race cùng team khác report =>1 x 201 +1 x 409 TEAM_BUSY. Nếu cả hai xung đột, ưu tiên
REPORT_ALREADY_ASSIGNED. Constraint violation được map sau rollback, không 500.
Team completion và assignment serialize theo invariant: assignment có thể 409 nếu
quan sát team còn BUSY, hoặc 201 sau completion; không hai active mission.

Disaster ACTIVE được đọc lại mỗi command verify/create mission thông qua published
queries, không cache. Nếu resolve commit trước lần đọc nghiệp vụ thì command trả
409 DISASTER_NOT_ACTIVE; nếu resolve sau lần đọc thì command được phép hoàn thành.
Đây là **check-time rule**, không hứa ACTIVE tại thời điểm commit mission; không
cross-module lock/transaction để chặn resolve. Mission có sẵn vẫn tiến triển/hoàn tất.
Report VERIFIED là terminal, không thể rút: điều này giữ report reference ổn định.

## Status và errors

Mỗi operation YAML liệt kê đúng success, error statuses và mã hợp lệ. Với request có
nhiều lỗi, xác thực trước, role trước, parse/validate shape, visibility rồi business.
E10 kiểm tra report tồn tại/PENDING trước disaster. E13: report tồn tại → VERIFIED →
ACTIVE disaster → team tồn tại → report uniqueness → team availability; kiểm tra DB
cuối vẫn bắt race. Missing input reference (E10 disaster; E13 report/team) =>404
NOT_FOUND; disaster reference biến mất trong snapshot VERIFIED là lỗi toàn vẹn 500,
không coi là ACTIVE. Không tự retry tạo entity, không idempotency-key trong MVP.

| Status | code | Ý nghĩa |
| --- | --- | --- |
| 400 | VALIDATION_ERROR | Shape/limits/query/UUID sai; `fieldErrors` mô tả field, không chứa giá trị đầu vào |
| 401 | UNAUTHENTICATED | Token thiếu/sai/hết hạn; `WWW-Authenticate: Bearer` |
| 401 | INVALID_CREDENTIALS | E01 sai credentials, user không tồn tại hoặc role ngoài MVP; cùng message |
| 403 | FORBIDDEN | Token hợp lệ nhưng role không được phép; không leak resource |
| 404 | NOT_FOUND | Không tồn tại/đã rút/ngoài ownership; cùng message |
| 409 | INVALID_TRANSITION, STALE_VERSION | State/version thay đổi hoặc hành động không hợp lệ |
| 409 | DISASTER_NOT_ACTIVE, REPORT_NOT_VERIFIED | Không đủ điều kiện nghiệp vụ |
| 409 | REPORT_ALREADY_ASSIGNED, TEAM_BUSY | Xung đột phân công |
| 415 | UNSUPPORTED_MEDIA_TYPE | Body không phải application/json |
| 500 | INTERNAL_ERROR | Lỗi ngoài dự kiến; message chung, không stacktrace |

`Error` luôn có timestamp/status/code/message/path/fieldErrors; path không gồm query.
Mọi API response chứa dữ liệu user và auth dùng `Cache-Control: no-store`; error cũng
không cache. `fieldErrors=[]` ngoài validation. Không trả token/password/SQL/exception
vào errors/logs. Client dựa code/status, không parse message. Unknown API default deny
ở security; health/Swagger/static không nằm trong 15 operations.

## Dữ liệu mẫu và JSON

Các UUID dưới đây chỉ là fixture tài liệu, không seed/migration P00. Hai citizen:
`00000000-0000-4000-8000-000000000001` / `citizen.one@example.test` và
`00000000-0000-4000-8000-000000000002` / `citizen.two@example.test`; authority:
`00000000-0000-4000-8000-000000000003` / `authority@example.test`.
Team thứ hai `00000000-0000-4000-8000-000000000008` / `Demo team 2`.
Không có mật khẩu mặc định: P01/demo profile lấy từ env, hash PasswordEncoder,
idempotent, không reset credential/role đã có; D1/demo seed team cùng nguyên tắc.
Không đưa fixture vào production, không endpoint seed.

Luồng mẫu dùng các body dưới đây: E05 tạo disaster ACTIVE; E07 tạo report PENDING;
E10 VerifyReportRequest gắn disaster; E13 tạo mission; E15 IN_PROGRESS rồi COMPLETED.
Các id trả về thật phải thay UUID mẫu. Snapshot Report/Mission bên dưới là tại lúc
tạo, không phải trạng thái sau toàn bộ luồng. Các field tùy trạng thái xem YAML và
bảng dưới; ví dụ rejection là nhánh khác của một report PENDING khác.

| DTO | Bắt buộc theo trạng thái | Phải vắng |
| --- | --- | --- |
| Report PENDING | Các trường common trong YAML | disasterId, verifiedAt, rejectionReason, rejectedAt |
| Report VERIFIED | disasterId, verifiedAt | rejectionReason, rejectedAt |
| Report REJECTED | rejectionReason, rejectedAt | disasterId, verifiedAt |
| Mission ASSIGNED | assignedAt, updatedAt + IDs/status | startedAt, completedAt |
| Mission IN_PROGRESS | startedAt + common | completedAt |
| Mission COMPLETED | startedAt, completedAt + common | Không |

### User

```json
{
  "id": "00000000-0000-4000-8000-000000000001",
  "email": "citizen.one@example.test",
  "role": "CITIZEN"
}
```

### LoginRequest

```json
{
  "email": "citizen.one@example.test",
  "password": "<from-demo-environment>"
}
```

### LoginResponse

```json
{
  "accessToken": "<issued-jwt-not-a-real-token>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "expiresAt": "2026-10-08T03:15:00Z",
  "user": {
    "id": "00000000-0000-4000-8000-000000000001",
    "email": "citizen.one@example.test",
    "role": "CITIZEN"
  }
}
```

### CreateDisasterRequest

```json
{
  "name": "Hanoi flood exercise",
  "type": "FLOOD",
  "severity": "HIGH",
  "description": "Training scenario only",
  "latitude": 21.028,
  "longitude": 105.834
}
```

### UpdateDisasterRequest

```json
{
  "expectedVersion": 0,
  "status": "RESOLVED"
}
```

### Disaster

```json
{
  "id": "00000000-0000-4000-8000-000000000004",
  "name": "Hanoi flood exercise",
  "type": "FLOOD",
  "severity": "HIGH",
  "description": "Training scenario only",
  "latitude": 21.028,
  "longitude": 105.834,
  "status": "ACTIVE",
  "version": 0,
  "createdAt": "2026-10-08T03:00:00Z",
  "updatedAt": "2026-10-08T03:00:00Z"
}
```

### CreateReportRequest

```json
{
  "type": "FLOOD",
  "description": "Water rising near the entrance",
  "latitude": 21.028,
  "longitude": 105.834
}
```

### Report

```json
{
  "id": "00000000-0000-4000-8000-000000000005",
  "reporterId": "00000000-0000-4000-8000-000000000001",
  "type": "FLOOD",
  "description": "Water rising near the entrance",
  "latitude": 21.028,
  "longitude": 105.834,
  "status": "PENDING",
  "createdAt": "2026-10-08T03:00:00Z",
  "updatedAt": "2026-10-08T03:00:00Z"
}
```

### VerifyReportRequest

```json
{
  "status": "VERIFIED",
  "disasterId": "00000000-0000-4000-8000-000000000004"
}
```

### RejectReportRequest

```json
{
  "status": "REJECTED",
  "rejectionReason": "Insufficient location details"
}
```

### VerificationRequest

```json
{
  "status": "VERIFIED",
  "disasterId": "00000000-0000-4000-8000-000000000004"
}
```

### RescueTeam

```json
{
  "id": "00000000-0000-4000-8000-000000000006",
  "name": "Demo team 1",
  "availability": "AVAILABLE"
}
```

### CreateMissionRequest

```json
{
  "reportId": "00000000-0000-4000-8000-000000000005",
  "teamId": "00000000-0000-4000-8000-000000000006"
}
```

### UpdateMissionStatusRequest

```json
{
  "status": "IN_PROGRESS"
}
```

### Mission

```json
{
  "id": "00000000-0000-4000-8000-000000000007",
  "reportId": "00000000-0000-4000-8000-000000000005",
  "teamId": "00000000-0000-4000-8000-000000000006",
  "status": "ASSIGNED",
  "assignedAt": "2026-10-08T03:00:00Z",
  "updatedAt": "2026-10-08T03:00:00Z"
}
```

### DisasterPage

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000004",
      "name": "Hanoi flood exercise",
      "type": "FLOOD",
      "severity": "HIGH",
      "description": "Training scenario only",
      "latitude": 21.028,
      "longitude": 105.834,
      "status": "ACTIVE",
      "version": 0,
      "createdAt": "2026-10-08T03:00:00Z",
      "updatedAt": "2026-10-08T03:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### ReportPage

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000005",
      "reporterId": "00000000-0000-4000-8000-000000000001",
      "type": "FLOOD",
      "description": "Water rising near the entrance",
      "latitude": 21.028,
      "longitude": 105.834,
      "status": "PENDING",
      "createdAt": "2026-10-08T03:00:00Z",
      "updatedAt": "2026-10-08T03:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### RescueTeamPage

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000006",
      "name": "Demo team 1",
      "availability": "AVAILABLE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### MissionPage

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000007",
      "reportId": "00000000-0000-4000-8000-000000000005",
      "teamId": "00000000-0000-4000-8000-000000000006",
      "status": "ASSIGNED",
      "assignedAt": "2026-10-08T03:00:00Z",
      "updatedAt": "2026-10-08T03:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### Error

```json
{
  "timestamp": "2026-10-08T03:00:00Z",
  "status": 409,
  "code": "TEAM_BUSY",
  "message": "Team already has an active mission.",
  "path": "/api/rescue-missions",
  "fieldErrors": []
}
```
