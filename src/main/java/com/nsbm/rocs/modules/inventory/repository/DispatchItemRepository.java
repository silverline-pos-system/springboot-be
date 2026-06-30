package com.nsbm.rocs.modules.inventory.repository;

import com.nsbm.rocs.entity.inventory.Dispatch;
import com.nsbm.rocs.entity.inventory.DispatchItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DispatchItemRepository extends JpaRepository<DispatchItem, Long> {
    List<DispatchItem> findByDispatchId(Long dispatchId);
    List<DispatchItem> findByProductId(Long productId);
    List<DispatchItem> findByBatchCode(String batchCode);
    List<DispatchItem> findByExpiryDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(gi.qtyDispatched) FROM DispatchItem gi WHERE gi.productId = :productId")
    BigDecimal getTotalDispatchedQuantityByProduct(@Param("productId") Long productId);

    @Query("SELECT gi FROM DispatchItem gi JOIN Dispatch g ON gi.dispatchId = g.dispatchId WHERE g.branchId = :branchId AND gi.productId = :productId")
    List<DispatchItem> findByBranchIdAndProductId(@Param("branchId") Long branchId, @Param("productId") Long productId);
}


