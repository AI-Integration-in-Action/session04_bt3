package com.logistics.incident.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record IncidentExtraction(
    @JsonPropertyDescription("Mã đơn hàng liên quan đến sự cố (vd: ORD-2026-8812)")
    String orderCode,

    @JsonPropertyDescription("Biển số xe gặp sự cố (vd: 29C-123.45)")
    String licensePlate,

    @JsonPropertyDescription("Loại sự cố (vd: TAI_NAN, HONG_XE, KET_XE)")
    String incidentType,

    @JsonPropertyDescription("Mức độ khẩn cấp: LOW, MEDIUM, HIGH, CRITICAL")
    String urgency,

    @JsonPropertyDescription("Mô tả chi tiết nội dung sự cố")
    String details
) {}
