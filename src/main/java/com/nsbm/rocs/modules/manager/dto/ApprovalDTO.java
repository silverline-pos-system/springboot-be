package com.nsbm.rocs.modules.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDTO {
    private Long id;
    private String category;
    private String reference;
    private String requestedBy;
    private String username;
    private String email;
    private String time;
    private String approvedAt;
    private String status;
    private String description;
    
    // Enhanced fields for payout details
    private java.math.BigDecimal amount;
    private String reason;
    private String type; // e.g. PAID_OUT, PAID_IN
    private String approvedBy;
    private String referenceNo;
    private Long referenceId;
}

