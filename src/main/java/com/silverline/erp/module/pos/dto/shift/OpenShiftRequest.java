package com.silverline.erp.module.pos.dto.shift;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * PURPOSE: Carries data from frontend when opening a shift
 * WHY: Separates HTTP layer data from database entity
 */
public class OpenShiftRequest {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Opening cash is required")
    @PositiveOrZero(message = "Opening cash cannot be negative")
    private BigDecimal openingCash;

    // Default constructor (required for JSON deserialization)
    public OpenShiftRequest() {}

    // Constructor with parameters
    public OpenShiftRequest(Long branchId, BigDecimal openingCash) {
        this.branchId = branchId;
        this.openingCash = openingCash;
    }

    // Getters and Setters
    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public BigDecimal getOpeningCash() {
        return openingCash;
    }

    public void setOpeningCash(BigDecimal openingCash) {
        this.openingCash = openingCash;
    }

    @Override
    public String toString() {
        return "OpenShiftRequest{" +
                "branchId=" + branchId +
                ", openingCash=" + openingCash +
                '}';
    }
}
