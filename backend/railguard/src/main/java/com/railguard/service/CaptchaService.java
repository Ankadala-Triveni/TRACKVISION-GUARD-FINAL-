package com.railguard.service;

import com.railguard.dto.CaptchaDto;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing server-side distortion-resistant visual image CAPTCHAs.
 * Enforces single-use, 5-minute expiration, and attempt limits.
 */
@Service
public class CaptchaService {

    private static final String CHAR_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 5;
    private static final int WIDTH = 160;
    private static final int HEIGHT = 48;
    private static final long EXPIRATION_SECONDS = 300; // 5 minutes
    private static final int MAX_ATTEMPTS = 3;

    private static final Color[] TEXT_COLORS = new Color[]{
            new Color(0, 212, 170),   // Teal
            new Color(56, 189, 248),  // Sky Blue
            new Color(245, 158, 11),  // Amber
            new Color(167, 139, 250), // Purple
            new Color(52, 211, 153)   // Emerald
    };

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();

    private static class Challenge {
        final String text;
        final Instant expiresAt;
        int remainingAttempts;

        Challenge(String text, Instant expiresAt, int remainingAttempts) {
            this.text = text;
            this.expiresAt = expiresAt;
            this.remainingAttempts = remainingAttempts;
        }
    }

    public CaptchaDto generateCaptcha() {
        cleanExpiredChallenges();

        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHAR_POOL.charAt(random.nextInt(CHAR_POOL.length())));
        }
        String challengeCode = code.toString();

        String captchaId = UUID.randomUUID().toString();
        challenges.put(captchaId, new Challenge(challengeCode, Instant.now().plusSeconds(EXPIRATION_SECONDS), MAX_ATTEMPTS));

        String base64Image = renderCaptchaImage(challengeCode);
        return new CaptchaDto(captchaId, base64Image);
    }

    public boolean validateCaptcha(String captchaId, String userAnswer) {
        if (captchaId == null || captchaId.trim().isEmpty() || userAnswer == null) {
            return false;
        }

        Challenge challenge = challenges.get(captchaId.trim());
        if (challenge == null) {
            return false; // Challenge not found or already consumed
        }

        if (Instant.now().isAfter(challenge.expiresAt)) {
            challenges.remove(captchaId.trim());
            return false; // Expired
        }

        challenge.remainingAttempts--;

        boolean matches = challenge.text.equalsIgnoreCase(userAnswer.trim());

        if (matches) {
            // Strictly single-use: consume immediately on success
            challenges.remove(captchaId.trim());
            return true;
        } else {
            if (challenge.remainingAttempts <= 0) {
                challenges.remove(captchaId.trim());
            }
            return false;
        }
    }

    private String renderCaptchaImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Enable high-quality anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background: Dark Slate (#0d1117)
        g2d.setColor(new Color(13, 17, 23));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw background noise lines
        for (int i = 0; i < 5; i++) {
            g2d.setColor(new Color(40 + random.nextInt(40), 50 + random.nextInt(40), 60 + random.nextInt(40), 120));
            g2d.setStroke(new BasicStroke(1.2f));
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g2d.drawLine(x1, y1, x2, y2);
        }

        // Draw noise dots
        for (int i = 0; i < 40; i++) {
            g2d.setColor(new Color(70 + random.nextInt(50), 80 + random.nextInt(50), 90 + random.nextInt(50), 150));
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            image.setRGB(x, y, g2d.getColor().getRGB());
        }

        // Render characters with slight rotation & offset
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 26);
        g2d.setFont(font);

        int charWidth = (WIDTH - 20) / CODE_LENGTH;
        for (int i = 0; i < CODE_LENGTH; i++) {
            char ch = code.charAt(i);
            Color charColor = TEXT_COLORS[random.nextInt(TEXT_COLORS.length)];
            g2d.setColor(charColor);

            double angle = (random.nextDouble() - 0.5) * 0.45; // -13 to +13 degrees
            int x = 12 + i * charWidth + random.nextInt(5);
            int y = 32 + random.nextInt(7);

            AffineTransform orig = g2d.getTransform();
            g2d.rotate(angle, x, y);
            g2d.drawString(String.valueOf(ch), x, y);
            g2d.setTransform(orig);
        }

        // Add subtle foreground distortion curve
        g2d.setColor(new Color(0, 212, 170, 70));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawArc(10, random.nextInt(15), WIDTH - 20, 25, 0, 180);

        g2d.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode CAPTCHA image", e);
        }
    }

    private void cleanExpiredChallenges() {
        Instant now = Instant.now();
        Iterator<Map.Entry<String, Challenge>> it = challenges.entrySet().iterator();
        while (it.hasNext()) {
            if (now.isAfter(it.next().getValue().expiresAt)) {
                it.remove();
            }
        }
    }
}

