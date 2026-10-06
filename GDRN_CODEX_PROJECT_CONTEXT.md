# GDRN — Codex Project Context & Engineering Specification

> **Project:** Global Disaster Response Network (GDRN)  
> **Tên tiếng Việt:** Hệ thống Điều phối và Ứng phó Thảm họa Toàn cầu  
> **Course:** Kiến trúc phần mềm  
> **Team size:** 4 thành viên  
> **Development window:** ~1 tháng cho Pha 1  
> **Purpose of this file:** Đây là nguồn ngữ cảnh kỹ thuật chính cho Codex/AI coding agent khi phân tích, thiết kế, sinh hoặc sửa code trong repository GDRN.

---

## 0. Instructions for Codex / Coding Agent

Khi làm việc trong repository này, phải tuân thủ các nguyên tắc sau:

1. **Không phá vỡ module boundaries.** Không truy cập trực tiếp persistence/JPA entity/repository implementation của module khác.
2. **Domain layer phải framework-independent.** Không import Spring Web, Spring Data, Hibernate/JPA hoặc database library vào domain.
3. **Không biến project thành CRUD thuần túy.** Business rule phải nằm trong domain/application phù hợp.
4. **Pha 1 là Modular Monolith.** Không tự ý tách microservice.
5. **Pha 1 không dùng RabbitMQ/Kafka/Redis làm dependency bắt buộc**, trừ khi yêu cầu được cập nhật rõ ràng. Event trong Pha 1 mặc định là internal/in-process domain event.
6. Controller chỉ chịu trách nhiệm HTTP concerns: parse/validate request, gọi use case, map response.
7. Application layer orchestration use cases; không chứa chi tiết HTTP hay persistence framework.
8. Infrastructure implements ports/interfaces được định nghĩa ở phía trong.
9. Ưu tiên dependency inversion: outer layers phụ thuộc vào inner abstractions, không ngược lại.
10. Mọi thay đổi quan trọng về architecture phải giữ compatibility với mục tiêu benchmark Pha 1 → Pha 2.
11. Không thêm công nghệ chỉ vì “production-like”. Mọi dependency mới cần có lý do.
12. Khi yêu cầu chưa rõ, ưu tiên implementation nhỏ nhất phù hợp architecture hiện tại thay vì tự mở rộng scope.
13. Code mới phải có test phù hợp với mức độ rủi ro của thay đổi.
14. Không hard-code secret, JWT secret, password, database credential hoặc environment-specific configuration.
15. API errors phải dùng format thống nhất của project.
16. Database schema changes phải qua migration, không dựa vào auto schema mutation trong môi trường chính.

---

# 1. Project Overview

GDRN mô phỏng một nền tảng backend quy mô lớn phục vụ điều phối ứng phó thảm họa.

Hệ thống hỗ trợ các nhóm nghiệp vụ chính:

- Quản lý thảm họa.
- Tiếp nhận báo cáo sự cố từ người dân/nguồn dữ liệu mô phỏng.
- Quản lý yêu cầu cứu hộ.
- Điều phối đội cứu hộ và nhiệm vụ cứu hộ.
- Quản lý và phân bổ tài nguyên khẩn cấp.
- Xác định khu vực ảnh hưởng bằng dữ liệu địa lý.
- Phát hành cảnh báo khẩn cấp.
- Quản lý danh tính, đăng nhập và phân quyền.

Các loại thảm họa ban đầu:

- EARTHQUAKE
- FLOOD
- TYPHOON
- WILDFIRE
- TSUNAMI

Project là mô phỏng phục vụ môn Kiến trúc phần mềm, không phải hệ thống cứu hộ thực tế.

---

# 2. Two-Phase Architecture Strategy

## Phase 1 — Baseline

Pha 1 sử dụng:

- Java
- Spring Boot
- Modular Monolith
- Domain-Driven Design concepts
- Clean / Hexagonal Architecture
- REST + JSON
- Spring Security + JWT
- PostgreSQL
- PostGIS
- Spring Data JPA / Hibernate ở infrastructure layer
- Flyway
- OpenAPI / Swagger
- Docker / Docker Compose
- JUnit / Mockito / Testcontainers
- k6 cho load testing

Mục tiêu Pha 1:

