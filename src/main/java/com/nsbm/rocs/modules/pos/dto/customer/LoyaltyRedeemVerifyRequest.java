package com.nsbm.rocs.modules.pos.dto.customer;

import lombok.Data;

@Data
public class LoyaltyRedeemVerifyRequest {
    private Long customerId;
    private Integer pointsToRedeem;
    private String otpCode;
}

