package com.silverline.erp.module.pos.repository;

import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.product.Product;
/**
 * PURPOSE: Handle stock updates during sales
 */
public interface StockRepository {

    /**
     * Reduce stock quantity when product is sold
     * @param branchId - Branch ID
     * @param productId - Product ID
     * @param quantity - Quantity to reduce
     */
    void reduceStock(Long branchId, Long productId, Integer quantity);

    /**
     * Check if sufficient stock is available
     * @param branchId - Branch ID
     * @param productId - Product ID
     * @param quantity - Quantity needed
     * @return true if available, false otherwise
     */
    boolean isStockAvailable(Long branchId, Long productId, Integer quantity);

    /**
     * Get current stock quantity
     * @param branchId - Branch ID
     * @param productId - Product ID
     * @return Current quantity
     */
    Integer getCurrentStock(Long branchId, Long productId);
}
