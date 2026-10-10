# Pha 1 — published contracts và migration ledger

**Contracts P00; tích hợp B1/C1 09/10/2026:** Identity V2, Disaster V3 và Reporting V4 đã có;
DisasterQuery đã triển khai trong C1. ReportingQuery thuộc B2, chưa có trên main.
Theo [kế hoạch](../PROMPTS_PHA_1_4_NGUOI.md), [HTTP/UI contract](../api/phase1-contract.md)
và [ADR 005](../adr/005-phase1-mvp-auth-frontend.md).

## Chiều phụ thuộc và owner

```text
Rescue → Reporting → Disaster                 (chiều phụ thuộc module mục tiêu)
Consumer application → consumer-owned port
Consumer infrastructure adapter → provider application.contract
```

Disaster không biết Reporting/Rescue; Reporting không biết Rescue. Domain chỉ Java,
không phụ thuộc published DTO hay framework của module khác. Consumer dùng port của
mình, adapter gọi đúng interface published của provider; không repository/entity/SQL
join xuyên module, không HTTP nội bộ. Reporting mapper chuyển Disaster DTO sang DTO
của Reporting, không expose Disaster type cho Rescue. Interface/record/enums công
khai ở `<module>.application.contract`; chỉ chứa Java types, không Spring/JSON/JPA/
HTTP/Page. Chưa tạo package/class placeholder P00.

| Provider | Owner / task triển khai | Consumer / nhu cầu |
| --- | --- | --- |
| DisasterQuery | Người 3 / C1 | Người 2 / B2: kiểm tra ACTIVE để verify; dựng snapshot cho Rescue |
| ReportingQuery | Người 2 / B2 | Người 4 / D1: eligibility E13, visibility E14, batch report lookup |

Identity không cần published query cho MVP: security adapter cung cấp authenticated
user UUID và active role cho use case, không tin body và không kéo Spring principal
vào application/domain. E02 do Identity sở hữu. Không module gọi ngược Identity
persistence để xác thực mỗi resource. Public user/role administration và deletion
ngoài scope. Provisioning demo nội bộ thuộc P01 đã có; không cấm bootstrap đã chốt.

## Published signatures và trạng thái triển khai

DisasterQuery bên dưới đã có ở C1; ReportingQuery là contract đích của B2, chưa có
runtime. Các type public đặt ở file Java riêng khi triển khai; đoạn dưới mô tả
signature, không yêu cầu tạo placeholder hay một file chứa nhiều public types.

Trong `com.gdrn.disaster.application.contract`:

```java
public interface DisasterQuery {
    Optional<DisasterSnapshot> findById(UUID disasterId);
    Map<UUID, DisasterSnapshot> findByIds(Set<UUID> disasterIds);
}
public record DisasterSnapshot(UUID id, DisasterState status) {}
public enum DisasterState { ACTIVE, RESOLVED }
```

Trong `com.gdrn.reporting.application.contract`:

```java
public interface ReportingQuery {
    Optional<ReportSnapshot> findById(UUID reportId);
    Map<UUID, ReportSnapshot> findByIds(Set<UUID> reportIds);
}
public record ReportSnapshot(
    UUID id,
    UUID reporterId,
    ReportState status,
    Optional<LinkedDisaster> disaster
) {}
public record LinkedDisaster(UUID id, LinkedDisasterState status) {}
public enum ReportState { PENDING, VERIFIED, REJECTED }
public enum LinkedDisasterState { ACTIVE, RESOLVED }
```

DTO immutable; collections defensive copies/unmodifiable; UUID/enums/Optional không
null. ReportSnapshot.disaster chỉ present khi VERIFIED (bắt buộc khi VERIFIED),
absent với PENDING/REJECTED. LinkedDisasterState thuộc Reporting, map tường minh từ
DisasterState. Không expose reporter email, credentials, report text hay JPA entity.
Chưa thêm interface generic/god DTO/shared domain; không expose list tất cả report IDs.

Missing Disaster => Optional.empty; không có delete Disaster trong MVP, nhưng missing
ID vẫn hợp lệ như lookup không tìm thấy. Missing/soft-deleted Report => Optional.empty;
batch bỏ key missing/deleted, không null value/tombstone. Các lookup là **trusted
internal** query, không lọc theo người gọi: Rescue application chịu trách nhiệm
kiểm tra reporterId với citizen principal trước khi trả HTTP. Không expose query
interface trực tiếp thành endpoint.

