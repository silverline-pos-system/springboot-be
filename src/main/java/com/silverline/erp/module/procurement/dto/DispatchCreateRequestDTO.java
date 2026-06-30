package com.silverline.erp.module.procurement.dto;

import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.inventory.Dispatch;
import com.silverline.erp.domain.inventory.Supplier;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.domain.product.Unit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispatchCreateRequestDTO {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    private Long poId; // Optional - can be null for direct dispatch

    @NotNull(message = "Dispatch date is required")
    private LocalDate dispatchDate;

    private String invoiceNo;
    private LocalDate invoiceDate;

    @NotEmpty(message = "Dispatch items cannot be empty")
    private List<DispatchItemCreateDTO> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispatchItemCreateDTO {

        @NotNull(message = "Product ID is required")
        private Long productId;

        private String batchCode;
        private LocalDate expiryDate;

        @NotNull(message = "Quantity dispatched is required")
        @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
        private BigDecimal qtyDispatched;

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.00", message = "Unit price must be greater than or equal to 0")
        private BigDecimal unitPrice;

        private BigDecimal sellingPrice;
        private BigDecimal mrp;
        private String serialNo;
    }
}

