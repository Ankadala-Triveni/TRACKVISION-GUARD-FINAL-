package com.railguard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railguard.dto.LoginRequest;
import com.railguard.dto.RegisterRequest;
import com.railguard.entity.User;
import com.railguard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests validating Phase 2 security configurations:
 * Registration, BCrypt password hashing, unified login, JWT validation, and RBAC endpoint guards.
 */
@SpringBootTest(properties = {
    "JWT_SECRET=dGhpcy1pcy1hLXNlY3VyZS1hbmQtc3VmZmljaWVudGx5LWxvbmctc2VjcmV0LWtleS1mb3ItZGV2LTIwMjYtcmFpbGd1YXJk"
})
@AutoConfigureMockMvc
public class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.railguard.service.CaptchaService captchaService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.railguard.repository.InspectionRepository inspectionRepository;

    @Autowired
    private com.railguard.repository.DetectionRepository detectionRepository;

    @Autowired
    private com.railguard.repository.MaintenanceTaskRepository maintenanceTaskRepository;

    @BeforeEach
    void setUp() {
        maintenanceTaskRepository.deleteAll();
        detectionRepository.deleteAll();
        inspectionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testRegistrationAndValidation() throws Exception {
        RegisterRequest request = new RegisterRequest("Test User", "testuser", "test@example.com", "Password@123");

        // 1. Successful registration (Default role: INSPECTOR)
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("INSPECTOR"))
                .andExpect(jsonPath("$.password").doesNotExist()); // Confirm password fields are hidden

        // Confirm password was hashed with BCrypt
        User savedUser = userRepository.findByUsername("testuser").orElseThrow();
        assertTrue(passwordEncoder.matches("Password@123", savedUser.getPassword()));
        assertNotEquals("Password@123", savedUser.getPassword());

        // 2. Duplicate Username Rejected (returns 409 Conflict)
        RegisterRequest dupUser = new RegisterRequest("Dup Name", "testuser", "other@example.com", "Password@123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dupUser)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username is already taken"));

        // 3. Duplicate Email Rejected (returns 409 Conflict)
        RegisterRequest dupEmail = new RegisterRequest("Dup Name", "otheruser", "test@example.com", "Password@123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dupEmail)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"));

        // 4. Password too short (<8) Rejected (returns 400 Bad Request)
        RegisterRequest shortPass = new RegisterRequest("Short Pass", "shortuser", "short@example.com", "Pass123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shortPass)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void testLoginAndJwtSecuredEndpoints() throws Exception {
        // Register test user
        RegisterRequest reg = new RegisterRequest("Auth User", "authuser", "auth@example.com", "Password@123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // 1. Login with Username
        LoginRequest loginUser = new LoginRequest("authuser", "Password@123");
        MvcResult resUser = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("authuser"))
                .andExpect(jsonPath("$.role").value("INSPECTOR"))
                .andReturn();

        String token = objectMapper.readTree(resUser.getResponse().getContentAsString()).get("token").asText();

        // 2. Login with Email (Unified Login)
        LoginRequest loginEmail = new LoginRequest("auth@example.com", "Password@123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // 3. Bad Credentials Login Fails (returns 401)
        LoginRequest badLogin = new LoginRequest("authuser", "WrongPass");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));

        // 4. Secure Profile Route without JWT Rejected (returns 401)
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        // 5. Secure Profile Route with JWT Allowed
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("authuser"))
                .andExpect(jsonPath("$.role").value("INSPECTOR"));

        // 6. Access Authenticated Test Route Allowed
        mockMvc.perform(get("/api/test/authenticated")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Access granted to authenticated user"));

        // 7. Access Inspector Test Route (User is INSPECTOR, so allowed)
        mockMvc.perform(get("/api/test/inspector")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Access granted to INSPECTOR role"));

        // 8. Access Admin Test Route (User is INSPECTOR, so forbidden 403)
        mockMvc.perform(get("/api/test/admin")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have the required authorities to access this resource"));
    }

    @Test
    void testActuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testVisualCaptchaFlow() throws Exception {
        // 1. Generate visual image CAPTCHA
        MvcResult captchaRes = mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captchaId").exists())
                .andExpect(jsonPath("$.image").exists())
                .andReturn();

        String captchaJson = captchaRes.getResponse().getContentAsString();
        String captchaId = objectMapper.readTree(captchaJson).get("captchaId").asText();
        String imageBase64 = objectMapper.readTree(captchaJson).get("image").asText();

        assertTrue(imageBase64.startsWith("data:image/png;base64,"));

        // Register user for login verification
        userRepository.save(User.builder()
                .fullName("Captcha Test User")
                .username("captchanin")
                .email("captcha@test.com")
                .password(passwordEncoder.encode("Password@123"))
                .role(com.railguard.entity.Role.INSPECTOR)
                .enabled(true)
                .build());

        // 2. Attempt login with wrong CAPTCHA answer -> 400 Bad Request
        LoginRequest wrongCaptchaReq = new LoginRequest("captchanin", "Password@123", captchaId, "WRONG");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongCaptchaReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid, expired, or already-used CAPTCHA code. Please refresh and try again."));

        // 3. Attempt with non-existent or expired CAPTCHA ID -> 400 Bad Request
        LoginRequest nonExistentReq = new LoginRequest("captchanin", "Password@123", "non-existent-id", "ABCD2");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistentReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid, expired, or already-used CAPTCHA code. Please refresh and try again."));
    }

    @Test
    void testGoogleLoginEndpoint_validationAndRejection() throws Exception {
        // 1. Empty token should fail -> 400 Bad Request
        com.railguard.dto.GoogleLoginRequest emptyReq = com.railguard.dto.GoogleLoginRequest.builder().idToken("").build();
        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyReq)))
                .andExpect(status().is4xxClientError());

        // 2. Malformed / untrusted Google token should be rejected -> 400 or 401
        com.railguard.dto.GoogleLoginRequest fakeTokenReq = com.railguard.dto.GoogleLoginRequest.builder().idToken("invalid.google.id.token.signature").build();
        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fakeTokenReq)))
                .andExpect(status().is4xxClientError());
    }
}
