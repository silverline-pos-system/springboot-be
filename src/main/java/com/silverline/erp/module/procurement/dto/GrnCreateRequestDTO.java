package com.silverline.erp.module.procurement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrnCreateRequestDTO {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    private Long poId; // Optional - can be null for a direct receipt

    @NotNull(message = "GRN date is required")
    private LocalDate grnDate;

    private String invoiceNo;
    private LocalDate invoiceDate;

    @NotEmpty(message = "GRN items cannot be empty")
    private List<GrnItemCreateDTO> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrnItemCreateDTO {

        @NotNull(message = "Product ID is required")
        private Long productId;

        private String batchCode;
        private LocalDate expiryDate;

        @NotNull(message = "Quantity received is required")
        @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
        private BigDecimal qtyReceived;

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.00", message = "Unit price must be greater than or equal to 0")
        private BigDecimal unitPrice;

        private BigDecimal sellingPrice;
        private BigDecimal mrp;
        private String serialNo;
    }
}
