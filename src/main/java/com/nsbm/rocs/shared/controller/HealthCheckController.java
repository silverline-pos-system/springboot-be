package com.nsbm.rocs.shared.controller;

import com.nsbm.rocs.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check endpoint for monitoring and deployment verification.
 * Accessible without authentication.
 */
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final DataSource dataSource;

    @Value("${spring.application.name:rocs}")
    private String appName;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("application", appName);
        health.put("timestamp", LocalDateTime.now());
        health.put("database", checkDatabase());
        health.put("memory", getMemoryInfo());
        return ResponseEntity.ok(ApiResponse.success("System is healthy", health));
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.success("pong", "OK"));
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> db = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            db.put("status", "UP");
            db.put("database", conn.getMetaData().getDatabaseProductName());
            db.put("url", conn.getMetaData().getURL().replaceAll("password=[^&]*", "password=***"));
        } catch (Exception e) {
            db.put("status", "DOWN");
            db.put("error", e.getMessage());
        }
        return db;
    }

    private Map<String, Object> getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("totalMB", runtime.totalMemory() / (1024 * 1024));
        memory.put("freeMB", runtime.freeMemory() / (1024 * 1024));
        memory.put("usedMB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        memory.put("maxMB", runtime.maxMemory() / (1024 * 1024));
        return memory;
    }
}
