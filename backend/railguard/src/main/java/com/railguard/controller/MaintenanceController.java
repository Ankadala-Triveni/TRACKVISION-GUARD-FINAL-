package com.railguard.controller;

import com.railguard.dto.CreateMaintenanceTaskRequest;
import com.railguard.dto.MaintenanceTaskResponse;
import com.railguard.dto.UpdateMaintenanceStatusRequest;
import com.railguard.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller managing railway maintenance tasks and work orders.
 */
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping
    public ResponseEntity<MaintenanceTaskResponse> createMaintenanceTask(
            @Valid @RequestBody CreateMaintenanceTaskRequest request
    ) {
        return new ResponseEntity<>(maintenanceService.createTask(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceTaskResponse>> getAllMaintenanceTasks() {
        return ResponseEntity.ok(maintenanceService.getAllTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceTaskResponse> getMaintenanceTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.getTaskById(id));
    }

    @GetMapping("/inspection/{inspectionId}")
    public ResponseEntity<List<MaintenanceTaskResponse>> getTasksByInspection(@PathVariable Long inspectionId) {
        return ResponseEntity.ok(maintenanceService.getTasksByInspection(inspectionId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<MaintenanceTaskResponse> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMaintenanceStatusRequest request
    ) {
        return ResponseEntity.ok(maintenanceService.updateTaskStatus(id, request));
    }
}

