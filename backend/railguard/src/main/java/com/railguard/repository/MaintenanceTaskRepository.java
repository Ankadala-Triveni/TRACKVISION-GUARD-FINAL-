package com.railguard.repository;

import com.railguard.entity.MaintenanceStatus;
import com.railguard.entity.MaintenanceTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, Long> {
    List<MaintenanceTask> findAllByOrderByCreatedAtDesc();
    List<MaintenanceTask> findByInspectionId(Long inspectionId);
    List<MaintenanceTask> findByStatus(MaintenanceStatus status);
    Optional<MaintenanceTask> findByTaskNumber(String taskNumber);
    long countByStatus(MaintenanceStatus status);
}

