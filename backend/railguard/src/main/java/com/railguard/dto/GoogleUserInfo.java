package com.railguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Verified user identity claims returned by Google OAuth verification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleUserInfo {
    private String email;
    private boolean emailVerified;
    private String name;
    private String sub;
}

