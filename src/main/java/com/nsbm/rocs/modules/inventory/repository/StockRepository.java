package com.nsbm.rocs.modules.inventory.repository;

import com.nsbm.rocs.entity.inventory.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    List<Stock> findByBranchId(Long branchId);

    Optional<Stock> findByBranchIdAndProductId(Long branchId, Long productId);

    List<Stock> findByProductId(Long productId);

    @Query("SELECT s FROM InventoryStock s JOIN InventoryProduct p ON s.productId = p.productId " +
           "WHERE s.branchId = :branchId AND s.availableQty <= p.reorderLevel")
    List<Stock> findLowStockByBranch(@Param("branchId") Long branchId);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM InventoryStock s WHERE s.productId = :productId")
    java.math.BigDecimal getTotalStockByProduct(@Param("productId") Long productId);

    @Query("SELECT s FROM InventoryStock s WHERE s.branchId = :branchId AND s.quantity = 0")
    List<Stock> findOutOfStockByBranch(@Param("branchId") Long branchId);

    @Modifying
    @Query("UPDATE InventoryStock s SET s.quantity = s.quantity - :qty, s.availableQty = s.availableQty - :qty " +
           "WHERE s.branchId = :branchId AND s.productId = :productId AND s.availableQty >= :qty")
    int decrementQuantityIfAvailable(@Param("branchId") Long branchId,
                                     @Param("productId") Long productId,
                                     @Param("qty") int qty);

    @Modifying
    @Query("UPDATE InventoryStock s SET s.quantity = s.quantity + :qty, s.availableQty = s.availableQty + :qty " +
           "WHERE s.branchId = :branchId AND s.productId = :productId")
    void incrementQuantity(@Param("branchId") Long branchId,
                          @Param("productId") Long productId,
                          @Param("qty") int qty);

    @Query("SELECT s FROM InventoryStock s WHERE s.branchId = :branchId AND s.quantity <= :threshold")
    List<Stock> findLowStockProducts(@Param("branchId") Long branchId, @Param("threshold") Integer threshold);
}

