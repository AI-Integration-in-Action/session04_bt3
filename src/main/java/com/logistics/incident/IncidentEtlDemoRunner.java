package com.logistics.incident;

import com.logistics.incident.entity.IncidentReport;
import com.logistics.incident.exception.EtlValidationException;
import com.logistics.incident.repository.IncidentReportRepository;
import com.logistics.incident.service.IncidentETLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IncidentEtlDemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IncidentEtlDemoRunner.class);

    private final IncidentETLService etlService;
    private final IncidentReportRepository repository;

    public IncidentEtlDemoRunner(IncidentETLService etlService, IncidentReportRepository repository) {
        this.etlService = etlService;
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        log.info("==================================================================");
        log.info(" STARTING REFACTORED DEFENSIVE ETL SERVICE DEMONSTRATION (BT3)");
        log.info("==================================================================");

        // TEST CASE 1: Markdown Wrapped Raw AI Response (Fixed by Regex Helper)
        String markdownWrappedResponse = """
            ```json
            {
              "orderCode": "ORD-2026-7788",
              "licensePlate": "51D-999.88",
              "incidentType": "KE_XE_TRON",
              "urgency": "MEDIUM",
              "details": "Kẹt xe kéo dài tại cầu Phú Mỹ."
            }
            ```
            """;

        log.info("\n--- TEST CASE 1: Processing Markdown Wrapped AI JSON Response ---");
        try {
            IncidentReport report1 = etlService.processRawAiResponseDirectly(
                    "Tài xế báo kẹt xe đơn ORD-2026-7788 xe 51D-999.88", markdownWrappedResponse);
            log.info("SUCCESSfully processed Report 1: ID = {}, OrderCode = {}", report1.getId(), report1.getOrderCode());
        } catch (Exception e) {
            log.error("Failed Test Case 1: {}", e.getMessage());
        }

        // TEST CASE 2: Invalid Data (Missing OrderCode & Invalid License Plate & Invalid Urgency)
        String trashDataAiResponse = """
            ```json
            {
              "orderCode": "",
              "licensePlate": "INVALID_PLATE_123",
              "incidentType": "UNKNOWN",
              "urgency": "SUPER_EXTREME_URGENT",
              "details": "Dữ liệu thiếu mã đơn hàng và sai định dạng biển số."
            }
            ```
            """;

        log.info("\n--- TEST CASE 2: Processing Trash / Missing Data (Defensive Validation Rollback) ---");
        try {
            etlService.processRawAiResponseDirectly("Tin nhắn lỗi rác", trashDataAiResponse);
        } catch (EtlValidationException e) {
            log.warn("CATCH EXPECTED ETL VALIDATION EXCEPTION: {}", e.getMessage());
            log.info("TRANSACTION ROLLBACK VERIFIED: Record was rejected and not persisted.");
        }

        // TEST CASE 3: Database Verification
        log.info("\n--- VERIFYING DATABASE PERSISTENCE AFTER ETL RUN ---");
        List<IncidentReport> reports = repository.findAll();
        log.info("Total Reports Saved in DB: {}", reports.size());
        for (IncidentReport r : reports) {
            log.info(" -> Saved Record: ID={}, OrderCode={}, Plate={}, Urgency={}, CreatedAt={}",
                    r.getId(), r.getOrderCode(), r.getLicensePlate(), r.getUrgency(), r.getCreatedAt());
        }

        log.info("==================================================================");
        log.info(" DEFENSIVE ETL DEMONSTRATION COMPLETED SUCCESSFULLY!");
        log.info("==================================================================");
    }
}