1. Có hệ thống chức năng hoàn chỉnh ở mức cơ bản.
2. Có architecture boundaries rõ ràng.
3. Có baseline performance cố định.
4. Có thể xác định bottleneck bằng benchmark.
5. Tạo cơ sở khoa học cho quyết định cải tiến Pha 2.

## Phase 2 — Candidate Improvements

Pha 2 chưa được mặc định là implementation bắt buộc. Chỉ áp dụng sau khi đánh giá Pha 1.

Candidate technologies/patterns:

- Event-Driven Architecture
- RabbitMQ
- Async workers
- Redis caching
- Retry
- Dead Letter Queue
- Idempotent consumers
- Prometheus
- Grafana
- Selective service/worker extraction

Không mặc định chuyển toàn bộ hệ thống sang microservices.

---

# 3. Architectural Style — Phase 1

Kiến trúc tổng thể:

```text
Client
  |
  | HTTP / REST / JSON
  v
API Layer
  |
  v
Application Layer
  |
  v
Domain Layer
  ^
  |
Ports / Interfaces
  ^
  |
Infrastructure Layer
  |
  v
PostgreSQL + PostGIS
```

Dependency rule:

```text
API -------------> Application -------> Domain
Infrastructure ------------------------> Domain/Application ports

Domain -X-> Spring
Domain -X-> JPA/Hibernate
Domain -X-> PostgreSQL
Domain -X-> HTTP
```

Domain là trung tâm của hệ thống.

---

# 4. Modular Monolith Boundaries

Các bounded context/module chính:

```text
gdrn
├── identity
├── disaster
├── reporting
├── rescue
├── alert
├── resource
├── geo
└── shared
```

Không dùng `shared` như nơi chứa mọi thứ. Chỉ đưa vào shared những primitive/cross-cutting abstraction thật sự dùng chung và ổn định.

Mỗi business module nên có dạng:

```text
<module>/
├── api/
│   ├── controller/
│   ├── request/
│   └── response/
├── application/
│   ├── command/
│   ├── query/
│   ├── usecase/
│   └── dto/
├── domain/
│   ├── model/
│   ├── service/
│   ├── event/
│   └── repository/
└── infrastructure/
    ├── persistence/
    ├── mapper/
    └── configuration/
```

Tên package cụ thể có thể thay đổi nếu repository đã có convention tốt hơn, nhưng dependency direction không được thay đổi.

---

# 5. Module Responsibilities

## 5.1 Identity & Access

Responsibilities:

- User registration.
- Login.
- JWT issuance/validation.
- Role-based authorization.
- User identity.

Initial roles:

```text
CITIZEN
RESPONDER
AUTHORITY
ADMIN
```

Security phải được xử lý tập trung qua Spring Security filter chain/interceptor mechanism, không lặp authentication logic trong controller.

---

## 5.2 Disaster Management

Aggregate/domain concepts:

```text
Disaster
DisasterId
DisasterType
Severity
DisasterStatus
AffectedArea
```

Lifecycle tham khảo:

```text
DETECTED
   ↓
VERIFIED
   ↓
ACTIVE
   ↓
CONTROLLED
   ↓
RESOLVED
```

Business rules cần validate transition hợp lệ. Không cho phép arbitrary status update nếu transition không hợp lệ.

Responsibilities:

- Create disaster.
- Verify disaster.
- Change severity.
- Update lifecycle.
- Query active disasters.
- Associate affected geographic area.

---

## 5.3 Incident Reporting

Concepts:

```text
IncidentReport
ReportStatus
Reporter
Evidence metadata
GeoLocation
```

Responsibilities:

- Citizen submits report.
- Verify/reject report.
- Detect basic duplicate reports.
- Associate report with known disaster when appropriate.
- Produce internal domain event when relevant.

Example request:

```json
{
  "type": "FLOOD",
  "latitude": 21.028,
  "longitude": 105.834,
  "description": "Water level rising rapidly"
}
```

---

## 5.4 Rescue Coordination

Concepts:

```text
RescueRequest
RescueRequestStatus
RescueTeam
TeamCapability
RescueMission
MissionStatus
PriorityScore
```

Lifecycle tham khảo:

