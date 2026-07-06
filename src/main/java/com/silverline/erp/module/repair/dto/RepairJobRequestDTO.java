package com.silverline.erp.module.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RepairJobRequestDTO {
    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Contact number is required")
    private String contactNo;

    @NotBlank(message = "Device brand is required")
    private String deviceBrand;

    @NotBlank(message = "Device model is required")
    private String deviceModel;

    private String imeiNo;

    @NotBlank(message = "Problem description is required")
    private String problemDescription;

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    private Long createdBy;

    // New fields for advance payment workflow
    private BigDecimal advancePayment;
    private String paymentMethod;
}

