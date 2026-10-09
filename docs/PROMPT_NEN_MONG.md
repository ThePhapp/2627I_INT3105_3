# Prompt làm nền móng chạy được, chia việc và commit

Ngày 09/10/2026. Đây là lát cắt nhỏ trước MVP, dùng lại bootstrap hiện có.
Không thay thế [16 task Pha 1](PROMPTS_PHA_1_4_NGUOI.md) hoặc mở rộng 15 API/6 UI.
“Module” ở đây là module nghiệp vụ và phần việc nhỏ; không phải Maven submodule
hay microservice. Một ứng dụng Spring Boot tiếp tục chứa các module `com.gdrn`.

## Prompt dùng ngay cho nền tảng

```text
Làm phần nền móng có thể chạy được cho GDRN, chưa làm toàn bộ sản phẩm.

Đọc AGENTS.md, GDRN_CODEX_PROJECT_CONTEXT.md,
docs/architecture/architecture-overview.md và code thật. Đọc thêm kế hoạch MVP,
docs/api/phase1-contract.md, docs/architecture/phase1-module-contracts.md và
docs/phase1-progress.md để phân biệt đã có với dự kiến.

Kiểm tra git status trước khi sửa. Tạo nhánh feat/runnable-foundation từ nhánh
hiện tại; nếu tên đã tồn tại thì chọn hậu tố mới, không reset/ghi đè nhánh cũ.
Giữ nguyên thay đổi của người dùng. Thực hiện tuần tự bốn phần dưới đây,
mỗi phần một commit có ý nghĩa, chỉ stage đúng file của phần đó:

F01 — docs(plan): ghi scope, tiêu chí hoàn thành, prompt và bảng chia module.
F02 — feat(platform): bật liveness/readiness; readiness kiểm tra cả database,
liveness chỉ phản ánh ứng dụng. Docker healthcheck dùng readiness. Có test
HTTP với PostGIS thật cho trạng thái UP và readiness từ chối phục vụ; không
lộ cấu hình DB. Cập nhật tài liệu quyết định kỹ thuật.
F03 — chore(dev): script PowerShell init/up/status/smoke/down/verify; chạy từ
bất kỳ thư mục nào. Init sinh mật khẩu local ngẫu nhiên vào .env bị Git ignore,
không in secret, không ghi đè .env có sẵn. Up kiểm tra Docker Linux, validate
Compose, build và chờ healthy. Smoke kiểm tra health/readiness/liveness,
OpenAPI và Swagger thật. Down giữ volume. Verify chạy mvnw.cmd clean verify,
không skip integration tests. Native command lỗi phải trả exit code thất bại.
F04 — docs(handoff): chạy full verify và Compose thật, smoke, ghi kết quả thực
tế, cách chạy, danh sách commit và phần chưa triển khai. Cập nhật README.

Giữ Modular Monolith, domain plain Java và ports/adapters. Không tạo business
placeholder, bảng mẫu, JWT giả, frontend giả, broker/Redis hoặc migration khi
chưa có use case. Không tự chạy các task MVP bên dưới, không push/merge/deploy.
Không xóa volume hoặc reset DB. Docker chưa chạy thì thử mở Docker Desktop;
nếu môi trường vẫn chặn, ghi lệnh/lỗi thật, không báo build xanh hoặc chạy được.

Hoàn thành khi: build đầy đủ qua, backend+PostGIS khởi chạy healthy, Swagger
truy cập được, scripts dùng được và có hướng dẫn cùng commit riêng từng phần.
Nêu rõ chưa có login, frontend và API nghiệp vụ; nền móng không phải MVP hoàn tất.
```

## Các phần nghiệp vụ để làm tiếp

Chỉ chạy khi được giao task tiếp theo và dependency đã có trên nhánh làm việc.
Mỗi hàng là phạm vi commit, không phải một ứng dụng riêng. Các hàng thuộc cùng
feature nên đi chung một PR để code, migration và tests được review cùng nhau.
Không đánh dấu task P01/C1/B1/B2/D1 hoàn tất chỉ vì một hàng đã xong.

