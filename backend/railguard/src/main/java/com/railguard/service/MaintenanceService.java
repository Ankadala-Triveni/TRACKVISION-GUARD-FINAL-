package com.railguard.service;

import com.railguard.dto.CreateMaintenanceTaskRequest;
import com.railguard.dto.MaintenanceTaskResponse;
import com.railguard.dto.UpdateMaintenanceStatusRequest;
import com.railguard.entity.*;
import com.railguard.repository.InspectionRepository;
import com.railguard.repository.MaintenanceTaskRepository;
import com.railguard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service orchestrating maintenance workflows, work order tracking, and lifecycle updates.
 */
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceTaskRepository maintenanceTaskRepository;
    private final InspectionRepository inspectionRepository;
    private final UserRepository userRepository;
    private final com.railguard.repository.DetectionRepository detectionRepository;

    @Transactional
    public MaintenanceTaskResponse createTask(CreateMaintenanceTaskRequest request) {
        User currentUser = getCurrentAuthenticatedUser();

        // Security check: Only ADMIN, ENGINEER, and INSPECTOR can create maintenance tasks
        if (currentUser.getRole() == Role.MAINTENANCE) {
            // Maintenance personnel work on existing tasks; creation reserved for inspectors/engineers/admins
            throw new AccessDeniedException("Maintenance crew can update tasks, but creation is reserved for Inspectors, Engineers, and Admins.");
        }

        Inspection inspection = inspectionRepository.findById(request.getInspectionId())
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + request.getInspectionId()));

        // Prevent duplicate active work orders for the same inspection
        List<MaintenanceTask> existingTasks = maintenanceTaskRepository.findByInspectionId(request.getInspectionId());
        for (MaintenanceTask ex : existingTasks) {
            if (ex.getStatus() != MaintenanceStatus.COMPLETED && ex.getStatus() != MaintenanceStatus.CANCELLED) {
                return mapToResponse(ex);
            }
        }

        MaintenancePriority priority = parsePriority(request.getPriority());
        String taskNumber = generateTaskNumber();

        MaintenanceTask task = MaintenanceTask.builder()
                .taskNumber(taskNumber)
                .inspection(inspection)
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .priority(priority)
                .status(MaintenanceStatus.SCHEDULED)
                .assignedTo(request.getAssignedTo().trim())
                .scheduledDate(request.getScheduledDate() != null ? request.getScheduledDate() : LocalDateTime.now().plusDays(2))
                .completionNotes(null)
                .createdBy(currentUser)
                .build();

        MaintenanceTask saved = maintenanceTaskRepository.save(task);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskResponse> getAllTasks() {
        return maintenanceTaskRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MaintenanceTaskResponse getTaskById(Long id) {
        MaintenanceTask task = maintenanceTaskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance task not found: " + id));
        return mapToResponse(task);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskResponse> getTasksByInspection(Long inspectionId) {
        return maintenanceTaskRepository.findByInspectionId(inspectionId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MaintenanceTaskResponse updateTaskStatus(Long id, UpdateMaintenanceStatusRequest request) {
        MaintenanceTask task = maintenanceTaskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance task not found: " + id));

        MaintenanceStatus newStatus = parseStatus(request.getStatus());
        task.setStatus(newStatus);

        if (request.getCompletionNotes() != null && !request.getCompletionNotes().isBlank()) {
            task.setCompletionNotes(request.getCompletionNotes().trim());
        }

        MaintenanceTask updated = maintenanceTaskRepository.save(task);
        return mapToResponse(updated);
    }

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Full authentication is required.");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found."));
    }

    private String generateTaskNumber() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "MNT-" + dateStr + "-" + suffix;
    }

    private MaintenancePriority parsePriority(String priorityStr) {
        if (priorityStr == null) return MaintenancePriority.MEDIUM;
        try {
            return MaintenancePriority.valueOf(priorityStr.toUpperCase());
        } catch (Exception e) {
            return MaintenancePriority.MEDIUM;
        }
    }

    private MaintenanceStatus parseStatus(String statusStr) {
        if (statusStr == null) return MaintenanceStatus.SCHEDULED;
        try {
            return MaintenanceStatus.valueOf(statusStr.toUpperCase());
        } catch (Exception e) {
            return MaintenanceStatus.SCHEDULED;
        }
    }

    private MaintenanceTaskResponse mapToResponse(MaintenanceTask task) {
        Inspection inspection = task.getInspection();
        List<Detection> dets = detectionRepository.findByInspectionId(inspection.getId());
        String defectCategory = (dets != null && !dets.isEmpty()) ? dets.get(0).getDefectClass() : "Healthy Track";

        return MaintenanceTaskResponse.builder()
                .id(task.getId())
                .taskNumber(task.getTaskNumber())
                .inspectionId(inspection.getId())
                .inspectionNumber(inspection.getInspectionNumber())
                .railwayZone(inspection.getRailwayZone())
                .trackSection(inspection.getTrackSection())
                .location(inspection.getLocation())
                .defectCategory(defectCategory)
                .riskLevel(inspection.getRiskLevel() != null ? inspection.getRiskLevel().name() : "LOW")
                .riskScore(inspection.getRiskScore())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority().name())
                .status(task.getStatus().name())
                .assignedTo(task.getAssignedTo())
                .scheduledDate(task.getScheduledDate())
                .completionNotes(task.getCompletionNotes())
                .createdByUsername(task.getCreatedBy() != null ? task.getCreatedBy().getUsername() : "System")
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}