```text
PENDING
   ↓
VERIFIED
   ↓
ASSIGNED
   ↓
IN_PROGRESS
   ↓
RESCUED
```

Core business flow:

```text
Rescue Request
      ↓
Priority Calculation
      ↓
Candidate Teams
      ↓
Distance Calculation
      ↓
Capability Matching
      ↓
Availability Check
      ↓
Team Assignment
      ↓
Rescue Mission
```

Priority có thể xem xét:

- Disaster severity.
- Number of people at risk.
- Waiting time.
- Vulnerable people.
- Geographic risk.

Không hard-code một công thức phức tạp nếu requirements chưa chốt; thiết kế để policy có thể thay đổi/test độc lập.

---

## 5.5 Emergency Alert

Concepts:

```text
EmergencyAlert
AlertSeverity
AlertStatus
TargetArea
PublishedAt
ExpiresAt
```

Responsibilities:

- Create alert.
- Determine target area from supplied risk/disaster information.
- Publish alert.
- Expire/cancel alert.
- Query active alerts.

Possible flow:

```text
Disaster / Risk
      ↓
Affected Area
      ↓
Emergency Alert
      ↓
Publish
```

External SMS/email/push integration is not required in Phase 1. Simulation/logging is sufficient unless requirements change.

---

## 5.6 Resource Management

Concepts:

```text
EmergencyResource
ResourceType
ResourceInventory
Shelter
Vehicle
ResourceAllocation
```

Initial resource types:

- WATER
- FOOD
- MEDICAL_SUPPLY
- AMBULANCE
- RESCUE_VEHICLE
- RESCUE_EQUIPMENT
- SHELTER_CAPACITY

Responsibilities:

- Register resource/inventory.
- Query availability.
- Allocate resources to disaster/mission/area.
- Prevent invalid over-allocation.
- Release/consume allocation as defined by business rules.

Resource allocation should contain actual domain rules rather than only CRUD.

---

## 5.7 Geo / Risk

Use PostgreSQL + PostGIS.

Concepts:

```text
GeoLocation
GeoRadius
EmergencyZone
AffectedArea
RiskLevel
```

Responsibilities:

- Distance queries.
- Nearby rescue requests.
- Nearby disasters.
- Determine resources/reports within affected area.
- Spatial filtering.
- Basic risk assessment policies.

Example use case:

```text
Find all Rescue Requests within 10 km of a disaster epicenter.
```

Spatial queries belong in infrastructure adapters/repositories; domain/application receives domain-friendly results.

---

# 6. Cross-Module Communication Rules

Forbidden pattern:

```text
RescueService
    ↓
DisasterJpaRepository
```

Preferred approaches:

```text
Rescue Application
    ↓
DisasterQueryPort / Published Application API
    ↓
Disaster Module
```

hoặc internal domain/application events khi temporal coupling không cần thiết.

Không expose persistence entity như cross-module contract.

Cross-module DTO/contracts phải nhỏ và ổn định.

---

# 7. Domain Events — Phase 1

Pha 1 có thể sử dụng internal/in-process events.

Candidate events:

```text
DisasterDetected
DisasterVerified
DisasterEscalated
CitizenReportSubmitted
CitizenReportVerified
RescueRequested
RescueTeamAssigned
RescueCompleted
HighRiskAreaDetected
EmergencyAlertCreated
EmergencyAlertPublished
ResourceAllocated
```

Pha 1 **không yêu cầu external broker**.

Default behavior:

```text
Domain operation
    ↓
Internal event
    ↓
In-process handler
```

Nếu transaction semantics quan trọng, implementation phải ghi rõ handler chạy trước/sau commit và có test tương ứng.

---

# 8. REST API Baseline

API prefix:

```text
/api
```

## Auth

```text
POST /api/auth/register
POST /api/auth/login
```

## Disasters

```text
GET    /api/disasters
GET    /api/disasters/{id}
POST   /api/disasters
PATCH  /api/disasters/{id}
DELETE /api/disasters/{id}
```

## Reports

```text
GET    /api/reports
GET    /api/reports/{id}
POST   /api/reports
PATCH  /api/reports/{id}/verify
DELETE /api/reports/{id}
```

## Rescue

