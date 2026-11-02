package com.railguard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * Temporary protected endpoints for verifying Role-Based Access Control configuration.
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/authenticated")
    public ResponseEntity<Map<String, String>> getAuthenticated() {
        return ResponseEntity.ok(Map.of("message", "Access granted to authenticated user"));
    }

    @GetMapping("/admin")
    public ResponseEntity<Map<String, String>> getAdmin() {
        return ResponseEntity.ok(Map.of("message", "Access granted to ADMIN role"));
    }

    @GetMapping("/inspector")
    public ResponseEntity<Map<String, String>> getInspector() {
        return ResponseEntity.ok(Map.of("message", "Access granted to INSPECTOR role"));
    }

    @GetMapping("/engineer")
    public ResponseEntity<Map<String, String>> getEngineer() {
        return ResponseEntity.ok(Map.of("message", "Access granted to ENGINEER role"));
    }

    @GetMapping("/maintenance")
    public ResponseEntity<Map<String, String>> getMaintenance() {
        return ResponseEntity.ok(Map.of("message", "Access granted to MAINTENANCE role"));
    }
}
