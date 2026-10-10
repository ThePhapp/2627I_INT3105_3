# Tiến độ Pha 1

Cập nhật 10/10/2026 sau đối chiếu main `ca3751d`, triển khai nhánh B2 và UI01-C1. Đây là bảng trạng thái, không phải kế hoạch riêng.
**DONE không đồng nghĩa đã review/merge** hay toàn bộ 15 APIs/6 screens đã implemented.
Nhánh B2 từ main `ca3751d` có E01–E11 và S01/S05, Flyway V1–V5 (chờ review/merge); xem [handoff B2](handoffs/B2.md).
Nhánh UI01-C1 từ B2 `2814b4f` đã cải tạo S05 và chờ review/merge; xem [handoff UI01-C1](handoffs/UI01-C1.md).
Xem [P00 handoff](handoffs/P00.md) và [kế hoạch/dependencies](PROMPTS_PHA_1_4_NGUOI.md).

**Bổ sung 09/10/2026 — F00 nền tảng chạy được:** hoàn tất readiness/liveness,
script local, Compose smoke và full verification trên nhánh `feat/runnable-foundation`.
Xem [handoff F00](handoffs/F00.md) để tra lịch sử kiểm chứng. Nền tảng đã merge qua
`853a532`; các nhãn F00–F04 không phải task mới trong kế hoạch. Chỉ dùng 16 task lõi + 3 checkpoint UI ở
[kế hoạch duy nhất](PROMPTS_PHA_1_4_NGUOI.md) để giao việc tiếp.

| Task | Owner | Prerequisite đã merge | Trạng thái | Evidence / phần còn lại |
| --- | --- | --- | --- | --- |
| P00 | 1; cả nhóm review | Repo nền | DONE | OpenAPI + module contracts + ADR 005; đã có trên main tại 0e19c36 |
| P01 | 1 | P00 | DONE | [Handoff](handoffs/P01.md): E01/E02/S01; đã merge main qua PR #1 (`e4b449b`); kiểm tra tích hợp F00: 30 backend + 10 frontend tests pass |
| B1 | 2 | P01; C1 migration trước B1 | DONE | [Handoff](handoffs/B1.md): E07–E09, V4, security production, ownership/PostGIS/upgrade/snapshot tests; local smoke pass; radius/verify/delete thuộc B2 |
| C1 | 3 | P01 | DONE | [Handoff](handoffs/C1.md): E03–E06/S05, V3, DisasterQuery; integration retest 62 backend +15 frontend +4 browser tests pass; list snapshot đã sửa theo P00 |
| UI01-A | 1 | P00, P01, B1, C1 | DONE | [Handoff](handoffs/UI01-A.md): shared theme/primitives + S01; đã merge PR #6 tại `78f164b` (implementation `7445913`); evidence 62 backend/17 frontend/8 browser tests; S05 patch giao UI01-C1 |
| B2 | 2 | B1, C1 | DONE | [Handoff](handoffs/B2.md): E10/E11, E08 radius, ReportingQuery, V5; 83 backend tests pass; nhánh feat/b2-report-verification, chờ review/merge |
| UI01-C1 | 3 | UI01-A, C1 | DONE | [Handoff](handoffs/UI01-C1.md): S05 workbench responsive 360/768/1280, focus dialog; 19 frontend +1 browser +83 backend tests pass; nhánh `feat/ui01-c1-disaster-ui`, chờ review/merge |
| D1 | 4 | B2, C1 | TODO | Rescue backend/DB race protection |
| B3 | 2 | B1, UI01-A; B2/D1 để hoàn tất | TODO | S02/S03 theo shared design system |
| C2 | 3 | B2, C1, UI01-A | TODO | S04 theo shared design system |
| D2 | 4 | D1, UI01-A | TODO | S06 theo shared design system |
| A1 | 1 | UI01-A; audit lại khi B2/D1 merge | TODO | Security/shared behavior; UI foundation thuộc UI01-A |
| A2 | 1 | UI01-A; hoàn tất UI runtime và X2/CI | TODO | Docker/frontend/CI |
| UI01-B | 1 + owners | B3, C2, D2, UI01-C1 | TODO | Audit đồng nhất/accessibility đủ 6 màn hình; gate trước X1 |
| X1 | 1 + owners | UI01-B, A1, A2 runtime | TODO | Tích hợp thật/feature freeze |
| X2 | 2 + 1 ghép CI | X1 | TODO | Browser E2E/hồi quy |
| D3 | 4 | X1 cho số đo; smoke D10 | TODO | Benchmark/Kaggle evidence |
| C3 | 3 | X1; bổ sung X2/D3 | TODO | Tài liệu/rubric/demo |
| A3 | 1 + nhóm | X2, C3, D3, UI01-B và mọi task trước | TODO | Audit cuối; không tuyên bố Pha 1 DONE khi thiếu evidence |

P00/P01/B1/C1/UI01-A đã tích hợp. B2 và UI01-C1 hoàn tất trên nhánh riêng, chờ review/merge.
Có thể giao A1/A2 lượt đầu (người 1) theo catalog, trên nhánh/clone riêng. B3 phần gửi/xem đã đủ
dependency nhưng người 2 cần tự sắp lịch với B2; B3 chỉ DONE sau B2/D1. C2 đợi B2;
D1 đợi B2; D2 đợi D1. Chưa đủ điều kiện chạy UI01-B/X1/X2/A3.
Chỉ thực hiện prompt được giao; bảng này không tự giao hoặc khởi chạy task tiếp theo.

## Rà soát tài liệu 10/10/2026

Đối chiếu trên nền `78f164b`, không triển khai task nghiệp vụ mới. Đồng bộ README,
runbooks, architecture/context, ledger và ghi chú tích hợp UI01-A; quy tắc nguồn tài
liệu và dependency nhiều lượt được bổ sung ngay trong catalog duy nhất. Handoff cũ
giữ evidence lịch sử, không chuyển số test cũ thành kiểm chứng mới.

Kiểm tra lần rà soát này: OpenAPI validator PASS (15 operations, 6 screens,
owners/consumers, 140 examples); 123 liên kết file Markdown nội bộ tồn tại;
catalog/progress cùng 19 task IDs; `git diff --check` PASS. Backend
`.\mvnw.cmd --no-transfer-progress clean verify` BUILD SUCCESS/exit0, 62 tests
(37 unit/architecture +25 integration), 0 failures/errors/skips, 1:23.
Maven host JDK23 với release21; backend image/CI pin Java21. Không chạy lại frontend
hoặc browser trong lượt chỉ sửa tài liệu này; evidence UI01-A vẫn ở handoff riêng.

Lượt review tiếp theo cùng ngày: làm rõ port/adapter và public signatures, missing/
deleted của ReportingQuery, migration B2 trên constraint V4, phối hợp security và
OpenAPI runtime trước DONE. Đồng bộ điều kiện A2 nhiều lượt và mốc D5; sửa hướng dẫn
clone theo remote hiện tại, giữ dữ liệu Compose khi thư mục clone cũ khác tên repo.
AGENTS chỉ được sửa mô tả bootstrap lịch sử; các quy tắc kiến trúc/test giữ nguyên.
Không thay schema/API wire contract hay tạo task mới. Chạy lại validator PASS,
123 file links hợp lệ, 19 task IDs khớp, diff check PASS; full clean verify
BUILD SUCCESS/exit0 lúc 20:13:58 +07:00, 1:15, 37 unit/architecture +25 integration,
0 failures/errors/skips. Frontend/browser không chạy lại vì chỉ thay tài liệu.
