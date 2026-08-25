package com.silverline.erp.module.inventory.repository;

import com.silverline.erp.domain.inventory.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    @Query("SELECT a FROM StockAdjustment a " +
            "WHERE (:branchId IS NULL OR a.branchId = :branchId) " +
            "AND (:productId IS NULL OR a.productId = :productId) " +
            "ORDER BY a.createdAt DESC")
    List<StockAdjustment> search(@Param("branchId") Long branchId, @Param("productId") Long productId);
}
