package com.railguard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railguard.dto.CreateMaintenanceTaskRequest;
import com.railguard.dto.UpdateMaintenanceStatusRequest;
import com.railguard.entity.Inspection;
import com.railguard.entity.InspectionStatus;
import com.railguard.entity.RiskLevel;
import com.railguard.entity.Role;
import com.railguard.entity.User;
import com.railguard.repository.DetectionRepository;
import com.railguard.repository.InspectionRepository;
import com.railguard.repository.MaintenanceTaskRepository;
import com.railguard.repository.UserRepository;
import com.railguard.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "JWT_SECRET=dGhpcy1pcy1hLXNlY3VyZS1hbmQtc3VmZmljaWVudGx5LWxvbmctc2VjcmV0LWtleS1mb3ItZGV2LTIwMjYtcmFpbGd1YXJk"
})
@AutoConfigureMockMvc
public class MaintenanceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InspectionRepository inspectionRepository;

    @Autowired
    private DetectionRepository detectionRepository;

    @Autowired
    private MaintenanceTaskRepository maintenanceTaskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User inspector;
    private String inspectorToken;
    private Inspection inspection;

    @BeforeEach
    void setUp() {
        maintenanceTaskRepository.deleteAll();
        detectionRepository.deleteAll();
        inspectionRepository.deleteAll();
        userRepository.deleteAll();

        inspector = userRepository.save(User.builder()
                .fullName("Maintenance Field Tester")
                .username("maint_tester")
                .email("maint_tester@railguard.ai")
                .password(passwordEncoder.encode("Password@123"))
                .role(Role.INSPECTOR)
                .build());
        inspectorToken = jwtService.generateToken(inspector.getId(), inspector.getUsername(), inspector.getRole().name());

        inspection = inspectionRepository.save(Inspection.builder()
                .inspectionNumber("INS-TEST-MAINT-01")
                .user(inspector)
                .inspectionDate(LocalDateTime.now())
                .railwayZone("Southern Railway (SR)")
                .trackSection("SEC-CHENNAI-01")
                .location("KM 12/4")
                .latitude(13.0827)
                .longitude(80.2707)
                .status(InspectionStatus.COMPLETED)
                .riskScore(45.0)
                .riskLevel(RiskLevel.HIGH)
                .build());
    }

    @Test
    void testCreateAndManageMaintenanceWorkflow() throws Exception {
        CreateMaintenanceTaskRequest createReq = CreateMaintenanceTaskRequest.builder()
                .inspectionId(inspection.getId())
                .title("Emergency Joint Re-alignment")
                .description("Joint bar fissure detected by YOLO11 requiring immediate welding.")
                .priority("HIGH")
                .assignedTo("Rapid Response Engineering Unit")
                .scheduledDate(LocalDateTime.now().plusDays(1))
                .build();

        // 1. Create Maintenance Task
        mockMvc.perform(post("/api/maintenance")
                .header("Authorization", "Bearer " + inspectorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskNumber", notNullValue()))
                .andExpect(jsonPath("$.title", is("Emergency Joint Re-alignment")))
                .andExpect(jsonPath("$.status", is("SCHEDULED")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.inspectionNumber", is("INS-TEST-MAINT-01")));

        Long taskId = maintenanceTaskRepository.findAll().get(0).getId();

        // 2. Transition status to IN_PROGRESS
        UpdateMaintenanceStatusRequest updateReq1 = UpdateMaintenanceStatusRequest.builder()
                .status("IN_PROGRESS")
                .completionNotes("Crew arrived on site.")
                .build();

        mockMvc.perform(put("/api/maintenance/" + taskId + "/status")
                .header("Authorization", "Bearer " + inspectorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.completionNotes", is("Crew arrived on site.")));

        // 3. Transition status to COMPLETED
        UpdateMaintenanceStatusRequest updateReq2 = UpdateMaintenanceStatusRequest.builder()
                .status("COMPLETED")
                .completionNotes("Welding certified and track opened.")
                .build();

        mockMvc.perform(put("/api/maintenance/" + taskId + "/status")
                .header("Authorization", "Bearer " + inspectorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.completionNotes", is("Welding certified and track opened.")));

        // 4. Retrieve All Maintenance Tasks
        mockMvc.perform(get("/api/maintenance")
                .header("Authorization", "Bearer " + inspectorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].status", is("COMPLETED")));
    }
}