```text
POST   /api/rescue-requests
GET    /api/rescue-requests
GET    /api/rescue-requests/{id}
GET    /api/rescue-requests/me
POST   /api/rescue-requests/{id}/assign
PATCH  /api/rescue-requests/{id}/status
DELETE /api/rescue-requests/{id}
```

## Resources

```text
GET  /api/resources
POST /api/resources
POST /api/resource-allocations
GET  /api/disasters/{id}/resources
```

## Alerts

```text
GET   /api/alerts
GET   /api/alerts/{id}
POST  /api/alerts
PATCH /api/alerts/{id}/publish
```

## Geo

```text
GET /api/disasters/{id}/nearby-rescue-requests
GET /api/disasters/{id}/affected-resources
GET /api/disasters/nearby
```

API list là baseline, không phải yêu cầu phải implement tất cả ngay lập tức.

---

# 9. API Design Conventions

- JSON request/response.
- RESTful resource naming.
- Dùng plural nouns cho collection.
- Không expose JPA entities trực tiếp.
- Request DTO và response DTO tách khỏi persistence model.
- Validation errors phải trả response nhất quán.
- Sử dụng HTTP status code đúng semantics.
- Pagination cho collection có khả năng lớn.
- Timestamp dùng ISO-8601.
- API documentation phải xuất hiện trong OpenAPI/Swagger.

Suggested error shape:

```json
{
  "timestamp": "2026-10-06T12:00:00Z",
  "status": 400,
  "code": "INVALID_DISASTER_STATUS_TRANSITION",
  "message": "Cannot transition disaster from RESOLVED to ACTIVE",
  "path": "/api/disasters/123"
}
```

Không leak stack trace/internal exception ra client.

---

# 10. Security

Authentication architecture:

```text
HTTP Request
     ↓
JWT Authentication Filter
     ↓
Spring Security Context
     ↓
Authorization
     ↓
Controller
```

Requirements:

- Password phải hash bằng password encoder phù hợp.
- JWT secret lấy từ environment/config secret.
- Protected endpoints phải dùng centralized security.
- Authorization theo role/use case.
- Không tin user ID từ request nếu có thể lấy identity từ authenticated principal.
- Không log password/token đầy đủ.

Minimum assignment requirement phải được đáp ứng:

- Có login.
- Ít nhất một GET endpoint yêu cầu authentication.
- Ít nhất một POST endpoint yêu cầu authentication.

---

# 11. Persistence

Database:

```text
PostgreSQL + PostGIS
```

Rules:

- JPA/Hibernate chỉ thuộc infrastructure.
- Domain object không bắt buộc đồng nhất với JPA entity.
- Mapping giữa persistence model và domain model ở infrastructure.
- Schema changes qua Flyway.
- Index dựa trên query patterns thực tế.
- Geo columns sử dụng PostGIS type phù hợp.
- Spatial index được cân nhắc cho query khoảng cách/khu vực.

Không tối ưu premature; baseline phải đo được trước khi tuning mạnh.

---

# 12. Suggested Database Areas

Không coi đây là schema cuối cùng, nhưng các aggregate/table area có thể gồm:

```text
users
roles
user_roles

disasters
incident_reports

rescue_requests
rescue_teams
rescue_team_capabilities
rescue_missions

emergency_alerts

resources
resource_inventories
resource_allocations
shelters
```

Geo information có thể nằm trực tiếp trong aggregate phù hợp hoặc qua value/object mapping tùy thiết kế.

Không tạo generic mega-table cho mọi domain object.

---

# 13. Testing Strategy

## Domain Unit Tests

Ưu tiên cho:

- Status transition.
- Priority calculation.
- Resource allocation rules.
- Assignment policy.
- Risk policy.

Domain tests không cần Spring context nếu có thể.

## Application Tests

Test use case orchestration bằng mock/fake ports khi phù hợp.

## Integration Tests

Sử dụng Testcontainers cho:

- PostgreSQL.
- PostGIS behavior.
- Repository adapters.
- Security/API integration quan trọng.

## API Tests

Kiểm tra:

- Authentication.
- Authorization.
- Validation.
- Status codes.
- Response contracts.

Không chỉ test happy path.

---

# 14. Definition of Done for a Feature

Một feature chỉ được coi là hoàn thành khi phù hợp các mục liên quan:

