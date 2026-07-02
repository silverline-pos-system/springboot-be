package com.silverline.erp.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetRequestDTO {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "New password is required")
    private String newPassword;

    private String reason;
}

