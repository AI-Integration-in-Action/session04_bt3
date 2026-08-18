package com.logistics.incident.entity;

import com.logistics.incident.enums.UrgencyLevel;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_reports")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, length = 50)
    private String orderCode;

    @Column(name = "license_plate", nullable = false, length = 20)
    private String licensePlate;

    @Column(name = "incident_type", nullable = false, length = 50)
    private String incidentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false, length = 20)
    private UrgencyLevel urgency;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected IncidentReport() {
    }

    public IncidentReport(String orderCode, String licensePlate, String incidentType, UrgencyLevel urgency, String details) {
        this.orderCode = orderCode;
        this.licensePlate = licensePlate;
        this.incidentType = incidentType;
        this.urgency = urgency;
        this.details = details;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public UrgencyLevel getUrgency() {
        return urgency;
    }

    public void setUrgency(UrgencyLevel urgency) {
        this.urgency = urgency;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "IncidentReport{" +
                "id=" + id +
                ", orderCode='" + orderCode + '\'' +
                ", licensePlate='" + licensePlate + '\'' +
                ", incidentType='" + incidentType + '\'' +
                ", urgency=" + urgency +
                ", createdAt=" + createdAt +
                '}';
    }
}
