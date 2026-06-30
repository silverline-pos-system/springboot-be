package com.silverline.erp.module.repair.dto;

import com.silverline.erp.domain.enums.ServiceType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaleServiceRequestDTO {
    private Long saleId;
    private ServiceType serviceType;
    private Boolean installationRequired;
    private BigDecimal serviceCharge;
    private Boolean paymentOk; // Whether the service charge is paid upfront
    private BigDecimal advancePayment; // Upfront payment amount if not fully paid
    private String notes;
    private Long technicianId;
    private Long createdBy;
    
    // For standalone creation without a completed sale yet:
    private Long customerId;
    private String customerName;
    private String contactNo;
    private String altContactNo;
    private String address;
    private Long branchId;
}