- Domain/business rule đã implement.
- Module boundary không bị phá.
- API contract rõ ràng.
- Validation có xử lý.
- Authorization đúng nếu endpoint protected.
- Persistence migration có nếu schema thay đổi.
- Unit/integration tests cần thiết đã có.
- Swagger/OpenAPI cập nhật.
- Dockerized environment vẫn chạy.
- Không có secret hard-coded.
- README/spec cập nhật nếu architecture/API thay đổi đáng kể.

---

# 15. Phase 1 Baseline Flow

Ví dụ synchronous report flow:

```text
POST /reports
      ↓
Authenticate
      ↓
Validate
      ↓
Create Incident Report
      ↓
Risk Analysis
      ↓
Associate / Update Disaster
      ↓
Generate Alert if needed
      ↓
Persist
      ↓
HTTP Response
```

Pha 1 cố ý giữ nhiều flow synchronous để tạo baseline dễ hiểu và benchmark.

Không artificial delay chỉ để làm Pha 1 trông chậm.

---

# 16. Performance Evaluation

Benchmark phải chạy trên cấu hình phần cứng cố định, ưu tiên Kaggle CPU theo yêu cầu môn học.

Candidate workloads:

```text
100 concurrent users
250 concurrent users
500 concurrent users
1000 concurrent users
2000 concurrent users
```

Candidate endpoints:

```text
GET  /api/disasters
POST /api/reports
POST /api/rescue-requests
GET  /api/disasters/{id}/nearby-rescue-requests
```

Metrics:

- Requests/sec / throughput.
- Average latency.
- P50.
- P95.
- P99.
- Error rate.
- CPU.
- Memory.
- Database query latency.

Benchmark scripts phải version-controlled.

---

# 17. Phase 2 Decision Rules

Không implement một cải tiến chỉ vì nó phổ biến.

Mapping ví dụ:

| Observed problem | Candidate improvement |
|---|---|
| Read-heavy endpoint gây DB load cao | Redis cache |
| Long synchronous side effects | RabbitMQ + async worker |
| Burst traffic làm request timeout | Queue / async processing |
| Notification failure làm mất công việc | Retry + DLQ |
| Duplicate event processing | Idempotent consumer |
| Khó quan sát bottleneck | Prometheus + Grafana |
| Một workload có scaling profile khác core | Selective worker/service extraction |

Mọi improvement cần ghi:

1. Vấn đề quan sát được.
2. Quality attribute liên quan.
3. Quyết định kiến trúc.
4. Trade-off.
5. Benchmark trước.
6. Benchmark sau.

---

# 18. Candidate Phase 2 Architecture

```text
                         Client
                           |
                           v
                    +-------------+
                    | API Gateway |
                    +------+------+
                           |
                           v
                +---------------------+
                |      GDRN Core      |
                |  Modular Monolith   |
                +----------+----------+
                           |
              +------------+-------------+
              |                          |
              v                          v
       PostgreSQL/PostGIS              Redis
              |
              | Integration Events
              v
          +--------+
          |RabbitMQ|
          +---+----+
              |
       +------+-------+----------+
       |              |          |
       v              v          v
   Risk Worker    Alert Worker  Analytics Worker
```

Đây chỉ là candidate architecture.

---

# 19. Reliability Patterns for Phase 2

Nếu RabbitMQ được áp dụng, xem xét:

```text
Message
  ↓
Consumer
  ↓ FAIL
Retry
  ↓ FAIL
Retry
  ↓ FAIL
Dead Letter Queue
```

Các vấn đề phải cân nhắc:

- At-least-once delivery.
- Duplicate messages.
- Idempotency.
- Poison messages.
- Retry backoff.
- Eventual consistency.
- Transaction boundary giữa DB và broker.

Không tuyên bố exactly-once nếu implementation không thực sự bảo đảm.

---

# 20. Observability Candidate

```text
Spring Boot
    ↓
Micrometer
    ↓
Prometheus
    ↓
Grafana
```

Metrics có thể gồm:

- Request throughput.
- HTTP latency.
- Error rate.
- JVM memory.
- CPU.
- DB pool usage.
- Query latency.
- RabbitMQ queue depth.
- Worker processing duration.
- Redis cache hit/miss.

---

