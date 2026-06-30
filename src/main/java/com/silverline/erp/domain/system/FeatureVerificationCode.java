package com.silverline.erp.domain.system;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feature_verification_codes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FeatureVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feature_code", nullable = false, length = 50)
    private String featureCode;

    @Column(name = "action", nullable = false, length = 20)
    private String action; // ACTIVATE or DEACTIVATE

    @Column(name = "verification_code", nullable = false)
    private Integer verificationCode;

    @Column(name = "hashed_code", nullable = false)
    private Long hashedCode;

    @Column(name = "is_used")
    private Boolean isUsed = false;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
