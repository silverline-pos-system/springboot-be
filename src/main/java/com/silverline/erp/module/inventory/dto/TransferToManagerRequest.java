package com.silverline.erp.module.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TransferToManagerRequest {
    @NotBlank(message = "Supervisor username is required")
    private String supervisorUsername;

    @NotBlank(message = "Supervisor password is required")
    private String supervisorPassword;

    private String notes;
    private String priority; // URGENT, HIGH, NORMAL, LOW
}

