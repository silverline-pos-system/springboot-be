package com.silverline.erp.module.pos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftStartRequest {

    @NotNull(message = "Cashier ID is required")
    @JsonProperty("cashierId")
    private Long cashierId;

    @NotNull(message = "Branch ID is required")
    @JsonProperty("branchId")
    private Long branchId;

    // NOTE: terminalId REMOVED â€” shifts are tied to branches, not terminals

    @NotNull(message = "Opening cash is required")
    @PositiveOrZero(message = "Opening cash cannot be negative")
    @JsonProperty("openingCash")
    private BigDecimal openingCash;

    @JsonProperty("supervisorUsername")
    private String supervisorUsername;

    @JsonProperty("supervisorPassword")
    private String supervisorPassword;
}

