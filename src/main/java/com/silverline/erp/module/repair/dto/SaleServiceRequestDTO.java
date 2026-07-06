package com.silverline.erp.module.repair.dto;

import com.silverline.erp.domain.enums.ServiceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaleServiceRequestDTO {
    private Long saleId;

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    private Boolean installationRequired;

    @NotNull(message = "Service charge is required")
    @PositiveOrZero(message = "Service charge must be positive or zero")
    private BigDecimal serviceCharge;

    private Boolean paymentOk; // Whether the service charge is paid upfront

    @PositiveOrZero(message = "Advance payment cannot be negative")
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

    @NotNull(message = "Branch ID is required")
    private Long branchId;
}

