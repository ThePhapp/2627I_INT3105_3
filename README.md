# GDRN – Hệ thống Điều phối và Ứng phó Thảm họa Toàn cầu

**Tên tiếng Anh:** Global Disaster Response Network (GDRN)  
**Tên tiếng Việt:** Hệ thống Điều phối và Ứng phó Thảm họa Toàn cầu  
**Loại dự án:** Backend REST API – Đồ án môn Kiến trúc Phần mềm

---

## 1. Tổng quan dự án

GDRN là hệ thống hỗ trợ quản lý thông tin và điều phối hoạt động ứng phó khi xảy ra các thảm họa như:

- Động đất
- Lũ lụt
- Bão
- Cháy rừng
- Sóng thần

Trong một thảm họa lớn, nhiều vấn đề có thể xảy ra cùng lúc:

- Người dân liên tục gửi báo cáo về sự cố.
- Nhiều người gửi yêu cầu cứu hộ.
- Cơ quan điều phối cần xác định khu vực nguy hiểm.
- Cần tìm đội cứu hộ phù hợp và gần vị trí nạn nhân.
- Nguồn lực như xe cứu thương, thực phẩm, nước, thuốc và thiết bị có giới hạn.
- Cảnh báo cần được gửi đến đúng khu vực.
- Lượng truy cập có thể tăng đột biến trong thời gian ngắn.

GDRN tập trung các thông tin này vào một hệ thống và cung cấp các chức năng hỗ trợ quá trình tiếp nhận, xác minh, điều phối và theo dõi hoạt động ứng phó thảm họa.

---

## 2. Bài toán mà hệ thống giải quyết

Có thể mô tả bài toán chính bằng chuỗi sau:

```text
Thảm họa xảy ra
      ↓
Người dân báo cáo sự cố
      ↓
Xác minh thông tin
      ↓
Xác định khu vực và mức độ ảnh hưởng
      ↓
Người dân gửi yêu cầu cứu hộ
      ↓
Đánh giá mức độ ưu tiên
      ↓
Tìm đội cứu hộ phù hợp
      ↓
Phân công nhiệm vụ
      ↓
Phân bổ tài nguyên cứu trợ
      ↓
Phát cảnh báo cho khu vực nguy hiểm
      ↓
Theo dõi cho đến khi tình huống được xử lý
```

Hệ thống không nhằm xây dựng một nền tảng cứu hộ thực tế ở quy mô quốc gia ngay từ đầu. Mục tiêu của đồ án là xây dựng một **mô hình đủ lớn và có nghiệp vụ thực tế** để nghiên cứu các quyết định kiến trúc phần mềm, đặc biệt là Performance, Scalability, Reliability, Maintainability và Modifiability.

---

## 3. Người dùng chính

### Citizen – Người dân

Có thể:

- Đăng ký và đăng nhập.
- Xem thông tin thảm họa.
- Gửi báo cáo sự cố.
- Gửi yêu cầu cứu hộ.
- Theo dõi yêu cầu cứu hộ của mình.
- Xem cảnh báo liên quan đến khu vực.

### Responder – Nhân viên/đội cứu hộ

Có thể:

- Xem nhiệm vụ được phân công.
- Cập nhật trạng thái cứu hộ.
- Thực hiện nhiệm vụ cứu hộ.

### Authority – Cơ quan điều phối

Có thể:

- Xác minh báo cáo.
- Quản lý thông tin thảm họa.
- Điều phối đội cứu hộ.
- Phân bổ tài nguyên.
- Tạo và phát cảnh báo.

### Admin – Quản trị viên

Có quyền quản trị hệ thống và các dữ liệu cần thiết.

---

# 4. Các nhóm chức năng chính

Hệ thống được chia thành 7 miền nghiệp vụ chính.

## 4.1. Identity & Access – Quản lý danh tính và truy cập

Quản lý:

- Người dùng
- Đăng ký
- Đăng nhập
- Xác thực
- Phân quyền

Các role dự kiến:

```text
CITIZEN
RESPONDER
AUTHORITY
ADMIN
```

---

## 4.2. Disaster Management – Quản lý thảm họa

Quản lý thông tin về thảm họa:

- Loại thảm họa
- Vị trí
- Mức độ nghiêm trọng
- Khu vực ảnh hưởng
- Thời điểm phát hiện
- Trạng thái

Ví dụ vòng đời:

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

---

## 4.3. Incident Reporting – Báo cáo sự cố

Cho phép người dân gửi thông tin về tình hình thực tế.

Ví dụ:

