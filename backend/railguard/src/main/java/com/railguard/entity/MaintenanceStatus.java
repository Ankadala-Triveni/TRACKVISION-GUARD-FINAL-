package com.railguard.entity;

/**
 * Lifecycle execution state of a railway maintenance task.
 */
public enum MaintenanceStatus {
    PENDING,
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    VERIFIED,
    CANCELLED
}

