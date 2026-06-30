package com.silverline.erp.module.inventory.repository;

import com.silverline.erp.domain.inventory.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryStockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByBranchIdAndProductId(Long branchId, Long productId);

    List<Stock> findByBranchId(Long branchId);

    List<Stock> findByProductId(Long productId);

    @Query("SELECT s FROM InventoryStock s WHERE s.branchId = :branchId AND s.quantity <= :threshold")
    List<Stock> findLowStockItems(@Param("branchId") Long branchId, @Param("threshold") BigDecimal threshold);

    @Query("SELECT s FROM InventoryStock s WHERE s.branchId = :branchId AND s.availableQty <= :threshold")
    List<Stock> findLowAvailableStockItems(@Param("branchId") Long branchId, @Param("threshold") BigDecimal threshold);

    @Query("SELECT SUM(s.quantity) FROM InventoryStock s WHERE s.productId = :productId")
    BigDecimal getTotalStockByProduct(@Param("productId") Long productId);

    @Query("SELECT COUNT(s) FROM InventoryStock s WHERE s.branchId = :branchId AND s.quantity > 0")
    Long countStockedItemsByBranch(@Param("branchId") Long branchId);
}