- Nước đang dâng nhanh.
- Một khu vực bị cô lập.
- Có người mắc kẹt.
- Phát hiện cháy rừng.
- Đường bị sạt lở.

Cơ quan có thẩm quyền có thể xác minh hoặc từ chối báo cáo.

---

## 4.4. Rescue Coordination – Điều phối cứu hộ

Quản lý:

- Yêu cầu cứu hộ
- Đội cứu hộ
- Năng lực của đội
- Trạng thái đội
- Nhiệm vụ cứu hộ
- Quá trình phân công

Ví dụ:

```text
Yêu cầu cứu hộ
      ↓
Xác minh
      ↓
Tính mức độ ưu tiên
      ↓
Tìm đội cứu hộ gần đó
      ↓
Kiểm tra năng lực đội
      ↓
Kiểm tra trạng thái sẵn sàng
      ↓
Phân công
      ↓
Thực hiện nhiệm vụ
      ↓
Hoàn thành
```

Đây là một trong những phần có nhiều business logic nhất của hệ thống.

---

## 4.5. Resource Management – Quản lý tài nguyên cứu trợ

Quản lý các nguồn lực như:

- Xe cứu thương
- Xe cứu hộ
- Thực phẩm
- Nước
- Thuốc
- Vật tư y tế
- Thiết bị cứu hộ
- Nơi trú ẩn

Hệ thống hỗ trợ phân bổ tài nguyên cho khu vực/thảm họa và kiểm soát số lượng còn khả dụng.

---

## 4.6. Emergency Alert – Cảnh báo khẩn cấp

Quản lý:

- Nội dung cảnh báo
- Mức độ cảnh báo
- Khu vực nhận cảnh báo
- Thời gian phát hành
- Trạng thái cảnh báo

Ví dụ:

```text
Thảm họa
   ↓
Xác định vùng ảnh hưởng
   ↓
Đánh giá rủi ro
   ↓
Tạo cảnh báo
   ↓
Phát cảnh báo
```

---

## 4.7. Geo / Risk – Địa lý và đánh giá rủi ro

Phụ trách các bài toán liên quan đến vị trí:

- Khoảng cách
- Bán kính ảnh hưởng
- Khu vực nguy hiểm
- Tìm yêu cầu cứu hộ gần thảm họa
- Tìm đội cứu hộ gần nạn nhân
- Truy vấn dữ liệu theo không gian địa lý

Ví dụ:

> Tìm tất cả yêu cầu cứu hộ nằm trong bán kính 10 km quanh tâm vùng lũ.

Pha 1 dự kiến sử dụng **PostgreSQL + PostGIS** để hỗ trợ các truy vấn không gian này.

---

# 5. Mục tiêu của Pha 1

Pha 1 xây dựng một phiên bản backend hoạt động được và có kiến trúc rõ ràng.

Mục tiêu quan trọng không phải tối ưu hệ thống ngay lập tức, mà là tạo ra một **baseline architecture** để:

1. Triển khai các nghiệp vụ chính.
2. Đảm bảo module và dependency rõ ràng.
3. Kiểm thử hệ thống.
4. Đóng gói bằng Docker.
5. Đo hiệu năng.
6. Tìm ra bottleneck thực tế.
7. Có dữ liệu để quyết định kiến trúc Pha 2.

---

# 6. Kiến trúc Pha 1

Kiến trúc chính:

> **Modular Monolith + Domain-Driven Design (DDD) + Hexagonal/Clean Architecture**

## 6.1. Modular Monolith

Toàn bộ backend vẫn được triển khai dưới dạng **một Spring Boot application**.

Tuy nhiên bên trong được chia thành các module nghiệp vụ:

```text
GDRN
│
├── Identity
├── Disaster
├── Reporting
├── Rescue
├── Resource
├── Alert
└── Geo/Risk
```

Kiến trúc tổng thể:

```text
                   Client
                      │
                   REST API
                      │
                      ▼
┌────────────────────────────────────────┐
│              GDRN Backend              │
│                                        │
│ Identity   Disaster   Reporting        │
│ Rescue     Resource   Alert   Geo      │
│                                        │
│           Modular Monolith             │
└──────────────────┬─────────────────────┘
                   │
                   ▼
           PostgreSQL + PostGIS
```

Pha 1 **không bắt đầu bằng Microservices**.

Lý do là nhóm cần một baseline đơn giản hơn để phát triển, kiểm thử và đo lường trước khi quyết định có cần kiến trúc phân tán hay không.

---

## 6.2. DDD và Hexagonal/Clean Architecture

Bên trong một module, kiến trúc dự kiến:

```text
API
 │
 ▼
Application
 │
 ▼
Domain
 ▲
 │
Infrastructure
 │
 ▼
Database
```

