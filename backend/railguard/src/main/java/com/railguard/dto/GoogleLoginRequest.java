package com.railguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO carrying Google credential (ID Token from GIS or Access Token from OAuth2 TokenClient).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleLoginRequest {

    private String idToken;
    private String accessToken;

    public GoogleLoginRequest(String idToken) {
        this.idToken = idToken;
    }
}