| Thứ tự | Module / task gốc | Phần nhỏ và commit gợi ý | Tiêu chí kiểm tra |
| --- | --- | --- | --- |
| 1 | Identity / P01 | `feat(identity): add credential and login use cases` | Tái dùng User/EmailAddress/Role; ports; unit tests với mocked ports |
| 2 | Identity / P01 | `feat(identity): persist credentials and provision demo users` | Mapping riêng, migration theo ledger; seed profile từ env, idempotent; Testcontainers |
| 3 | Identity / P01 | `feat(identity): expose JWT login and current user` | E01/E02, BCrypt, JWT 900s, 401/403/error contract, role CITIZEN/AUTHORITY |
| 4 | SPA / P01 | `feat(web): add authenticated app shell and login` | S01/API thật, token chỉ memory, reload login lại, typecheck/test/build |
| 5 | Disaster / C1 | `feat(disaster): implement lifecycle and use cases` | ACTIVE → RESOLVED; immutable sau resolved, version conflict; domain/application tests |
| 6 | Disaster / C1 | `feat(disaster): add persistence and published queries` | Migration được cấp theo ledger; DisasterQuery; adapter + concurrent-write tests |
| 7 | Disaster / C1 | `feat(disaster): expose APIs and operations screen` | E03–E06 + S05, authorization/validation, UI chạy API thật |
| 8 | Reporting / B1 | `feat(reporting): implement submission and owner queries` | PENDING, principal quyết định owner, E07–E09, persistence sau Disaster |
| 9 | Reporting / B2 | `feat(reporting): add verification withdrawal and radius search` | E10/E11, PENDING race, PostGIS mét, ReportingQuery, contract tests |
| 10 | Rescue / D1 | `feat(rescue): implement mission and team rules` | ASSIGNED → IN_PROGRESS → COMPLETED; availability suy ra; tests không Spring |
| 11 | Rescue / D1 | `feat(rescue): persist and expose assignment workflow` | E12–E15, unique report/active team, concurrency tests và ownership |
| 12 | SPA / B3,C2,D2 | Mỗi màn S02–S06 một commit `feat(web): ...` | Loading/error/empty, real API, role guards, không nút giả |
| 13 | Tích hợp / X1,X2 | `test(e2e): cover citizen to rescue workflow` | Login → report → verify → assign → complete; negative authorization |

Resource/Alert, Geo risk, map và events vẫn là backlog. Query bán kính nằm trong
Reporting infrastructure. Chiều contract là Rescue → Reporting → Disaster.
Việc chia commit không cấp trước số Flyway; theo đúng migration ledger của repo.

## Mẫu prompt cho từng phần nhỏ

```text
Thực hiện phần <hàng + tên> trong docs/PROMPT_NEN_MONG.md, thuộc task <P01/C1/...>.
Đọc AGENTS.md, context, architecture overview, contract HTTP/YAML, module contracts,
migration ledger và prompt task gốc trong docs/PROMPTS_PHA_1_4_NGUOI.md.
Kiểm tra dependency bằng code và git, không dựa riêng vào bảng tiến độ.

Tạo nhánh feat/<module>-<use-case> từ code đã có dependency. Chỉ làm phạm vi hàng
được giao. Viết yêu cầu/domain trước ports/application, rồi mapping/schema/migration
và HTTP. Không đổi public contract hoặc truy cập infrastructure module khác.
Giữ domain plain Java; domain tests không Spring, application tests mock ports,
adapter/security tests dùng PostGIS Testcontainers. Không stub runtime để báo xong.

Chạy .\mvnw.cmd clean verify; nếu có frontend, chạy typecheck, test không watch và
build, kiểm tra UI với backend thật. Cập nhật handoff của task, ghi rõ phần nào
còn lại. Review git diff, stage file đúng scope và tạo commit với tên ở bảng.
Không push/merge. Báo commit, lệnh chạy, kết quả test và blocker thực tế.
```