Mỗi batch tối đa 100 distinct IDs, empty input =>empty immutable map, vượt limit/
null input/null member =>IllegalArgumentException (lỗi caller nội bộ, không business
absence). Map không bảo đảm iteration order; caller giữ thứ tự trang ban đầu. Không
duplicate input vì Set; `findById` tương đương batch singleton. Khi publish, owner
phải test các semantics này bằng application tests + adapter Testcontainers.

Reporting batch tải các reports một lần, thu distinct disaster IDs từ VERIFIED,
gọi DisasterQuery.findByIds tối đa một lần; không gọi findById từng dòng. Nếu reference
Disaster của VERIFIED không còn tồn tại: báo lỗi toàn vẹn qua lỗi application nội bộ,
không trả snapshot hợp lệ giả/không silently bỏ report. HTTP map 500 INTERNAL_ERROR.
DB lỗi/timeout cũng propagate như failure, không chuyển thành Optional.empty.
Những trạng thái khác không cần query Disaster. Snapshot mới mỗi lời gọi, không cache.

## Luồng query và biên transaction

- E10: Reporting kiểm tra report PENDING, đọc DisasterQuery; missing =>404, RESOLVED
  =>409, ACTIVE =>attempt atomic verification. Đã rút ở lần đọc đầu =>404; đã quan sát
  PENDING nhưng thua concurrent withdraw khi conditional write =>409 INVALID_TRANSITION.
- E13: Rescue đọc ReportingQuery mới trong command, lấy owner/status/disaster.
  Missing/deleted report =>404, non-VERIFIED =>409, RESOLVED =>409. Sau đó validate
  team và bảo vệ report/team uniqueness trong transaction Rescue.
- E14 có reportId: query Reporting trước để kiểm tra tồn tại/ownership; mismatch
  citizen =>404 ngay cả khi không có mission. Citizen thiếu reportId =>400.
- E14 authority không reportId: Rescue paginate/filter/count trên bảng của mình.
  Nếu kiểm tra/enrich report của trang, dùng một batch tối đa page size 100; không
  N+1, không lọc bỏ mission sau paginate để chữa ownership. E14 citizen luôn lọc
  reportId đã authorize tại DB. Không cần fan-out query tất cả report của citizen.
- E09 không gọi Rescue; S03 gọi E09 + E14 riêng. UI chịu trách nhiệm refresh để xem
  tiến độ, không realtime/event bus. Report HTTP DTO không chứa Mission.