### API

Phụ trách:

- REST Controller
- HTTP Request
- HTTP Response
- Validation liên quan HTTP

### Application

Phụ trách:

- Use Case
- Command
- Query
- Điều phối luồng nghiệp vụ

### Domain

Chứa nghiệp vụ cốt lõi:

- Entity
- Value Object
- Aggregate
- Domain Service
- Domain Event
- Repository Port

Domain phải độc lập với framework.

Domain không phụ thuộc trực tiếp vào:

- Spring Web
- JPA/Hibernate
- PostgreSQL
- HTTP

### Infrastructure

Phụ trách các chi tiết kỹ thuật:

- Spring Data JPA
- PostgreSQL
- PostGIS
- Persistence Adapter
- Security infrastructure
- Các adapter bên ngoài

---

# 7. Công nghệ dự kiến cho Pha 1

```text
Java 21
Spring Boot
Spring Web
Spring Security
JWT
Spring Data JPA / Hibernate
PostgreSQL
PostGIS
Flyway
Swagger / OpenAPI
Docker / Docker Compose
JUnit 5
Mockito
Testcontainers
ArchUnit
GitHub Actions
k6
```

---

# 8. Xác thực và phân quyền

Pha 1 dự kiến sử dụng:

```text
Request
   ↓
JWT Authentication Filter
   ↓
Spring Security
   ↓
SecurityContext
   ↓
Authorization
   ↓
Controller
```

Các endpoint nghiệp vụ quan trọng sẽ yêu cầu xác thực và role phù hợp.

---

# 9. Database

Pha 1 sử dụng:

> **PostgreSQL + PostGIS**

PostgreSQL lưu dữ liệu nghiệp vụ thông thường.

PostGIS hỗ trợ dữ liệu địa lý và truy vấn không gian.

Ví dụ:

```text
Disaster center
      ●
    /   \
   /10km \
  ●   ●   ●
Rescue Requests
```

Hệ thống có thể tìm các yêu cầu cứu hộ nằm trong bán kính xác định của một thảm họa.

---

# 10. Luồng xử lý Pha 1

Ban đầu hệ thống chủ yếu xử lý theo cách đồng bộ.

Ví dụ:

```text
POST /reports
      ↓
Tạo báo cáo
      ↓
Xử lý nghiệp vụ
      ↓
Phân tích rủi ro
      ↓
Cập nhật dữ liệu liên quan
      ↓
Xử lý cảnh báo nếu cần
      ↓
Lưu Database
      ↓
HTTP Response
```

Cách làm này dễ triển khai và phù hợp để tạo baseline.

Tuy nhiên khi tải tăng cao, một số bước có thể trở thành bottleneck.

Đây chính là nội dung sẽ được đo thay vì giả định.

---

# 11. Domain Event trong Pha 1

Pha 1 có thể sử dụng **internal/in-process domain events**.

Ví dụ:

```text
IncidentReportSubmitted
DisasterDetected
DisasterEscalated
RescueRequested
RescueTeamAssigned
EmergencyAlertCreated
```

Các event này vẫn chạy trong cùng application/process.

Pha 1 chưa cần RabbitMQ hoặc Kafka.

---

# 12. Những thứ Pha 1 chưa sử dụng

Không đưa ngay các công nghệ sau vào Pha 1:

```text
Microservices
RabbitMQ
Kafka
Redis
Kubernetes
Distributed Database
```

Lý do:

Không nên chọn giải pháp trước khi biết vấn đề thực sự nằm ở đâu.

---

# 13. Kiểm thử và đảm bảo kiến trúc

Pha 1 dự kiến có:

### Unit Test

Kiểm thử business logic độc lập.

### Integration Test

Kiểm thử tương tác với PostgreSQL/PostGIS bằng Testcontainers.

### API Test

Kiểm thử các luồng REST quan trọng.

### Architecture Test

Sử dụng ArchUnit để kiểm tra các quy tắc như:

```text
Domain không phụ thuộc Spring

Domain không phụ thuộc Infrastructure

Application không phụ thuộc persistence implementation

Module A không truy cập Infrastructure của Module B
```

Như vậy kiến trúc không chỉ nằm trên tài liệu mà còn được kiểm tra tự động.

---

# 14. Benchmark Pha 1

Sau khi chức năng Pha 1 ổn định, hệ thống được load test.

Công cụ dự kiến:

> **k6**

Môi trường benchmark phải cố định để kết quả Pha 1 và Pha 2 có thể so sánh.

