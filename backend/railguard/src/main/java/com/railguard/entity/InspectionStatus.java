package com.railguard.entity;

/**
 * Inspection lifecycle status enum.
 * State Transitions:
 * CREATED -> UPLOADED -> PROCESSING -> COMPLETED or FAILED
 */
public enum InspectionStatus {
    CREATED,
    UPLOADED,
    PROCESSING,
    COMPLETED,
    FAILED
}
