package com.silverline.erp.module.inventory.dto;

import com.silverline.erp.domain.inventory.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductDetailsDTO {
    private Product product;
    private String categoryName;
    private String subCategoryName;
    private String brandName;
    private String unitName;
    private BigDecimal totalStock;
    private List<BatchSummaryDTO> batches;
    private List<SupplierSummaryDTO> suppliers;

    @Data
    @Builder
    public static class BatchSummaryDTO {
        private Long batchId;
        private String batchCode;
        private BigDecimal qty;
        private String expiryDate;
        private BigDecimal costPrice;
        private BigDecimal sellingPrice;
        private String branchName;
    }

    @Data
    @Builder
    public static class SupplierSummaryDTO {
        private Long supplierId;
        private String name;
        private String contactPerson;
        private String phone;
        private String email;
    }
}


