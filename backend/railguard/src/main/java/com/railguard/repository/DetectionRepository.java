package com.railguard.repository;

import com.railguard.entity.Detection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Data access repository for the Detection entity.
 */
@Repository
public interface DetectionRepository extends JpaRepository<Detection, Long> {

    List<Detection> findByInspectionId(Long inspectionId);

    void deleteByInspectionId(Long inspectionId);
}
