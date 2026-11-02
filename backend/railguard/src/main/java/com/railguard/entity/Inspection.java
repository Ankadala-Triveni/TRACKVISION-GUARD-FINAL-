package com.railguard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Inspection entity representing railway track inspection records.
 * Uses a normal Foreign Key to User (without cascade delete) to preserve historical records.
 * Stores relative file path (never absolute system path) and full analysis metadata.
 */
@Entity
@Table(name = "inspections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inspection_number", nullable = false, unique = true, length = 64)
    private String inspectionNumber;

    // Normal Foreign Key relationship (NO cascade delete to preserve history)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "inspection_date", nullable = false)
    private LocalDateTime inspectionDate;

    @Column(name = "railway_zone", nullable = false, length = 100)
    private String railwayZone;

    @Column(name = "track_section", nullable = false, length = 100)
    private String trackSection;

    @Column(nullable = false, length = 255)
    private String location;

    private Double latitude;

    private Double longitude;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "stored_filename")
    private String storedFilename;

    // Relative storage key/path only (e.g. "inspections/101/inst_101_uuid.jpg")
    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private InspectionStatus status = InspectionStatus.CREATED;

    @Column(name = "risk_score")
    @Builder.Default
    private Double riskScore = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "analysis_started_at")
    private LocalDateTime analysisStartedAt;

    @Column(name = "analysis_completed_at")
    private LocalDateTime analysisCompletedAt;

    @Column(name = "analysis_error", length = 255)
    private String analysisError;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
