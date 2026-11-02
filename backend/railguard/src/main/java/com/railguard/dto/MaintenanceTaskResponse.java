package com.railguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Response DTO for maintenance work orders.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceTaskResponse {
    private Long id;
    private String taskNumber;
    private Long inspectionId;
    private String inspectionNumber;
    private String railwayZone;
    private String trackSection;
    private String location;
    private String defectCategory;
    private String riskLevel;
    private Double riskScore;
    private String title;
    private String description;
    private String priority;
    private String status;
    private String assignedTo;
    private LocalDateTime scheduledDate;
    private String completionNotes;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

