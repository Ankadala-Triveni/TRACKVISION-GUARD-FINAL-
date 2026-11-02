package com.railguard.repository;

import com.railguard.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Data access repository for the Inspection entity.
 */
@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {

    List<Inspection> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Inspection> findAllByOrderByCreatedAtDesc();

    Optional<Inspection> findByIdAndUserId(Long id, Long userId);

    boolean existsByInspectionNumber(String inspectionNumber);
}
