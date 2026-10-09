# Tiến độ Pha 1

Cập nhật 08/10/2026; task table khởi tạo TODO, P00 và P01 đã hoàn tất phạm vi task.
**DONE không đồng nghĩa đã review/merge** hay toàn bộ 15 APIs/6 screens đã implemented.
P01 chỉ có E01/E02/S01; các task sau vẫn TODO. ADR 005 ghi rõ phần đã triển khai.
Xem [P00 handoff](handoffs/P00.md) và [kế hoạch/dependencies](PROMPTS_PHA_1_4_NGUOI.md).

**Bổ sung 09/10/2026 — F00 nền tảng chạy được:** hoàn tất readiness/liveness,
script local, Compose smoke và full verification trên nhánh `feat/runnable-foundation`.
Xem [handoff F00](handoffs/F00.md) và [prompt chia commit](PROMPT_NEN_MONG.md).
F00 là công việc kỹ thuật trước MVP. Nhánh này đã tích hợp main tại `e4b449b`
(PR #1 chứa P01); giữ cả E01/E02/S01 và readiness/smoke. Xem kết quả kiểm tra
tích hợp trong handoff F00; không chuyển các task nghiệp vụ tiếp theo sang DONE.

| Task | Owner | Prerequisite đã merge | Trạng thái | Evidence / phần còn lại |
| --- | --- | --- | --- | --- |
| P00 | 1; cả nhóm review | Repo nền | DONE | OpenAPI + module contracts + ADR 005; validation trong handoff; chờ review/merge |
| P01 | 1 | P00 | DONE | [Handoff](handoffs/P01.md): E01/E02/S01; đã merge main qua PR #1 (`e4b449b`); kiểm tra tích hợp F00: 30 backend + 10 frontend tests pass |
| B1 | 2 | P01 | TODO | Report tạo/xem/ownership; migration chờ Disaster |
| C1 | 3 | P01 | TODO | Disaster backend + S05 |
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
