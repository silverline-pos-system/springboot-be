package com.nsbm.rocs.modules.inventory.dto;

import lombok.Data;

@Data
public class TransferToManagerRequest {
    private String supervisorUsername;
    private String supervisorPassword;
    private String notes;
    private String priority; // URGENT, HIGH, NORMAL, LOW
}

