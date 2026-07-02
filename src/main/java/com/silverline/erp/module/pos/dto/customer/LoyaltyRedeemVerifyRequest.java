package com.silverline.erp.module.pos.dto.customer;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoyaltyRedeemVerifyRequest {
    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Points to redeem is required")
    @Min(value = 1, message = "Points to redeem must be at least 1")
    private Integer pointsToRedeem;

    @NotBlank(message = "OTP code is required")
    private String otpCode;
}

