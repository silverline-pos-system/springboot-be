package com.nsbm.rocs.modules.pos.dto.customer;

import lombok.Data;

@Data
public class LoyaltyRedeemRequest {
    private Long customerId;
    private Integer pointsToRedeem;
}

