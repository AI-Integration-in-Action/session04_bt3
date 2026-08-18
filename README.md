AI Logistics Incident Reporter - Refactored Defensive ETL Service (BT3)

Tối ưu hóa và Refactor mã nguồn ETL bóc tách tin nhắn sự cố của tài xế theo chuẩn doanh nghiệp (Enterprise Standard), khắc phục hoàn toàn lỗi bọc Markdown code block và lỗi dữ liệu rác gây crash hệ thống.

---

1. Cấu trúc thư mục dự án

```text
BT3/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── logistics/
        │           └── incident/
        │               ├── IncidentApplication.java
        │               ├── IncidentEtlDemoRunner.java
        │               ├── dto/
        │               │   └── IncidentExtraction.java
        │               ├── entity/
        │               │   └── IncidentReport.java
        │               ├── enums/
        │               │   └── UrgencyLevel.java
        │               ├── exception/
        │               │   └── EtlValidationException.java
        │               ├── repository/
        │               │   └── IncidentReportRepository.java
        │               └── service/
        │                   └── IncidentETLService.java
        └── resources/
            └── application.properties
```

---

2. Tại sao việc kiểm chứng dữ liệu thủ công (Defensive Validation) lại BẮT BUỘC?

Mặc dù Spring AI cung cấp JSON Schema / Format Instructions gửi tới LLM, việc Kiểm chứng dữ liệu thủ công (Defensive Validation) trong mã Java vẫn là BẮT BUỘC vì 3 lý do kỹ thuật cốt lõi:

a. Bản chất Phi định hình (Non-Deterministic) & Ảo giác (Hallucination) của LLM:
- Format Instructions từ Prompt chỉ mang tính chất "Gợi ý" (Guidance) cho LLM chứ KHÔNG PHẢI là ràng buộc cứng (Hard Constraint) ở mức Compiler hay Database.
- Mô hình AI hoàn toàn có thể trả về các giá trị tưởng chừng hợp lệ về mặt cú pháp JSON nhưng vô lý về mặt nghiệp vụ (ví dụ: biển số xe "XYZ-INVALID", mức độ khẩn cấp "ULTRA_SUPER_HIGH", hoặc bỏ trống orderCode).

b. Xử lý Lỗi bọc Markdown (Code Blocks):
- LLM thường tự động bọc đầu ra trong các thẻ markdown ```json ... ```. Nếu không có bộ làm sạch (Clean Helper / Regex) và kiểm tra thủ công, Jackson Parser sẽ ném ra `JsonParseException` làm đứt gãy luồng xử lý của hệ thống.

c. Đảm bảo Tính toàn vẹn Cơ sở dữ liệu & Cơ chế Rollback Transaction:
- Các cột trong Database thường có ràng buộc NOT NULL, Unique, hoặc Foreign Key.
- Nếu không validate DTO thủ công trước khi map sang Entity, câu lệnh `repository.save()` sẽ ném ngoại lệ SQL (`ConstraintViolationException`). Việc kiểm chứng chủ động cho phép ném ra `EtlValidationException` có cấu trúc, kích hoạt `@Transactional` rollback an toàn và ghi log chi tiết phục vụ cảnh báo.

---

3. Hướng dẫn chạy thử nghiệm

```bash
./mvnw spring-boot:run
```
Ứng dụng tự động chạy `IncidentEtlDemoRunner` minh chứng:
- Kịch bản 1: Nhận JSON bị bọc Markdown ```json ... ``` -> Regex helper làm sạch -> Validate DTO thành công -> Lưu DB ID 1.
- Kịch bản 2: Nhận JSON thiếu `orderCode` -> Defensive Validation phát hiện lỗi -> Ném `EtlValidationException` -> Trigger Rollback Transaction -> Kiểm tra DB xác nhận record không bị lưu bẩn.
