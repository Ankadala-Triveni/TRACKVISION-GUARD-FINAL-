package com.railguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing inspection details, relative image path, status, and analysis metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionResponse {
    private Long id;
    private String inspectionNumber;
    private Long userId;
    private String username;
    private LocalDateTime inspectionDate;
    private String railwayZone;
    private String trackSection;
    private String location;
    private Double latitude;
    private Double longitude;
    private String originalFilename;
    private String filePath; // Relative storage key/path
    private String status;
    private Double riskScore;
    private String riskLevel;
    private LocalDateTime analysisStartedAt;
    private LocalDateTime analysisCompletedAt;
    private String analysisError;
    private String modelVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DetectionResponse> detections;
}
