package com.nsbm.rocs.modules.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PasswordResetResponseDTO {
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String status;
    private String requestNotes;
    private String adminNotes;
    private Long reviewedBy;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}

