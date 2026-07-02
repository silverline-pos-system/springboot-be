package com.silverline.erp.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

@Slf4j
@Configuration
public class DatabaseAutoCreationConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minIdle;

    @Value("${spring.datasource.hikari.idle-timeout:300000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.connection-timeout:20000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1200000}")
    private long maxLifetime;

    @Bean
    @Primary
    public DataSource dataSource() {
        if (dbUrl != null && dbUrl.startsWith("jdbc:postgresql:")) {
            try {
                // Parse URI from "jdbc:postgresql://host:port/dbname"
                String cleanUrl = dbUrl.substring(5); // remove "jdbc:"
                URI uri = URI.create(cleanUrl);
                String dbName = uri.getPath();
                if (dbName.startsWith("/")) {
                    dbName = dbName.substring(1);
                }
                
                // In case of query params, extract only database name before "?"
                int paramIdx = dbName.indexOf('?');
                if (paramIdx != -1) {
                    dbName = dbName.substring(0, paramIdx);
                }

                String host = uri.getHost();
                int port = uri.getPort();
                if (port == -1) {
                    port = 5432;
                }

                String maintenanceDbUrl = "jdbc:postgresql://" + host + ":" + port + "/postgres";
                
                log.info("Checking database existence for: {} on host {}:{}", dbName, host, port);
                
                // Load driver class
                Class.forName(driverClassName);
                
                // Sanitize database name identifier to prevent SQL injection since identifier names cannot be parameterized in CREATE DATABASE
                if (!dbName.matches("^[a-zA-Z0-9_]+$")) {
                    throw new IllegalArgumentException("Database name contains invalid characters: " + dbName);
                }

                try (Connection conn = DriverManager.getConnection(maintenanceDbUrl, username, password)) {
                    // Check if database exists using parameterized PreparedStatement query
                    boolean dbExists = false;
                    try (java.sql.PreparedStatement pstmt = conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
                        pstmt.setString(1, dbName);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            if (rs.next()) {
                                dbExists = true;
                            }
                        }
                    }
                    
                    if (!dbExists) {
                        log.info("Database '{}' does not exist. Creating it...", dbName);
                        try (Statement stmt = conn.createStatement()) {
                            // Safe to concatenate now because dbName has been verified against the alphanumeric regex
                            stmt.executeUpdate("CREATE DATABASE " + dbName);
                            log.info("Database '{}' created successfully!", dbName);
                        }
                    } else {
                        log.info("Database '{}' already exists.", dbName);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to check or create database automatically", e);
                // Continue and let it fail during actual connection if it is a real issue
            }
        }

        // Return the actual HikariDataSource
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setIdleTimeout(idleTimeout);
        config.setConnectionTimeout(connectionTimeout);
        config.setMaxLifetime(maxLifetime);
        
        return new HikariDataSource(config);
    }
}
