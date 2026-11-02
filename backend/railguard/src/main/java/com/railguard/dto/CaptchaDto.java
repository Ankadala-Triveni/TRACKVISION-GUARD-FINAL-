package com.railguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO carrying server-generated image CAPTCHA challenge.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaptchaDto {
    private String captchaId;
    private String image; // Base64 data URI (data:image/png;base64,...)
}

