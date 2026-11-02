package com.railguard.service;

import com.railguard.client.AiServiceClient;
import com.railguard.dto.*;
import com.railguard.entity.*;
import com.railguard.exception.DuplicateResourceException;
import com.railguard.repository.DetectionRepository;
import com.railguard.repository.InspectionRepository;
import com.railguard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service orchestrating inspection lifecycles, status state transitions, image upload,
 * AI service communication, and risk score calculation.
 */
@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final DetectionRepository detectionRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AiServiceClient aiServiceClient;
    private final RiskAssessmentService riskAssessmentService;

    @Transactional
    public InspectionResponse createInspection(CreateInspectionRequest request) {
        User currentUser = getCurrentAuthenticatedUser();

        // Security role check: Only INSPECTOR and ADMIN can create inspections
        if (currentUser.getRole() != Role.INSPECTOR && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only Inspectors and Admins can create inspection records.");
        }

        String inspectionNumber = generateInspectionNumber();

        String rZone = (request.getRailwayZone() != null && !request.getRailwayZone().trim().isEmpty())
                ? request.getRailwayZone().trim()
                : "Unspecified";

        String tSection = (request.getTrackSection() != null && !request.getTrackSection().trim().isEmpty())
                ? request.getTrackSection().trim()
                : "Unspecified";

        String loc = (request.getLocation() != null && !request.getLocation().trim().isEmpty())
                ? request.getLocation().trim()
                : "Location unavailable";

        Inspection inspection = Inspection.builder()
                .inspectionNumber(inspectionNumber)
                .user(currentUser)
                .inspectionDate(LocalDateTime.now())
                .railwayZone(rZone)
                .trackSection(tSection)
                .location(loc)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(InspectionStatus.CREATED)
                .riskScore(0.0)
                .riskLevel(RiskLevel.LOW)
                .build();

        Inspection saved = inspectionRepository.save(inspection);
        return mapToResponse(saved, List.of());
    }

    @Transactional
    public InspectionResponse uploadImage(Long inspectionId, MultipartFile file) {
        User currentUser = getCurrentAuthenticatedUser();
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));

        // Ownership & Role Enforcement: Owner or ADMIN
        verifyOwnershipOrAdmin(inspection, currentUser);

        // Store file and get relative storage key path
        String relativeFilePathKey = fileStorageService.storeFile(inspectionId, file);

        inspection.setOriginalFilename(file.getOriginalFilename());
        inspection.setStoredFilename(Path.of(relativeFilePathKey).getFileName().toString());
        inspection.setFilePath(relativeFilePathKey); // Relative storage path ONLY
        inspection.setMimeType(file.getContentType());
        inspection.setFileSize(file.getSize());
        inspection.setStatus(InspectionStatus.UPLOADED);

        Inspection updated = inspectionRepository.save(inspection);
        List<Detection> existingDetections = detectionRepository.findByInspectionId(inspectionId);
        return mapToResponse(updated, existingDetections);
    }

    @Transactional
    public InspectionResponse analyzeInspection(Long inspectionId) {
        User currentUser = getCurrentAuthenticatedUser();
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));

        // Security check: INSPECTOR (own), ENGINEER, ADMIN
        verifyAnalysisPermission(inspection, currentUser);

        if (inspection.getFilePath() == null || inspection.getStatus() == InspectionStatus.CREATED) {
            throw new IllegalStateException("Inspection does not have an uploaded image to analyze.");
        }

        // Transition status to PROCESSING
        inspection.setStatus(InspectionStatus.PROCESSING);
        inspection.setAnalysisStartedAt(LocalDateTime.now());
        inspection.setAnalysisError(null);
        inspectionRepository.save(inspection);

        // Load local file path
        Path localImagePath = fileStorageService.loadFileAsPath(inspection.getFilePath());

        // Dispatch request to FastAPI AI service
        AiServiceClient.AiResponse aiResponse = aiServiceClient.analyzeImage(inspectionId, localImagePath);

        inspection.setAnalysisCompletedAt(LocalDateTime.now());

        if ("SUCCESS".equalsIgnoreCase(aiResponse.getStatus())) {
            // Delete old detections if re-analyzing
            detectionRepository.deleteByInspectionId(inspectionId);

            List<Detection> savedDetections = new ArrayList<>();
            Double riskScore = 0.0;
            RiskLevel riskLevel = RiskLevel.LOW;

            if (aiResponse.getDetections() != null && !aiResponse.getDetections().isEmpty()) {
                List<Detection> newDetections = new ArrayList<>();
                for (AiServiceClient.AiDetectionData dData : aiResponse.getDetections()) {
                    Severity severity = parseSeverity(dData.getSeverity());
                    Detection detection = Detection.builder()
                            .inspection(inspection)
                            .defectClass(dData.getDefectClass())
                            .confidence(dData.getConfidence())
                            .severity(severity)
                            .xMin(dData.getXMin())
                            .yMin(dData.getYMin())
                            .xMax(dData.getXMax())
                            .yMax(dData.getYMax())
                            .modelVersion(aiResponse.getModelVersion())
                            .detectedAt(LocalDateTime.now())
                            .build();
                    newDetections.add(detection);
                }

                savedDetections = detectionRepository.saveAll(newDetections);

                // Compute risk assessment based on detections
                RiskAssessmentService.RiskResult riskResult = riskAssessmentService.calculateRisk(savedDetections);
                riskScore = riskResult.getScore();
                riskLevel = riskResult.getLevel();
            }

            // A clean track (0 detections) or defected track both successfully complete analysis
            inspection.setStatus(InspectionStatus.COMPLETED);
            inspection.setRiskScore(riskScore);
            inspection.setRiskLevel(riskLevel);
            inspection.setModelVersion(aiResponse.getModelVersion());
            inspection.setAnalysisError(null);

            Inspection completed = inspectionRepository.save(inspection);
            return mapToResponse(completed, savedDetections);
        } else {
            // AI failure or MODEL_NOT_AVAILABLE handling
            inspection.setStatus(InspectionStatus.FAILED);
            inspection.setAnalysisError(aiResponse.getErrorMessage() != null ? aiResponse.getErrorMessage() : "MODEL_NOT_AVAILABLE");
            inspection.setModelVersion(null);

            Inspection failed = inspectionRepository.save(inspection);
            List<Detection> existingDetections = detectionRepository.findByInspectionId(inspectionId);
            return mapToResponse(failed, existingDetections);
        }
    }

    @Transactional(readOnly = true)
    public InspectionResponse getInspection(Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + id));

        verifyViewPermission(inspection, currentUser);
        List<Detection> detections = detectionRepository.findByInspectionId(id);
        return mapToResponse(inspection, detections);
    }

    @Transactional(readOnly = true)
    public Inspection getInspectionEntity(Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + id));
        verifyViewPermission(inspection, currentUser);
        return inspection;
    }

    @Transactional(readOnly = true)
    public List<Inspection> getAllInspectionEntities() {
        return inspectionRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<InspectionResponse> getInspections() {
        User currentUser = getCurrentAuthenticatedUser();
        List<Inspection> inspections;

        if (currentUser.getRole() == Role.INSPECTOR) {
            // Inspectors see only their own inspections
            inspections = inspectionRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        } else if (currentUser.getRole() == Role.ENGINEER || currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MAINTENANCE) {
            // Engineers, Admins, and Maintenance personnel view all inspections
            inspections = inspectionRepository.findAllByOrderByCreatedAtDesc();
        } else {
            throw new AccessDeniedException("Access denied for your role.");
        }

        return inspections.stream()
                .map(ins -> mapToResponse(ins, detectionRepository.findByInspectionId(ins.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Resource getImageResource(Long inspectionId) {
        User currentUser = getCurrentAuthenticatedUser();
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));

        verifyViewPermission(inspection, currentUser);

        if (inspection.getFilePath() == null || inspection.getFilePath().isBlank()) {
            throw new IllegalArgumentException("No image uploaded for inspection: " + inspectionId);
        }

        try {
            Path path = fileStorageService.loadFileAsPath(inspection.getFilePath());
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("Image file cannot be read: " + inspection.getFilePath());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid image file path URI: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<DetectionResponse> getDetections(Long inspectionId) {
        User currentUser = getCurrentAuthenticatedUser();
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));

        verifyViewPermission(inspection, currentUser);
        return detectionRepository.findByInspectionId(inspectionId).stream()
                .map(this::mapToDetectionResponse)
                .collect(Collectors.toList());
    }

    // --- Helper & Security Validation Methods ---

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Full authentication is required.");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found."));
    }

    private void verifyOwnershipOrAdmin(Inspection inspection, User user) {
        if (user.getRole() != Role.ADMIN && !inspection.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to modify this inspection.");
        }
    }

    private void verifyAnalysisPermission(Inspection inspection, User user) {
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.ENGINEER) {
            return;
        }
        if (user.getRole() == Role.INSPECTOR && inspection.getUser().getId().equals(user.getId())) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to analyze this inspection.");
    }

    private void verifyViewPermission(Inspection inspection, User user) {
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.ENGINEER || user.getRole() == Role.MAINTENANCE) {
            return;
        }
        if (user.getRole() == Role.INSPECTOR && inspection.getUser().getId().equals(user.getId())) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to view this inspection.");
    }

    private String generateInspectionNumber() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "INS-" + dateStr + "-" + suffix;
    }

    private Severity parseSeverity(String severityStr) {
        if (severityStr == null) return Severity.MEDIUM;
        try {
            return Severity.valueOf(severityStr.toUpperCase());
        } catch (Exception e) {
            return Severity.MEDIUM;
        }
    }

    private InspectionResponse mapToResponse(Inspection inspection, List<Detection> detections) {
        List<DetectionResponse> detectionResponses = detections.stream()
                .map(this::mapToDetectionResponse)
                .collect(Collectors.toList());

        return InspectionResponse.builder()
                .id(inspection.getId())
                .inspectionNumber(inspection.getInspectionNumber())
                .userId(inspection.getUser().getId())
                .username(inspection.getUser().getUsername())
                .inspectionDate(inspection.getInspectionDate())
                .railwayZone(inspection.getRailwayZone())
                .trackSection(inspection.getTrackSection())
                .location(inspection.getLocation())
                .latitude(inspection.getLatitude())
                .longitude(inspection.getLongitude())
                .originalFilename(inspection.getOriginalFilename())
                .filePath(inspection.getFilePath()) // Relative path key ONLY
                .status(inspection.getStatus().name())
                .riskScore(inspection.getRiskScore())
                .riskLevel(inspection.getRiskLevel() != null ? inspection.getRiskLevel().name() : RiskLevel.LOW.name())
                .analysisStartedAt(inspection.getAnalysisStartedAt())
                .analysisCompletedAt(inspection.getAnalysisCompletedAt())
                .analysisError(inspection.getAnalysisError())
                .modelVersion(inspection.getModelVersion())
                .createdAt(inspection.getCreatedAt())
                .updatedAt(inspection.getUpdatedAt())
                .detections(detectionResponses)
                .build();
    }

    private DetectionResponse mapToDetectionResponse(Detection d) {
        return DetectionResponse.builder()
                .id(d.getId())
                .defectClass(d.getDefectClass())
                .confidence(d.getConfidence())
                .severity(d.getSeverity() != null ? d.getSeverity().name() : Severity.MEDIUM.name())
                .boundingBox(BoundingBoxDto.builder()
                        .xMin(d.getXMin())
                        .yMin(d.getYMin())
                        .xMax(d.getXMax())
                        .yMax(d.getYMax())
                        .build())
                .modelVersion(d.getModelVersion())
                .detectedAt(d.getDetectedAt())
                .build();
    }
}
