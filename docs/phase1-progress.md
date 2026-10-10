# Tiến độ Pha 1

Cập nhật 10/10/2026 sau thực hiện UI01-A. Đây là bảng trạng thái, không phải kế hoạch riêng.
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
| UI01-A | 1 | P00, P01, B1, C1 | DONE | [Handoff](handoffs/UI01-A.md): shared theme/primitives + S01; 62 backend/17 frontend/8 browser tests pass; working tree chưa review/merge; S05 patch giao UI01-C1 |
| B2 | 2 | B1, C1 | TODO | Verify/withdraw/spatial, ReportingQuery |
| UI01-C1 | 3 | UI01-A, C1 | TODO | Cải tạo S05; giữ nguyên E03–E06/route/semantics |
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

P00, P01, B1 và C1 đã hoàn tất; không chạy lại các prompt này. Bước mở tiếp theo là
review/merge UI01-A và triển khai B2 trên nhánh/clone riêng. UI01-C1 chỉ bắt đầu sau khi UI01-A đã review/merge;
các task còn lại tuân theo dependency trong `docs/PROMPTS_PHA_1_4_NGUOI.md`.
