package com.silverline.erp.module.pos.dto.shift;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PURPOSE: Carries shift data back to frontend
 * WHY: Hides database details, only sends what frontend needs
 */
public class ShiftResponse {

    private Long shiftId;
    private Long cashierId;
    private String cashierName; // Joined from users table
    private Long branchId;
    private String branchName; // Joined from branches table
    private BigDecimal openingCash;
    private BigDecimal closingCash;
    private BigDecimal expectedCash;
    private BigDecimal cashDifference;
    private BigDecimal totalSales;
    private BigDecimal totalReturns;
    private Integer transactionCount;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private String status;
    private String notes;

    // Builder pattern for easy object creation
    public static class Builder {
        private ShiftResponse response = new ShiftResponse();

        public Builder shiftId(Long shiftId) {
            response.shiftId = shiftId;
            return this;
        }

        public Builder cashierId(Long cashierId) {
            response.cashierId = cashierId;
            return this;
        }

        public Builder cashierName(String cashierName) {
            response.cashierName = cashierName;
            return this;
        }

        public Builder branchId(Long branchId) {
            response.branchId = branchId;
            return this;
        }

        public Builder branchName(String branchName) {
            response.branchName = branchName;
            return this;
        }

        public Builder openingCash(BigDecimal openingCash) {
            response.openingCash = openingCash;
            return this;
        }

        public Builder closingCash(BigDecimal closingCash) {
            response.closingCash = closingCash;
            return this;
        }

        public Builder expectedCash(BigDecimal expectedCash) {
            response.expectedCash = expectedCash;
            return this;
        }

        public Builder cashDifference(BigDecimal cashDifference) {
            response.cashDifference = cashDifference;
            return this;
        }

        public Builder totalSales(BigDecimal totalSales) {
            response.totalSales = totalSales;
            return this;
        }

        public Builder totalReturns(BigDecimal totalReturns) {
            response.totalReturns = totalReturns;
            return this;
        }

        public Builder transactionCount(Integer transactionCount) {
            response.transactionCount = transactionCount;
            return this;
        }

        public Builder openedAt(LocalDateTime openedAt) {
            response.openedAt = openedAt;
            return this;
        }

        public Builder closedAt(LocalDateTime closedAt) {
            response.closedAt = closedAt;
            return this;
        }

        public Builder status(String status) {
            response.status = status;
            return this;
        }

        public Builder notes(String notes) {
            response.notes = notes;
            return this;
        }

        public ShiftResponse build() {
            return response;
        }
    }

    // All getters (no setters - response is immutable)
    public Long getShiftId() { return shiftId; }
    public Long getCashierId() { return cashierId; }
    public String getCashierName() { return cashierName; }
    public Long getBranchId() { return branchId; }
    public String getBranchName() { return branchName; }
    public BigDecimal getOpeningCash() { return openingCash; }
    public BigDecimal getClosingCash() { return closingCash; }
    public BigDecimal getExpectedCash() { return expectedCash; }
    public BigDecimal getCashDifference() { return cashDifference; }
    public BigDecimal getTotalSales() { return totalSales; }
    public BigDecimal getTotalReturns() { return totalReturns; }
    public Integer getTransactionCount() { return transactionCount; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
}
