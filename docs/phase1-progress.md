# Tiến độ Pha 1

Cập nhật 09/10/2026 sau tích hợp B1/C1. Đây là bảng trạng thái, không phải kế hoạch riêng.
**DONE không đồng nghĩa đã review/merge** hay toàn bộ 15 APIs/6 screens đã implemented.
Runtime có E01–E09 và S01/S05, Flyway V1–V4; xem [kết quả tích hợp](handoffs/B1-C1-integration.md).
Xem [P00 handoff](handoffs/P00.md) và [kế hoạch/dependencies](PROMPTS_PHA_1_4_NGUOI.md).

**Bổ sung 09/10/2026 — F00 nền tảng chạy được:** hoàn tất readiness/liveness,
script local, Compose smoke và full verification trên nhánh `feat/runnable-foundation`.
Xem [handoff F00](handoffs/F00.md) để tra lịch sử kiểm chứng. Nền tảng đã merge qua
`853a532`; các nhãn F00–F04 không phải task mới trong kế hoạch. Chỉ dùng 16 task ở
[kế hoạch duy nhất](PROMPTS_PHA_1_4_NGUOI.md) để giao việc tiếp.

| Task | Owner | Prerequisite đã merge | Trạng thái | Evidence / phần còn lại |
| --- | --- | --- | --- | --- |
| P00 | 1; cả nhóm review | Repo nền | DONE | OpenAPI + module contracts + ADR 005; đã có trên main tại 0e19c36 |
| P01 | 1 | P00 | DONE | [Handoff](handoffs/P01.md): E01/E02/S01; đã merge main qua PR #1 (`e4b449b`); kiểm tra tích hợp F00: 30 backend + 10 frontend tests pass |
| B1 | 2 | P01; C1 migration trước B1 | DONE | [Handoff](handoffs/B1.md): E07–E09, V4, security production, ownership/PostGIS/upgrade/snapshot tests; local smoke pass; radius/verify/delete thuộc B2 |
| C1 | 3 | P01 | DONE | [Handoff](handoffs/C1.md): E03–E06/S05, V3, DisasterQuery; integration retest 62 backend +15 frontend +4 browser tests pass; list snapshot đã sửa theo P00 |
| B2 | 2 | B1, C1 | TODO | Verify/withdraw/spatial, ReportingQuery |
| D1 | 4 | B2, C1 | TODO | Rescue backend/DB race protection |
| B3 | 2 | B1; B2/D1 để hoàn tất | TODO | S02/S03 |
| C2 | 3 | B2, C1 | TODO | S04 |
| D2 | 4 | D1 | TODO | S06; B2 cũng có theo dependency D1 |
| A1 | 1 | P01; audit lại khi B2/D1 merge | TODO | Security/shared frontend |
| A2 | 1 | P01; hoàn tất UI runtime và X2/CI | TODO | Docker/frontend/CI |
| X1 | 1 + owners | B3, C2, D2, A1, A2 runtime | TODO | Tích hợp thật/feature freeze |
| X2 | 2 + 1 ghép CI | X1 | TODO | Browser E2E/hồi quy |
| D3 | 4 | X1 cho số đo; smoke D10 | TODO | Benchmark/Kaggle evidence |
| C3 | 3 | X1; bổ sung X2/D3 | TODO | Tài liệu/rubric/demo |
| A3 | 1 + nhóm | X2, C3, D3 và mọi task trước | TODO | Audit cuối; không tuyên bố Pha 1 DONE khi thiếu evidence |

P01 chỉ được bắt đầu sau khi P00 được review/merge vào nhánh làm việc. P00 không tự
push/merge, cấp migration version hay chạy prompt tiếp theo.
