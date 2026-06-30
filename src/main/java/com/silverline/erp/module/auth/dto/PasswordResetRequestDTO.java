package com.silverline.erp.module.auth.dto;

import lombok.Data;

@Data
public class PasswordResetRequestDTO {
    private String username;
    private String newPassword;
    private String reason;
}