# 21. Team Ownership Proposal

## Member 1 — Identity + Disaster

- Authentication.
- Authorization.
- User / role.
- Disaster lifecycle.
- Disaster API.

## Member 2 — Reporting + Geo/Risk

- Incident reports.
- Verification.
- PostGIS.
- Spatial queries.
- Basic risk policies.

## Member 3 — Rescue Coordination

- Rescue requests.
- Rescue teams.
- Rescue missions.
- Priority calculation.
- Assignment policy.

## Member 4 — Resource + Alert

- Resources.
- Shelters.
- Resource allocation.
- Emergency alerts.
- Alert lifecycle.

Shared responsibilities:

- Architecture decisions.
- Module contracts.
- Docker.
- CI if implemented.
- Integration tests.
- Load tests.
- Documentation.

---

# 22. One-Month Phase 1 Plan

## Week 1 — Foundation

- Requirements.
- Use cases.
- C4 diagrams.
- Domain model.
- Module boundaries.
- ERD.
- API conventions.
- Spring Boot skeleton.
- Docker Compose.
- PostgreSQL/PostGIS.
- Authentication foundation.
- Flyway.

## Week 2 — Parallel Domain Development

Mỗi thành viên triển khai module được phân công:

- Domain.
- Application use cases.
- API.
- Persistence adapter.
- Tests.

## Week 3 — Integration

- Cross-module workflows.
- Spatial queries.
- Security.
- Error handling.
- Integration tests.
- Swagger.
- Docker stabilization.

## Week 4 — Baseline Evaluation

- Complete Phase 1 scope.
- k6 load tests.
- Kaggle benchmark.
- Profiling.
- Bottleneck analysis.
- Architecture Decision Records.
- README.
- Phase 2 proposal.

---

# 23. Coding Conventions

Unless repository configuration specifies otherwise:

- Java naming conventions chuẩn.
- Constructor injection; tránh field injection.
- Prefer immutable value objects where practical.
- Không dùng static global state cho business logic.
- Không đặt business logic trong controller.
- Không đặt business logic quan trọng trong JPA entity nếu domain model tách biệt.
- Không trả `null` tùy tiện nếu domain semantics cần type rõ ràng.
- Exception types phải có ý nghĩa domain/application.
- Mapping code rõ ràng hơn magic reflection khi mapping ảnh hưởng correctness.
- Method/class nên có responsibility hẹp.
- Comment giải thích **why**, không lặp lại **what** mà code đã thể hiện.

---

# 24. Architecture Smells to Reject

Codex phải cảnh báo hoặc tránh khi phát hiện:

```text
Controller -> JpaRepository trực tiếp
```

```text
Domain -> Spring annotation/framework dependency
```

```text
Module A -> Module B JPA entity
```

```text
Business rule nằm trong controller
```

```text
One giant Service class xử lý nhiều bounded context
```

```text
shared/ chứa toàn bộ DTO/entity/service dùng chung
```

```text
RabbitMQ/Redis được thêm ở Pha 1 mà không có requirement mới
```

```text
Microservice được tạo chỉ để tách CRUD module
```

```text
Hard-coded JWT secret / DB password
```

```text
Schema update tự động thay migration có kiểm soát
```

---

# 25. Documentation Artifacts

Repository nên có:

```text
README.md
ARCHITECTURE.md
CODEX.md hoặc AGENTS.md
/docs
    /adr
    /diagrams
    /api
    /benchmark
```

Recommended ADRs:

```text
ADR-001: Use Modular Monolith for Phase 1
ADR-002: Use Clean/Hexagonal Architecture
ADR-003: Use PostgreSQL + PostGIS
ADR-004: Use JWT Authentication
ADR-005: Internal Domain Events in Phase 1
```

Pha 2 tạo ADR mới dựa trên benchmark thay vì sửa lịch sử ADR cũ.

---

# 26. Quality Attributes

Các quality attributes trọng tâm:

## Maintainability

Đạt qua module boundaries, dependency rules, domain isolation và tests.

## Modifiability

Có thể thêm disaster type, risk policy hoặc assignment policy mà không sửa toàn bộ hệ thống.

## Performance

Đo latency và throughput bằng load test.

## Scalability

Đánh giá hành vi khi concurrent load tăng và khi có burst traffic.

