package com.railguard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railguard.dto.CreateInspectionRequest;
import com.railguard.dto.RegisterRequest;
import com.railguard.entity.Role;
import com.railguard.entity.User;
import com.railguard.client.AiServiceClient;
import com.railguard.repository.DetectionRepository;
import com.railguard.repository.InspectionRepository;
import com.railguard.repository.UserRepository;
import com.railguard.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 3 Integration Tests verifying Inspection Management, Security Authorization Matrix,
 * Tika Magic-Byte Upload Validation, Risk Scoring, and FastAPI error handling.
 */
@SpringBootTest(properties = {
    "JWT_SECRET=dGhpcy1pcy1hLXNlY3VyZS1hbmQtc3VmZmljaWVudGx5LWxvbmctc2VjcmV0LWtleS1mb3ItZGV2LTIwMjYtcmFpbGd1YXJk"
})
@AutoConfigureMockMvc
public class InspectionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InspectionRepository inspectionRepository;

    @Autowired
    private DetectionRepository detectionRepository;

    @Autowired
    private com.railguard.repository.MaintenanceTaskRepository maintenanceTaskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private AiServiceClient aiServiceClient;

    private User inspectorUser;
    private User engineerUser;
    private User maintenanceUser;
    private String inspectorToken;
    private String engineerToken;
    private String maintenanceToken;

    @BeforeEach
    void setUp() {
        maintenanceTaskRepository.deleteAll();
        detectionRepository.deleteAll();
        inspectionRepository.deleteAll();
        userRepository.deleteAll();

        // Create Inspector User
        inspectorUser = userRepository.save(User.builder()
                .fullName("Test Inspector")
                .username("inspector_test")
                .email("inspector@test.com")
                .password(passwordEncoder.encode("Password@123"))
                .role(Role.INSPECTOR)
                .build());
        inspectorToken = jwtService.generateToken(inspectorUser.getId(), inspectorUser.getUsername(), inspectorUser.getRole().name());

        // Create Engineer User
        engineerUser = userRepository.save(User.builder()
                .fullName("Test Engineer")
                .username("engineer_test")
                .email("engineer@test.com")
                .password(passwordEncoder.encode("Password@123"))
                .role(Role.ENGINEER)
                .build());
        engineerToken = jwtService.generateToken(engineerUser.getId(), engineerUser.getUsername(), engineerUser.getRole().name());

        // Create Maintenance User
        maintenanceUser = userRepository.save(User.builder()
                .fullName("Test Maintenance")
                .username("maint_test")
                .email("maint@test.com")
                .password(passwordEncoder.encode("Password@123"))
                .role(Role.MAINTENANCE)
                .build());
        maintenanceToken = jwtService.generateToken(maintenanceUser.getId(), maintenanceUser.getUsername(), maintenanceUser.getRole().name());
    }

    @Test
    void testCreateInspectionPermissions() throws Exception {
        CreateInspectionRequest req = new CreateInspectionRequest("Northern Railway", "SEC-101", "KM 12/4", 28.61, 77.20);

        // 1. Unauthenticated -> 401
        mockMvc.perform(post("/api/inspections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // 2. MAINTENANCE role attempting creation -> 403 Forbidden (Phase 3 Rule)
        mockMvc.perform(post("/api/inspections")
                .header("Authorization", "Bearer " + maintenanceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // 3. INSPECTOR role creation -> 201 Created
        mockMvc.perform(post("/api/inspections")
                .header("Authorization", "Bearer " + inspectorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.inspectionNumber").exists())
                .andExpect(jsonPath("$.username").value("inspector_test"));

        // 4. Optional metadata defaults when null or empty
        CreateInspectionRequest emptyReq = new CreateInspectionRequest(null, "", null, null, null);
        mockMvc.perform(post("/api/inspections")
                .header("Authorization", "Bearer " + inspectorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.railwayZone").value("Unspecified"))
                .andExpect(jsonPath("$.trackSection").value("Unspecified"))
                .andExpect(jsonPath("$.location").value("Location unavailable"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void testFileValidationAndUploadSecurity() throws Exception {
        // Create inspection record first
        CreateInspectionRequest req = new CreateInspectionRequest("Northern Railway", "SEC-101", "KM 12/4", 28.61, 77.20);
        MvcResult createRes = mockMvc.perform(post("/api/inspections")
                .header("Authorization", "Bearer " + inspectorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long inspId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // 1. Upload text file renamed to .png (Magic-byte Tika validation should reject)
        MockMultipartFile fakePng = new MockMultipartFile("file", "script.png", "image/png", "echo 'malicious script'".getBytes());
        mockMvc.perform(multipart("/api/inspections/" + inspId + "/upload")
                .file(fakePng)
                .header("Authorization", "Bearer " + inspectorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Security violation: Magic-byte inspection failed. Content type 'text/plain' is not a valid image."));

        // 2. Upload valid PNG image magic bytes
        byte[] validPngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile validPng = new MockMultipartFile("file", "track_photo.png", "image/png", validPngHeader);

        MvcResult uploadRes = mockMvc.perform(multipart("/api/inspections/" + inspId + "/upload")
                .file(validPng)
                .header("Authorization", "Bearer " + inspectorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.filePath").exists())
                .andReturn();

        // Verify database stores RELATIVE path only (never absolute system paths!)
        String filePath = objectMapper.readTree(uploadRes.getResponse().getContentAsString()).get("filePath").asText();
        assertFalse(filePath.contains("C:"));
        assertFalse(filePath.contains("Users"));
        assertTrue(filePath.startsWith("inspections/" + inspId));
    }

    @Test
    void testAIModelUnavailableHandling() throws Exception {
        // Create and upload valid image
        CreateInspectionRequest req = new CreateInspectionRequest("Northern Railway", "SEC-101", "KM 12/4", 28.61, 77.20);
        MvcResult createRes = mockMvc.perform(post("/api/inspections")
                .header("Authorization", "Bearer " + inspectorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long inspId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        byte[] validPngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile validPng = new MockMultipartFile("file", "track_photo.png", "image/png", validPngHeader);
        mockMvc.perform(multipart("/api/inspections/" + inspId + "/upload")
                .file(validPng)
                .header("Authorization", "Bearer " + inspectorToken))
                .andExpect(status().isOk());

        // Trigger AI analysis when FastAPI is offline/model is missing -> Status should transition to FAILED with error code
        mockMvc.perform(post("/api/inspections/" + inspId + "/analyze")
                .header("Authorization", "Bearer " + inspectorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.analysisError").exists());
    }

    @Test
    void testCleanTrackInspectionWithZeroDetections() throws Exception {
        // Create and upload valid image
        CreateInspectionRequest req = new CreateInspectionRequest("Northern Railway", "SEC-101", "KM 12/4", 28.61, 77.20);
        MvcResult createRes = mockMvc.perform(post("/api/inspections")
                .header("Authorization", "Bearer " + inspectorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long inspId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        byte[] validPngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile validPng = new MockMultipartFile("file", "clean_track.png", "image/png", validPngHeader);
        mockMvc.perform(multipart("/api/inspections/" + inspId + "/upload")
                .file(validPng)
                .header("Authorization", "Bearer " + inspectorToken))
                .andExpect(status().isOk());

        // Mock clean track response (status SUCCESS, 0 detections)
        Mockito.doReturn(new AiServiceClient.AiResponse("SUCCESS", "YOLO11-RailGuard-v3.0", null, Collections.emptyList()))
                .when(aiServiceClient).analyzeImage(eq(inspId), any(Path.class));

        // Trigger AI analysis -> Should complete with COMPLETED status, LOW risk, risk score 0.0
        mockMvc.perform(post("/api/inspections/" + inspId + "/analyze")
                .header("Authorization", "Bearer " + inspectorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.riskScore").value(0.0))
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.modelVersion").value("YOLO11-RailGuard-v3.0"))
                .andExpect(jsonPath("$.analysisError").doesNotExist())
                .andExpect(jsonPath("$.detections").isEmpty());
    }
}
