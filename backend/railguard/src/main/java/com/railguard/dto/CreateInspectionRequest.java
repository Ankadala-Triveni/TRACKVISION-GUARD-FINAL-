package com.railguard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new track inspection.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInspectionRequest {

    private String railwayZone;

    private String trackSection;

    private String location;

    private Double latitude;

    private Double longitude;
}
