package com.silverline.erp.module.repair.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RepairJobRequestDTO {
    private String customerName;
    private String contactNo;
    private String deviceBrand;
    private String deviceModel;
    private String imeiNo;
    private String problemDescription;
    private Long branchId;
    private Long createdBy;
    
    // New fields for advance payment workflow
    private BigDecimal advancePayment;
    private String paymentMethod; 
}