Có thể sử dụng môi trường CPU cố định theo yêu cầu môn học, ví dụ Kaggle CPU nếu phù hợp với cách triển khai benchmark.

Các API đáng benchmark:

```text
GET  /api/v1/disasters
POST /api/v1/reports
POST /api/v1/rescue-requests
GET  /api/v1/disasters/{id}/nearby-rescue-requests
```

Các chỉ số:

- Throughput
- Average response time
- P50
- P95
- P99
- Error rate
- CPU usage
- Memory usage
- Database query time

---

# 15. Từ Pha 1 sang Pha 2

Quy trình:

```text
Hoàn thành Pha 1
       ↓
Load Test
       ↓
Profiling
       ↓
Tìm Bottleneck
       ↓
Xác định Quality Attribute bị ảnh hưởng
       ↓
Đề xuất thay đổi kiến trúc
       ↓
Triển khai Pha 2
       ↓
Benchmark lại
       ↓
So sánh Pha 1 và Pha 2
       ↓
Phân tích Trade-off
```

Điểm quan trọng:

> **Pha 2 không đơn giản là chuyển Monolith thành Microservices.**

Mỗi thay đổi phải giải quyết một vấn đề đã được quan sát hoặc đo lường.

---

# 16. Hướng kiến trúc Pha 2

Một hướng dự kiến nếu benchmark chứng minh phù hợp:

```text
                    Client
                       │
                       ▼
                   GDRN Core
                       │
          ┌────────────┼─────────────┐
          │            │             │
          ▼            ▼             ▼
     PostgreSQL      Redis        RabbitMQ
                                     │
                    ┌────────────────┼────────────────┐
                    ▼                ▼                ▼
               Risk Worker      Alert Worker    Analytics Worker
```

Core vẫn có thể là Modular Monolith.

Chỉ những phần thực sự cần xử lý độc lập hoặc bất đồng bộ mới được tách.

---

# 17. Event-Driven Architecture trong Pha 2

Giả sử Pha 1 có:

```text
POST /reports
     ↓
Save Report
     ↓
Risk Analysis
     ↓
Geo Analysis
     ↓
Alert Processing
     ↓
Response
```

Nếu benchmark cho thấy Risk/Alert làm request chậm và các bước này không cần hoàn thành trước HTTP response, Pha 2 có thể chuyển thành:

```text
POST /reports
     ↓
Save Report
     ↓
Return HTTP 201
     ↓
Publish ReportCreated
     ↓
RabbitMQ
  ┌──────┼─────────┐
  ▼      ▼         ▼
Risk   Alert    Analytics
Worker Worker    Worker
```

Lợi ích cần kiểm chứng bằng benchmark:

- Giảm thời gian phản hồi của request.
- Hấp thụ burst traffic tốt hơn.
- Cho phép xử lý nền.
- Giảm temporal coupling.

Đổi lại, hệ thống phức tạp hơn và xuất hiện eventual consistency.

---

# 18. Redis trong Pha 2

Redis chỉ nên được thêm nếu benchmark chứng minh có vấn đề với read-heavy workload hoặc truy vấn lặp lại.

Ví dụ:

```text
Client
   ↓
GDRN
   ↓
Redis
 ├── HIT  → Response
 │
 └── MISS
       ↓
   PostgreSQL
```

Ví dụ dữ liệu có thể cân nhắc cache:

- Danh sách thảm họa đang hoạt động.
- Cảnh báo hiện tại.
- Một số kết quả truy vấn được đọc thường xuyên.

Redis không được thêm chỉ để làm stack lớn hơn.

---

# 19. Reliability trong Pha 2

Nếu sử dụng message broker, Pha 2 cần xem xét:

- Retry
- Dead Letter Queue
- Idempotent Consumer
- Duplicate Event Handling
- Eventual Consistency
- Failure Recovery

Do đó Event-Driven Architecture vừa đem lại lợi ích vừa tạo ra độ phức tạp mới.

---

# 20. Observability trong Pha 2

Có thể bổ sung:

```text
Spring Boot
     ↓
Micrometer
     ↓
Prometheus
     ↓
Grafana
```

Để quan sát:

- Request latency
- Throughput
- Error rate
- JVM metrics
- Database behavior
- Queue behavior
- Worker processing

---

# 21. Có cần Microservices ở Pha 2 không?

Không mặc định.

Có ba khả năng:

### Trường hợp 1 – Modular Monolith vẫn đáp ứng tốt

Giữ nguyên kiến trúc.

### Trường hợp 2 – Chỉ một số tác vụ cần bất đồng bộ

Giữ GDRN Core là Modular Monolith và thêm RabbitMQ + Workers.

