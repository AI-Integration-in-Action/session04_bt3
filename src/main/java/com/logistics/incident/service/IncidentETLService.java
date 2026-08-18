package com.logistics.incident.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.incident.dto.IncidentExtraction;
import com.logistics.incident.entity.IncidentReport;
import com.logistics.incident.enums.UrgencyLevel;
import com.logistics.incident.exception.EtlValidationException;
import com.logistics.incident.repository.IncidentReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Enterprise-grade Refactored Defensive ETL Service.
 * 
 * Key Improvements:
 * 1. Constructor Injection replacing field @Autowired with optional ChatModel fallback.
 * 2. Pre-parsing Markdown Code Block Stripper (Regex) to eliminate Jackson parsing crashes.
 * 3. Manual Defensive Validation on DTO before Entity mapping.
 * 4. Strict Transaction Management (@Transactional with rollbackFor = Exception.class).
 * 5. Structured SLF4J Logging with context details on success and error.
 */
@Service
public class IncidentETLService {

    private static final Logger log = LoggerFactory.getLogger(IncidentETLService.class);

    // Regex for Vietnamese License Plate format validation (e.g. 29C-123.45, 51D-9999, 30F-888.88)
    private static final Pattern LICENSE_PLATE_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]-[0-9]{3,5}(\\.[0-9]{2})?$");

    private final ChatModel chatModel;
    private final IncidentReportRepository repository;
    private final ObjectMapper objectMapper;
    private final BeanOutputConverter<IncidentExtraction> outputConverter;

    public IncidentETLService(@Autowired(required = false) ChatModel chatModel,
                               IncidentReportRepository repository,
                               ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.outputConverter = new BeanOutputConverter<>(IncidentExtraction.class);
    }

    /**
     * Process raw driver incident message with defensive pipeline.
     */
    @Transactional(rollbackFor = Exception.class)
    public IncidentReport processReport(String rawMessage) {
        log.info("[ETL START] Received raw driver message: '{}'", rawMessage);

        if (!StringUtils.hasText(rawMessage)) {
            log.error("[ETL ERROR] Raw message is empty or null!");
            throw new EtlValidationException("Raw driver message cannot be empty");
        }

        try {
            // Step 1: Query LLM ChatModel (or fallback mock)
            String rawAiResponse = callLlm(rawMessage);
            log.info("[ETL STEP 1] Received raw AI response.");

            // Step 2: Clean Markdown Wrapper (Eliminate ```json ... ``` code fences)
            String cleanedJson = cleanMarkdownJsonResponse(rawAiResponse);
            log.info("[ETL STEP 2] Cleaned JSON response from Markdown wrappers.");

            // Step 3: Convert JSON to DTO
            IncidentExtraction dto = parseJsonToDto(cleanedJson);
            log.info("[ETL STEP 3] Successfully parsed JSON to DTO: {}", dto);

            // Step 4: Defensive Business Validation
            validateExtractionDto(dto);
            log.info("[ETL STEP 4] Defensive Validation PASSED.");

            // Step 5: Map DTO to JPA Entity
            UrgencyLevel urgencyLevel = UrgencyLevel.parseOrNull(dto.urgency());
            IncidentReport entity = new IncidentReport(
                    dto.orderCode().trim(),
                    dto.licensePlate().trim().toUpperCase(),
                    StringUtils.hasText(dto.incidentType()) ? dto.incidentType().trim() : "OTHER",
                    urgencyLevel,
                    StringUtils.hasText(dto.details()) ? dto.details().trim() : ""
            );

            // Step 6: Persist Entity
            IncidentReport savedReport = repository.save(entity);
            log.info("[ETL SUCCESS] Saved IncidentReport to DB with ID: {}", savedReport.getId());

            return savedReport;

        } catch (EtlValidationException e) {
            log.error("[ETL ROLLBACK] Defensive Validation failed for raw message: '{}'. Error: {}", rawMessage, e.getMessage());
            throw e; // Triggers @Transactional rollback
        } catch (Exception e) {
            log.error("[ETL ROLLBACK] Unexpected system error processing raw message: '{}'. Cause: {}", rawMessage, e.getMessage(), e);
            throw new EtlValidationException("ETL Processing Failed: " + e.getMessage(), e);
        }
    }

    /**
     * Directly process pre-obtained AI JSON string (used for direct simulation/testing).
     */
    @Transactional(rollbackFor = Exception.class)
    public IncidentReport processRawAiResponseDirectly(String rawMessage, String rawAiResponse) {
        log.info("[ETL START DIRECT] Processing pre-obtained raw AI response...");
        try {
            String cleanedJson = cleanMarkdownJsonResponse(rawAiResponse);
            IncidentExtraction dto = parseJsonToDto(cleanedJson);
            validateExtractionDto(dto);

            UrgencyLevel urgencyLevel = UrgencyLevel.parseOrNull(dto.urgency());
            IncidentReport entity = new IncidentReport(
                    dto.orderCode().trim(),
                    dto.licensePlate().trim().toUpperCase(),
                    StringUtils.hasText(dto.incidentType()) ? dto.incidentType().trim() : "OTHER",
                    urgencyLevel,
                    StringUtils.hasText(dto.details()) ? dto.details().trim() : ""
            );

            IncidentReport saved = repository.save(entity);
            log.info("[ETL SUCCESS DIRECT] Saved IncidentReport with ID: {}", saved.getId());
            return saved;
        } catch (EtlValidationException e) {
            log.error("[ETL ROLLBACK DIRECT] Defensive Validation failed: {}", e.getMessage());
            throw e;
        }
    }

    private String callLlm(String rawMessage) {
        if (chatModel == null) {
            // Mock AI response if ChatModel bean is not configured with live API key
            return """
                ```json
                {
                  "orderCode": "ORD-2026-9912",
                  "licensePlate": "29C-881.22",
                  "incidentType": "TAI_NAN",
                  "urgency": "HIGH",
                  "details": "Xe va chạm tại QL5, vỡ đèn pha."
                }
                ```
                """;
        }

        String formatInstructions = outputConverter.getFormat();
        Prompt prompt = new Prompt("Phân tích tin nhắn sự cố sau: " + rawMessage + "\n" + formatInstructions);
        return chatModel.call(prompt).getResult().getOutput().getText();
    }

    /**
     * Helper Method: Strips markdown code blocks (```json ... ```) from raw LLM responses.
     */
    public String cleanMarkdownJsonResponse(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            return "{}";
        }
        String trimmed = rawResponse.trim();
        // Remove starting ```json or ```
        trimmed = trimmed.replaceAll("(?s)^```(?:json)?\\s*", "");
        // Remove ending ```
        trimmed = trimmed.replaceAll("(?s)\\s*```$", "");
        return trimmed.trim();
    }

    private IncidentExtraction parseJsonToDto(String json) {
        try {
            return objectMapper.readValue(json, IncidentExtraction.class);
        } catch (Exception e) {
            log.error("Failed to parse JSON string to DTO: {}", json, e);
            throw new EtlValidationException("JSON Parsing Failed due to malformed AI structure: " + e.getMessage(), e);
        }
    }

    /**
     * Manual Defensive Validation on DTO.
     */
    private void validateExtractionDto(IncidentExtraction dto) {
        if (dto == null) {
            throw new EtlValidationException("Parsed DTO object is null");
        }

        // Rule 1: Order Code must NOT be blank
        if (!StringUtils.hasText(dto.orderCode())) {
            throw new EtlValidationException("Missing mandatory field: orderCode is null or empty");
        }

        // Rule 2: License Plate must NOT be blank and must match format pattern
        if (!StringUtils.hasText(dto.licensePlate())) {
            throw new EtlValidationException("Missing mandatory field: licensePlate is null or empty");
        }
        String cleanPlate = dto.licensePlate().trim().toUpperCase();
        if (!LICENSE_PLATE_PATTERN.matcher(cleanPlate).matches()) {
            throw new EtlValidationException("Invalid licensePlate format: '" + dto.licensePlate() + "'. Expected pattern: 29C-123.45");
        }

        // Rule 3: Urgency must be a valid UrgencyLevel enum (LOW, MEDIUM, HIGH, CRITICAL)
        if (!StringUtils.hasText(dto.urgency()) || UrgencyLevel.parseOrNull(dto.urgency()) == null) {
            throw new EtlValidationException("Invalid urgency level: '" + dto.urgency() + "'. Allowed values: LOW, MEDIUM, HIGH, CRITICAL");
        }
    }
}
