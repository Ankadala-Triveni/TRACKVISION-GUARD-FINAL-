package com.railguard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railguard.dto.GoogleLoginRequest;
import com.railguard.dto.GoogleUserInfo;
import com.railguard.entity.Role;
import com.railguard.entity.User;
import com.railguard.repository.UserRepository;
import com.railguard.security.JwtService;
import com.railguard.service.GoogleAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test suite for Google OAuth 2.0 / OpenID Connect authentication:
 * Validates configuration endpoint, token verification, new user provisioning,
 * existing user login, unverified email rejection, and disabled user rejection.
 */
@SpringBootTest(properties = {
    "JWT_SECRET=dGhpcy1pcy1hLXNlY3VyZS1hbmQtc3VmZmljaWVudGx5LWxvbmctc2VjcmV0LWtleS1mb3ItZGV2LTIwMjYtcmFpbGd1YXJk",
    "GOOGLE_CLIENT_ID=test-client-id.apps.googleusercontent.com"
})
@AutoConfigureMockMvc
public class GoogleAuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GoogleAuthService googleAuthService;

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
        Mockito.reset(googleAuthService);
        when(googleAuthService.getGoogleClientId()).thenReturn("test-client-id.apps.googleusercontent.com");
    }

    @Test
    void testAuthConfigEndpoint_returnsConfigStatus() throws Exception {
        mockMvc.perform(get("/api/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleClientId").value("test-client-id.apps.googleusercontent.com"))
                .andExpect(jsonPath("$.googleAuthEnabled").value(true));
    }

    @Test
    void testGoogleLogin_emptyToken_returnsBadRequest() throws Exception {
        GoogleLoginRequest emptyReq = GoogleLoginRequest.builder().idToken("").build();
        when(googleAuthService.verifyGoogleCredential("", null))
                .thenThrow(new BadCredentialsException("Google authentication failed: ID token or Access token is required."));

        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyReq)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testGoogleLogin_invalidToken_returnsUnauthorized() throws Exception {
        GoogleLoginRequest invalidReq = GoogleLoginRequest.builder().idToken("invalid.token.signature").build();
        when(googleAuthService.verifyGoogleCredential("invalid.token.signature", null))
                .thenThrow(new BadCredentialsException("Invalid, expired, or untrusted Google ID token."));

        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid, expired, or untrusted Google ID token."));
    }

    @Test
    void testGoogleLogin_unverifiedEmail_rejected() throws Exception {
        GoogleLoginRequest unverifiedReq = GoogleLoginRequest.builder().idToken("valid.token.unverified").build();
        when(googleAuthService.verifyGoogleCredential("valid.token.unverified", null))
                .thenThrow(new BadCredentialsException("Authentication rejected: Google account email is not verified."));

        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(unverifiedReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication rejected: Google account email is not verified."));
    }

    @Test
    void testGoogleLogin_newUser_successfullyProvisionedAndLoggedIn() throws Exception {
        GoogleUserInfo verifiedUser = GoogleUserInfo.builder()
                .email("new.inspector@railguard.test")
                .emailVerified(true)
                .name("New Google Inspector")
                .sub("google-sub-123456")
                .build();

        when(googleAuthService.verifyGoogleCredential("valid.google.id.token", null)).thenReturn(verifiedUser);

        GoogleLoginRequest request = GoogleLoginRequest.builder().idToken("valid.google.id.token").build();

        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("new.inspector@railguard.test"))
                .andExpect(jsonPath("$.fullName").value("New Google Inspector"))
                .andExpect(jsonPath("$.role").value("INSPECTOR"));

        // Verify user persisted in database
        User createdUser = userRepository.findByEmail("new.inspector@railguard.test").orElse(null);
        assertNotNull(createdUser, "User should be created in database");
        assertEquals("New Google Inspector", createdUser.getFullName());
        assertEquals(Role.INSPECTOR, createdUser.getRole());
        assertTrue(createdUser.isEnabled());
        assertNotNull(createdUser.getPassword(), "Password hash must be generated");
    }

    @Test
    void testGoogleLogin_existingUser_successfullyAuthenticatedWithoutDuplicate() throws Exception {
        // Pre-create user with same email
        User existingUser = userRepository.save(User.builder()
                .fullName("Existing Railway Engineer")
                .username("rail_eng_1")
                .email("engineer@railguard.test")
                .password(passwordEncoder.encode("ExistingPass@123"))
                .role(Role.ENGINEER)
                .enabled(true)
                .build());

        long initialUserCount = userRepository.count();

        GoogleUserInfo verifiedUser = GoogleUserInfo.builder()
                .email("engineer@railguard.test")
                .emailVerified(true)
                .name("Existing Railway Engineer")
                .sub("google-sub-789012")
                .build();

        when(googleAuthService.verifyGoogleCredential("valid.existing.token", null)).thenReturn(verifiedUser);

        GoogleLoginRequest request = GoogleLoginRequest.builder().idToken("valid.existing.token").build();

        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("engineer@railguard.test"))
                .andExpect(jsonPath("$.role").value("ENGINEER"))
                .andExpect(jsonPath("$.userId").value(existingUser.getId()));

        assertEquals(initialUserCount, userRepository.count(), "User count should not increase for existing user");
    }

    @Test
    void testGoogleLogin_disabledUser_rejected() throws Exception {
        userRepository.save(User.builder()
                .fullName("Suspended Inspector")
                .username("suspended_user")
                .email("suspended@railguard.test")
                .password(passwordEncoder.encode("Password@123"))
                .role(Role.INSPECTOR)
                .enabled(false) // Disabled account
                .build());

        GoogleUserInfo verifiedUser = GoogleUserInfo.builder()
                .email("suspended@railguard.test")
                .emailVerified(true)
                .name("Suspended Inspector")
                .sub("google-sub-disabled")
                .build();

        when(googleAuthService.verifyGoogleCredential("token.disabled", null)).thenReturn(verifiedUser);

        GoogleLoginRequest request = GoogleLoginRequest.builder().idToken("token.disabled").build();

        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is disabled. Contact system administrator."));
    }
}

