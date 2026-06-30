package com.silverline.erp.module.pos.dto.sale;

import com.silverline.erp.domain.pos.Customer;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class SaleResponse {

    private Long saleId;
    private String invoiceNo;
    private Long customerId;
    private String customerName;
    private Customer customer; // Full customer entity for recall
    private Long cashierId;
    private String cashierName;
    private Long branchId;
    private String branchName;
    private BigDecimal grossTotal;
    private BigDecimal discount;
    private BigDecimal taxAmount;
    private BigDecimal netTotal;
    private BigDecimal paidAmount;
    private BigDecimal changeAmount;
    private String paymentStatus;
    private LocalDateTime saleDate;
    private String notes;

    // Nested data
    private List<SaleItemResponse> items;
    private List<PaymentResponse> payments;

    // Builder pattern
    public static class Builder {
        private SaleResponse response = new SaleResponse();

        public Builder saleId(Long saleId) {
            response.saleId = saleId;
            return this;
        }

        public Builder invoiceNo(String invoiceNo) {
            response.invoiceNo = invoiceNo;
            return this;
        }

        public Builder customerId(Long customerId) {
            response.customerId = customerId;
            return this;
        }

        public Builder customerName(String customerName) {
            response.customerName = customerName;
            return this;
        }

        public Builder customer(Customer customer) {
            response.customer = customer;
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

        public Builder grossTotal(BigDecimal grossTotal) {
            response.grossTotal = grossTotal;
            return this;
        }

        public Builder discount(BigDecimal discount) {
            response.discount = discount;
            return this;
        }

        public Builder taxAmount(BigDecimal taxAmount) {
            response.taxAmount = taxAmount;
            return this;
        }

        public Builder netTotal(BigDecimal netTotal) {
            response.netTotal = netTotal;
            return this;
        }

        public Builder paidAmount(BigDecimal paidAmount) {
            response.paidAmount = paidAmount;
            return this;
        }

        public Builder changeAmount(BigDecimal changeAmount) {
            response.changeAmount = changeAmount;
            return this;
        }

        public Builder paymentStatus(String paymentStatus) {
            response.paymentStatus = paymentStatus;
            return this;
        }

        public Builder saleDate(LocalDateTime saleDate) {
            response.saleDate = saleDate;
            return this;
        }

        public Builder notes(String notes) {
            response.notes = notes;
            return this;
        }

        public Builder items(List<SaleItemResponse> items) {
            response.items = items;
            return this;
        }

        public Builder payments(List<PaymentResponse> payments) {
            response.payments = payments;
            return this;
        }

        public SaleResponse build() {
            return response;
        }
    }
}