## Reliability

Pha 2 có thể đánh giá retry, DLQ, message loss/duplication và failure isolation.

## Security

Authentication, authorization và secret management.

Không cố tối ưu tất cả quality attributes cùng lúc. Mỗi quyết định phải ghi rõ trade-off.

---

# 27. Acceptance Criteria for Phase 1

Pha 1 tối thiểu phải đáp ứng đề bài môn học:

- REST API giao tiếp JSON.
- Có POST.
- Có GET.
- Có DELETE.
- Có OpenAPI/Swagger.
- Phân tầng API → Business/Application/Domain → Data Access.
- Business/domain không phụ thuộc web framework hoặc DB library.
- Repository/Data Access Layer sử dụng ORM/data mapper.
- Có login.
- Có protected GET endpoint.
- Có protected POST endpoint.
- Authentication qua filter/middleware/interceptor.
- Dockerized.
- Public GitHub repository.
- README mô tả architecture và specification.
- Commit history có ý nghĩa.
- Có load testing trên Kaggle CPU.

Ngoài minimum rubric, GDRN hướng tới module boundaries, PostGIS, domain logic và benchmark đủ rõ để phục vụ Pha 2.

---

# 28. Initial Non-Goals

Để kiểm soát scope Pha 1, mặc định **không** triển khai:

- Full microservices.
- Kubernetes.
- Kafka cluster.
- Real satellite integration.
- Real government emergency APIs.
- Real SMS billing/provider integration.
- Machine-learning disaster prediction.
- Complex frontend.
- Multi-region deployment.
- Event sourcing.
- CQRS infrastructure phức tạp.

Các mục này chỉ được thêm khi nhóm chủ động thay đổi scope và có đủ thời gian/lý do.

---

# 29. Core Architectural Narrative

```text
PHASE 1

Modular Monolith
      +
DDD
      +
Clean / Hexagonal Architecture
      +
Internal Domain Events
      +
PostgreSQL / PostGIS
      |
      v
Functional Tests
      |
      v
Load Test / Profiling
      |
      v
Observed Bottlenecks
      |
      v
Architecture Decisions
      |
      v
PHASE 2

Selective Improvements
(Event-driven / RabbitMQ / Redis / Workers / Observability)
      |
      v
Benchmark Again
      |
      v
Compare Quality Attributes + Trade-offs
```

**Principle:** Không xây kiến trúc phức tạp trước rồi tìm lý do biện minh sau. Pha 1 tạo baseline; Pha 2 thay đổi kiến trúc dựa trên evidence.

---

# 30. Prompt Context for Future Codex Tasks

Khi được yêu cầu implement một feature, Codex nên tự kiểm tra:

1. Feature thuộc bounded context nào?
2. Business rule thuộc domain hay application?
3. Input/output port nào cần thiết?
4. Có đang tạo dependency từ inner layer ra outer layer không?
5. Có đang truy cập module khác qua implementation detail không?
6. Endpoint cần authentication/authorization gì?
7. Có schema migration không?
8. Cần unit test hay integration test nào?
9. Thay đổi có ảnh hưởng OpenAPI không?
10. Thay đổi có làm sai baseline benchmark hoặc vô tình đưa giải pháp Pha 2 vào Pha 1 không?

Nếu một task yêu cầu vi phạm các rule trên, không âm thầm làm sai architecture. Hãy chỉ rõ conflict và đề xuất phương án phù hợp với kiến trúc hiện tại.

---

## Project Summary

**GDRN — Global Disaster Response Network / Hệ thống Điều phối và Ứng phó Thảm họa Toàn cầu**

### Phase 1

```text
Java + Spring Boot
Modular Monolith
DDD
Clean / Hexagonal Architecture
Spring Security + JWT
PostgreSQL + PostGIS
Flyway
OpenAPI / Swagger
Docker
JUnit / Mockito / Testcontainers
k6
```

### Phase 2 candidates

```text
Event-Driven Architecture
RabbitMQ
Async Workers
Redis
Retry / DLQ / Idempotency
Prometheus + Grafana
Selective service extraction
```

Architecture quality is more important than raw line count. Preserve domain boundaries, produce measurable baselines, and make later improvements evidence-driven.
