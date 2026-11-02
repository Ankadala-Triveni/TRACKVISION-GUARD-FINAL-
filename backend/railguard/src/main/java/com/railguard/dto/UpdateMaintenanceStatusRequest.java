package com.railguard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating the status and completion notes of a maintenance work order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMaintenanceStatusRequest {

    @NotBlank(message = "Status is required (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED)")
    private String status;

    private String completionNotes;
}

