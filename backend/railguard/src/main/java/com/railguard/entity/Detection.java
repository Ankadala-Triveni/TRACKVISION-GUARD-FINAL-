package com.railguard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Detection entity representing AI-detected track defects.
 * Bounding box coordinates are stored in separate columns (xMin, yMin, xMax, yMax)
 * for native spatial indexing and JPA maintainability.
 */
@Entity
@Table(name = "detections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Detection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @Column(name = "defect_class", nullable = false, length = 100)
    private String defectClass;

    @Column(nullable = false)
    private Double confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(name = "x_min", nullable = false)
    private Double xMin;

    @Column(name = "y_min", nullable = false)
    private Double yMin;

    @Column(name = "x_max", nullable = false)
    private Double xMax;

    @Column(name = "y_max", nullable = false)
    private Double yMax;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @PrePersist
    protected void onCreate() {
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
    }
}