Query trả committed snapshot ở thời điểm đọc; không trả lock/token transaction cho
caller. Các command sở hữu write transaction tại module của mình; không có atomic
snapshot nhiều module hoặc ACTIVE-at-commit guarantee. Resolve trước business query
=>chặn verify/assignment; resolve sau query =>cho phép. Không có điều kiện resolve
phải chờ Rescue. Mission cũ vẫn hoàn tất được. Quy tắc này phải được test với barrier/
latch, không dựa sleep may rủi. Race report/team và transition trong
[HTTP contract](../api/phase1-contract.md#trạng-thái-và-cạnh-tranh) là acceptance.

## ID ownership và toàn vẹn dữ liệu dự kiến

Requirements → domain rules → persistence mapping → schema → Flyway. Đây là quyết
định ownership/constraints cần dùng khi thiết kế use case, không DDL/schema đã tạo.

| Reference | Chủ dữ liệu/validation | Chiến lược MVP |
| --- | --- | --- |
| Report.reporterId → User.id | Reporting nhận UUID từ identity đã xác thực | Scalar UUID, immutable; không FK/JPA relation xuyên module; không xóa User trong MVP |
| Report.disasterId → Disaster.id | Reporting qua DisasterQuery tại verify | Scalar UUID, chỉ VERIFIED có link; không FK xuyên module; Disaster không delete |
| Mission.reportId → Report.id | Rescue qua ReportingQuery | Scalar UUID, unique vĩnh viễn; VERIFIED không rút/đổi; không FK xuyên module |
| Mission.teamId → Team.id | Rescue sở hữu cả hai | FK nội module + ràng buộc một active mission/team; team không delete |

Cross-module referential integrity được giữ bằng contracts và lifecycle không hard
delete, không cascade, không SQL join xuyên module. Nếu tương lai thêm user/disaster
deletion, phải ADR/contract mới và có migration/test trước; không giả định constraints
DB xuyên module đã có. SQL nội module có thể join các bảng do chính module sở hữu.
Soft-delete Report lưu dấu rút riêng, mọi query bình thường loại trừ; không đổi enum
ReportStatus thành WITHDRAWN. Timestamp/enum/version business mapping do owner thiết
kế trong feature. Team availability được suy từ mission, tránh hai nguồn sự thật.

## Sổ migration (khởi tạo 08/10/2026; đối chiếu main 10/10/2026)

Người 1 giữ ledger và cấp version **khi PR sẵn sàng merge**, sau khi đọc Flyway history
của nhánh tích hợp và danh sách migrations đã merge. V1–V4 dưới đây đã cấp/merge;
các slot **CHƯA CẤP không phải reservation số tiếp theo**. Mỗi feature có thể cần nhiều migration; không tự cấp số ở
nhánh riêng, không dùng ngày/owner prefix để lách thứ tự.

| Slot merge | Module / owner | Task | Version | Nội dung dự kiến / trạng thái |
| --- | --- | --- | --- | --- |
| Đã có | Technical / 1 | Bootstrap | V1__enable_postgis.sql | Chỉ extension PostGIS; giữ nguyên |
| 1 | Identity / 1 | P01 | V2__identity_accounts.sql | Đã merge qua e4b449b; identity_users + identity_credentials, không seed |
| 2 | Disaster / 3 | C1 | V3__disasters.sql | Đã tích hợp vào main qua bacf960; Disaster lifecycle/expected-version atomic update, sau V2 |
| 3 | Reporting / 2; tích hợp / 1 | B1 | V4__reporting_reports.sql | Đã tích hợp/chốt tại c850103 trên nền bacf960 có V1–V3; PENDING/geography, không seed/FK xuyên module |
| Tiếp theo | Reporting / 2 | B2 | V5__reporting_verification_withdrawal.sql | Nhánh B2 từ main ca3751d: đối chiếu V1–V4 trước khi cấp V5; mở lifecycle constraints, metadata/withdrawn_at/version và GiST cho E08; giữ nguyên V4, chờ review/merge |
| 4 | Rescue / 4 | D1 | CHƯA CẤP | Team/mission/unique constraints sau B2 |

B1 domain/application có thể làm song song C1 sau P01, nhưng persistence migration
B1 phải chờ C1 merge rồi mới cấp số. Mọi migration Reporting B2 phải vào trước Rescue
D1; B2 không được dùng số cũ chen sau số đã áp dụng cao hơn. Feature không đổi schema
không cần migration. Sau vòng đầu, cấp version mới tăng đơn điệu theo thứ tự merge
thực tế, ghi thêm dòng ledger trong chính PR.

Trước merge: owner bàn giao nhu cầu schema/constraints và tests; Người 1 ghi version,
filename, PR/commit và dependencies thực tế vào ledger, kiểm tra collision/order;
owner chạy migration từ DB sạch và upgrade từ version trước bằng Testcontainers.
Không bật `out-of-order`, không sửa/rename/xóa migration đã merge hoặc đã áp dụng
(kể cả nhánh local đã chạy); sửa bằng migration mới. Không repair checksum để né lỗi.
Nếu migration nháp có vấn đề về thứ tự, phối hợp Người 1 trước khi merge; không drop
DB/volume đang dùng. P00 không cấp version mới và không tạo migration business.

P01 cập nhật ledger: User giữ id/email/role; credential hash là concern application/
infrastructure, không nằm trong User. ORM maps hai entity riêng, credential có FK nội
Identity tới user; email unique. V2 kế tiếp V1, đã merge qua e4b449b. V3 Disaster
đã có trên main tại 3220765; V4 Reporting được cấp trong tích hợp trên nền bacf960
sau khi đối chiếu code và Flyway history. B2 phải nhận version mới khi sẵn sàng. [P01 handoff](../handoffs/P01.md) ghi validation lịch sử.

Demo data được mô tả trong [HTTP contract](../api/phase1-contract.md#dữ-liệu-mẫu-và-json)
chỉ để chuẩn bị. P01/D1 dùng controlled demo profile idempotent, credential từ env;
không endpoint seed, không tài khoản mặc định trong production/migration.
