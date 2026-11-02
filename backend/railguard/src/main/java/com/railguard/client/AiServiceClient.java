package com.railguard.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Client managing HTTP communications between Spring Boot backend and Python FastAPI AI microservice.
 * Handles AI_SERVICE_UNAVAILABLE and MODEL_NOT_AVAILABLE error states gracefully.
 */
@Component
public class AiServiceClient {

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;
    private final ObjectMapper objectMapper;

    public static class AiResponse {
        private final String status;
        private final String modelVersion;
        private final String errorMessage;
        private final List<AiDetectionData> detections;

        public AiResponse(String status, String modelVersion, String errorMessage, List<AiDetectionData> detections) {
            this.status = status;
            this.modelVersion = modelVersion;
            this.errorMessage = errorMessage;
            this.detections = detections;
        }

        public String getStatus() { return status; }
        public String getModelVersion() { return modelVersion; }
        public String getErrorMessage() { return errorMessage; }
        public List<AiDetectionData> getDetections() { return detections; }
    }

    public static class AiDetectionData {
        private final String defectClass;
        private final Double confidence;
        private final String severity;
        private final Double xMin;
        private final Double yMin;
        private final Double xMax;
        private final Double yMax;

        public AiDetectionData(String defectClass, Double confidence, String severity, Double xMin, Double yMin, Double xMax, Double yMax) {
            this.defectClass = defectClass;
            this.confidence = confidence;
            this.severity = severity;
            this.xMin = xMin;
            this.yMin = yMin;
            this.xMax = xMax;
            this.yMax = yMax;
        }

        public String getDefectClass() { return defectClass; }
        public Double getConfidence() { return confidence; }
        public String getSeverity() { return severity; }
        public Double getXMin() { return xMin; }
        public Double getYMin() { return yMin; }
        public Double getXMax() { return xMax; }
        public Double getYMax() { return yMax; }
    }

    public AiServiceClient(@Value("${app.ai.service-url:http://localhost:8000}") String aiServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.aiServiceUrl = aiServiceUrl;
        this.objectMapper = new ObjectMapper();
    }

    public AiResponse analyzeImage(Long inspectionId, Path imageFilePath) {
        String url = aiServiceUrl + "/predict";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("inspection_id", inspectionId);
        body.add("file", new FileSystemResource(imageFilePath.toFile()));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String status = root.path("status").asText("FAILED");
                String modelVersion = root.path("model_version").isNull() ? null : root.path("model_version").asText();
                String error = root.path("error").isNull() ? null : root.path("error").asText();

                List<AiDetectionData> detections = new ArrayList<>();
                JsonNode detectionsNode = root.path("detections");
                if (detectionsNode.isArray()) {
                    for (JsonNode node : detectionsNode) {
                        String defectClass = node.path("defect_class").asText("UNKNOWN");
                        Double confidence = node.path("confidence").asDouble(0.5);
                        String severity = node.path("severity").asText("MEDIUM");
                        JsonNode bbox = node.path("bounding_box");
                        Double xMin = bbox.path("x_min").asDouble(0.0);
                        Double yMin = bbox.path("y_min").asDouble(0.0);
                        Double xMax = bbox.path("x_max").asDouble(0.0);
                        Double yMax = bbox.path("y_max").asDouble(0.0);

                        detections.add(new AiDetectionData(defectClass, confidence, severity, xMin, yMin, xMax, yMax));
                    }
                }

                return new AiResponse(status, modelVersion, error, detections);
            } else {
                return new AiResponse("FAILED", null, "AI_SERVICE_UNAVAILABLE", List.of());
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // FastAPI service is offline/unreachable
            return new AiResponse("FAILED", null, "AI_SERVICE_UNAVAILABLE", List.of());
        } catch (Exception e) {
            // Malformed data or general error
            return new AiResponse("FAILED", null, "INVALID_AI_RESPONSE", List.of());
        }
    }
}
