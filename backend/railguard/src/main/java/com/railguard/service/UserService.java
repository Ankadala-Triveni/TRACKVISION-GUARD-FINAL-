package com.railguard.service;

import com.railguard.dto.AuthResponse;
import com.railguard.dto.LoginRequest;
import com.railguard.dto.RegisterRequest;
import com.railguard.dto.UserResponse;
import com.railguard.entity.Role;
import com.railguard.entity.User;
import com.railguard.exception.DuplicateResourceException;
import com.railguard.repository.UserRepository;
import com.railguard.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.railguard.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service class implementing business logic for user registration, authentication, and profiles.
 * Business logic is kept separate from controllers.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static class ResetTokenEntry {
        final Long userId;
        final LocalDateTime expiry;
        ResetTokenEntry(Long userId, LocalDateTime expiry) {
            this.userId = userId;
            this.expiry = expiry;
        }
    }
    private final ConcurrentHashMap<String, ResetTokenEntry> resetTokens = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CaptchaService captchaService;
    private final GoogleAuthService googleAuthService;

    /**
     * Registers a new user. Default role is securely assigned to INSPECTOR.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.getCaptchaId() != null && !request.getCaptchaId().trim().isEmpty()) {
            boolean valid = captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptchaAnswer());
            if (!valid) {
                throw new IllegalArgumentException("Invalid, expired, or already-used CAPTCHA code. Please refresh and try again.");
            }
        }

        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .username(request.getUsername().trim().toLowerCase())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.INSPECTOR) // Default registration role
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        return UserResponse.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    /**
     * Performs authentication using AuthenticationManager and returns JWT access tokens.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (request.getCaptchaId() != null && !request.getCaptchaId().trim().isEmpty()) {
            boolean valid = captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptchaAnswer());
            if (!valid) {
                throw new IllegalArgumentException("Invalid, expired, or already-used CAPTCHA code. Please refresh and try again.");
            }
        }

        String identifier = request.getUsernameOrEmail().trim();
        
        // Find user first to resolve username (AuthenticationManager works with username)
        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Authenticates or creates an account using cryptographically verified Google credentials.
     */
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserInfo userInfo = googleAuthService.verifyGoogleCredential(request.getIdToken(), request.getAccessToken());
        String email = userInfo.getEmail().toLowerCase().trim();
        String name = userInfo.getName();
        if (name == null || name.trim().isEmpty()) {
            name = email.split("@")[0];
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            if (!user.isEnabled()) {
                throw new BadCredentialsException("Account is disabled. Contact system administrator.");
            }
        } else {
            // Create user account from verified Google credentials
            String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
            if (baseUsername.isEmpty()) {
                baseUsername = "user";
            }
            String candidateUsername = baseUsername;
            int counter = 1;
            while (userRepository.existsByUsername(candidateUsername)) {
                candidateUsername = baseUsername + "_" + counter;
                counter++;
            }

            user = User.builder()
                    .fullName(name.trim())
                    .username(candidateUsername)
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Secure random password
                    .role(Role.INSPECTOR)
                    .enabled(true)
                    .build();

            user = userRepository.save(user);
        }

        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Resolves currently authenticated user information.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("User not authenticated");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Initiates password reset for a registered user.
     */
    public Map<String, Object> forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);

        Map<String, Object> response = new HashMap<>();
        if (user == null) {
            response.put("success", true);
            response.put("message", "If an account exists with that email address, password reset instructions have been generated.");
            return response;
        }

        String token = UUID.randomUUID().toString();
        resetTokens.put(token, new ResetTokenEntry(user.getId(), LocalDateTime.now().plusMinutes(15)));

        response.put("success", true);
        response.put("message", "Password reset instructions have been dispatched to " + user.getEmail());
        response.put("resetToken", token); // Available for secure client-side reset workflow
        return response;
    }

    /**
     * Completes password reset using a verified reset token.
     */
    @Transactional
    public Map<String, Object> resetPassword(ResetPasswordRequest request) {
        String token = request.getToken().trim();
        ResetTokenEntry entry = resetTokens.get(token);

        if (entry == null || entry.expiry.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("The password reset token is invalid or has expired. Please request a new one.");
        }

        if (request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }

        User user = userRepository.findById(entry.userId)
                .orElseThrow(() -> new IllegalArgumentException("User account not found."));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        resetTokens.remove(token);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password has been successfully updated. You may now sign in with your new password.");
        return response;
    }
}
