package com.railguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO for defect detection responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectionResponse {
    private Long id;
    private String defectClass;
    private Double confidence;
    private String severity;
    private BoundingBoxDto boundingBox;
    private String modelVersion;
    private LocalDateTime detectedAt;
}