### Trường hợp 3 – Một module thực sự cần scale/deploy độc lập

Có thể cân nhắc tách module đó thành service riêng.

Quyết định phải dựa trên:

- Performance
- Scalability
- Reliability
- Coupling
- Deployment requirements
- Benchmark evidence

---

# 22. So sánh định hướng Pha 1 và Pha 2

| Thành phần            | Pha 1                    | Pha 2 dự kiến                                    |
| --------------------- | ------------------------ | ------------------------------------------------ |
| Kiến trúc chính       | Modular Monolith         | Modular Monolith cải tiến / selective extraction |
| Giao tiếp chính       | Synchronous              | Sync + Async                                     |
| Domain design         | DDD                      | DDD                                              |
| Internal architecture | Hexagonal/Clean          | Hexagonal/Clean                                  |
| Database              | PostgreSQL + PostGIS     | PostgreSQL + PostGIS                             |
| Messaging             | Internal events          | RabbitMQ nếu cần                                 |
| Cache                 | Không                    | Redis nếu cần                                    |
| Workers               | Không                    | Có thể có                                        |
| Observability         | Cơ bản                   | Prometheus/Grafana                               |
| Deployment            | Một backend              | Core + worker/service nếu cần                    |
| Mục tiêu              | Baseline đúng và đo được | Cải thiện quality attributes                     |
| Cơ sở quyết định      | Thiết kế ban đầu         | Kết quả benchmark Pha 1                          |

---

# 23. Các Quality Attribute quan trọng

Đề tài tập trung vào một số thuộc tính chất lượng chính.

### Performance

- Response time
- Throughput
- Database latency

### Scalability

Khả năng xử lý khi lượng report/rescue request tăng đột biến.

### Reliability

Khả năng xử lý lỗi và tránh mất tác vụ quan trọng.

### Maintainability

Khả năng duy trì code nhờ module boundary rõ ràng.

### Modifiability

Khả năng thêm loại thảm họa hoặc business rule mới mà không ảnh hưởng toàn hệ thống.

### Security

Authentication và role-based authorization.

Không nhất thiết tối ưu tất cả quality attributes cùng lúc.

---

# 24. Câu chuyện kiến trúc của đồ án

Toàn bộ hướng phát triển có thể tóm tắt:

```text
                 BÀI TOÁN
                    │
                    ▼
       Điều phối ứng phó thảm họa
                    │
                    ▼
                  PHA 1
                    │
       Modular Monolith + DDD
        + Hexagonal Architecture
                    │
                    ▼
         Xây dựng chức năng
                    │
                    ▼
              Load Testing
                    │
                    ▼
          Phân tích Bottleneck
                    │
                    ▼
          Quality Attributes
                    │
          ┌─────────┼─────────┐
          ▼         ▼         ▼
     Performance Scalability Reliability
          └─────────┼─────────┘
                    ▼
                  PHA 2
                    │
          Cải tiến có chọn lọc
                    │
       ┌────────────┼────────────┐
       ▼            ▼            ▼
 Event-Driven     Cache       Workers
 RabbitMQ         Redis       Async
       │
       ▼
            Benchmark lại
                    │
                    ▼
             So sánh P1/P2
                    │
                    ▼
          Phân tích Trade-off
```

---

# 25. Giá trị của đề tài đối với môn Kiến trúc Phần mềm

Giá trị của GDRN không chỉ nằm ở việc xây dựng một hệ thống quản lý cứu hộ.

Điểm chính của đồ án là chứng minh được quá trình:

```text
Phân tích bài toán
        ↓
Thiết kế Domain
        ↓
Chọn kiến trúc
        ↓
Triển khai Baseline
        ↓
Đo Quality Attributes
        ↓
Phát hiện vấn đề
        ↓
Đưa ra Architecture Decision
        ↓
Cải tiến kiến trúc
        ↓
Benchmark lại
        ↓
Đánh giá Trade-off
```

Pha 1 tạo ra một hệ thống có cấu trúc tốt và có thể đo lường.

Pha 2 không thay đổi kiến trúc theo xu hướng hoặc để tăng số lượng công nghệ, mà sử dụng kết quả thực nghiệm từ Pha 1 để quyết định **cần thay đổi gì, tại sao cần thay đổi và thay đổi đó cải thiện hoặc đánh đổi thuộc tính chất lượng nào**.

Đây là trọng tâm của đề tài GDRN trong môn Kiến trúc Phần mềm.
"""

out = "/mnt/data/GDRN_Mo_Ta_Du_An.md"
pypandoc.convert_text(content, 'md', format='md', outputfile=out, extra_args=['--standalone'])
print(out)
