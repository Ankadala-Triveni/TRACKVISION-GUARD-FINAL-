package com.railguard.controller;

import com.railguard.dto.CreateInspectionRequest;
import com.railguard.dto.DetectionResponse;
import com.railguard.dto.InspectionResponse;
import com.railguard.service.InspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller exposing REST endpoints for railway track inspection management.
 * Business logic is kept separate in InspectionService.
 */
@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;
    private final com.railguard.service.PdfReportService pdfReportService;

    @PostMapping
    public ResponseEntity<InspectionResponse> createInspection(@Valid @RequestBody CreateInspectionRequest request) {
        return new ResponseEntity<>(inspectionService.createInspection(request), HttpStatus.CREATED);
    }

    @PostMapping(value = {"/{id}/upload", "/{id}/image"})
    public ResponseEntity<InspectionResponse> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(inspectionService.uploadImage(id, file));
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<InspectionResponse> analyzeInspection(@PathVariable Long id) {
        return ResponseEntity.ok(inspectionService.analyzeInspection(id));
    }

    @GetMapping
    public ResponseEntity<List<InspectionResponse>> getInspections() {
        return ResponseEntity.ok(inspectionService.getInspections());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InspectionResponse> getInspection(@PathVariable Long id) {
        return ResponseEntity.ok(inspectionService.getInspection(id));
    }

    @GetMapping("/{id}/detections")
    public ResponseEntity<List<DetectionResponse>> getDetections(@PathVariable Long id) {
        return ResponseEntity.ok(inspectionService.getDetections(id));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getInspectionImage(@PathVariable Long id) {
        Resource resource = inspectionService.getImageResource(id);
        InspectionResponse inspection = inspectionService.getInspection(id);
        MediaType mediaType = MediaType.IMAGE_JPEG;
        if (inspection.getOriginalFilename() != null && inspection.getOriginalFilename().toLowerCase().endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (inspection.getOriginalFilename() != null ? inspection.getOriginalFilename() : "inspection_" + id + ".jpg") + "\"")
                .body(resource);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPdfReport(@PathVariable Long id) {
        com.railguard.entity.Inspection inspection = inspectionService.getInspectionEntity(id);
        byte[] pdfBytes = pdfReportService.generateInspectionReport(inspection);

        String filename = "TrackVisionGuard_Report_" + 
                (inspection.getInspectionNumber() != null ? inspection.getInspectionNumber() : "INS-" + id) + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    @GetMapping(value = "/reports/high-risk-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadHighRiskSafetySummaryPdf() {
        List<com.railguard.entity.Inspection> all = inspectionService.getAllInspectionEntities();
        List<com.railguard.entity.Inspection> highRisk = all.stream()
                .filter(i -> i.getRiskLevel() == com.railguard.entity.RiskLevel.CRITICAL || i.getRiskLevel() == com.railguard.entity.RiskLevel.HIGH)
                .collect(java.util.stream.Collectors.toList());

        byte[] pdfBytes = pdfReportService.generateHighRiskSafetySummaryReport(highRisk);
        String filename = "TrackVisionGuard_HighRisk_Safety_Summary.pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }
}
