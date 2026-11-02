package com.railguard.service;

import com.railguard.entity.Detection;
import com.railguard.entity.RiskLevel;
import com.railguard.entity.Severity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service calculating preliminary AI-assisted risk scores and risk levels.
 *
 * DISCLAIMER / NOTICE:
 * This risk assessment calculation is a preliminary, demo AI-assisted heuristic evaluation.
 * It does NOT constitute a railway safety certification, official engineering clearance,
 * or scientifically validated structural safety score.
 */
@Service
public class RiskAssessmentService {

    public static class RiskResult {
        private final double score;
        private final RiskLevel level;

        public RiskResult(double score, RiskLevel level) {
            this.score = score;
            this.level = level;
        }

        public double getScore() { return score; }
        public RiskLevel getLevel() { return level; }
    }

    public RiskResult calculateRisk(List<Detection> detections) {
        if (detections == null || detections.isEmpty()) {
            return new RiskResult(0.0, RiskLevel.LOW);
        }

        double totalScore = 0.0;

        for (Detection detection : detections) {
            double severityWeight = getSeverityWeight(detection.getSeverity());
            double confidence = detection.getConfidence() != null ? detection.getConfidence() : 0.5;
            totalScore += (severityWeight * confidence);
        }

        // Cap maximum risk score at 100.0
        double finalScore = Math.min(100.0, Math.round(totalScore * 100.0) / 100.0);

        RiskLevel level;
        if (finalScore >= 75.0) {
            level = RiskLevel.CRITICAL;
        } else if (finalScore >= 50.0) {
            level = RiskLevel.HIGH;
        } else if (finalScore >= 25.0) {
            level = RiskLevel.MEDIUM;
        } else {
            level = RiskLevel.LOW;
        }

        // Safety override: A detected CRITICAL or HIGH severity defect must not be downgraded by confidence
        boolean hasCritical = detections.stream().anyMatch(d -> d.getSeverity() == Severity.CRITICAL || (d.getDefectClass() != null && d.getDefectClass().toLowerCase().contains("broken")));
        boolean hasHigh = detections.stream().anyMatch(d -> d.getSeverity() == Severity.HIGH);

        if (hasCritical) {
            level = RiskLevel.CRITICAL;
            finalScore = Math.max(finalScore, 85.0);
        } else if (hasHigh && level == RiskLevel.LOW) {
            level = RiskLevel.HIGH;
            finalScore = Math.max(finalScore, 65.0);
        }

        return new RiskResult(finalScore, level);
    }

    private double getSeverityWeight(Severity severity) {
        if (severity == null) return 5.0;
        return switch (severity) {
            case CRITICAL -> 40.0;
            case HIGH -> 25.0;
            case MEDIUM -> 15.0;
            case LOW -> 5.0;
        };
    }
}
