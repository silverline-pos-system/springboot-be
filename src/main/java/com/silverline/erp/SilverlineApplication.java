package com.silverline.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SilverlineApplication {

    public static void main(String[] args) {
        loadDotEnvIfPresent();
        SpringApplication.run(SilverlineApplication.class, args);
    }

    private static void loadDotEnvIfPresent() {
        Path[] candidatePaths = new Path[] {
            Path.of(".env"),
            Path.of("springboot-be", ".env"),
            Path.of("..", "springboot-be", ".env")
        };
        for (Path path : candidatePaths) {
            if (Files.isRegularFile(path)) {
                try {
                    List<String> lines = Files.readAllLines(path);
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        int eqIdx = trimmed.indexOf('=');
                        if (eqIdx > 0) {
                            String key = trimmed.substring(0, eqIdx).trim();
                            String value = trimmed.substring(eqIdx + 1).trim();
                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                (value.startsWith("'") && value.endsWith("'"))) {
                                if (value.length() >= 2) {
                                    value = value.substring(1, value.length() - 1);
                                }
                            }
                            if (System.getenv(key) == null && System.getProperty(key) == null) {
                                System.setProperty(key, value);
                            }
                        }
                    }
                    System.out.println("[SilverlineApplication] Loaded local configuration from " + path.toAbsolutePath());
                    break;
                } catch (IOException ignored) {
                }
            }
        }
    }
}

