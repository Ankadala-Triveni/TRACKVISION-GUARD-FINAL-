package com.railguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Request DTO for creating a new maintenance work order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMaintenanceTaskRequest {

    @NotNull(message = "Inspection ID is required")
    private Long inspectionId;

    @NotBlank(message = "Maintenance task title is required")
    private String title;

    private String description;

    @NotBlank(message = "Priority is required (LOW, MEDIUM, HIGH, CRITICAL)")
    private String priority;

    @NotBlank(message = "Assigned engineering personnel or unit is required")
    private String assignedTo;

    private LocalDateTime scheduledDate;
}

