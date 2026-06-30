package com.silverline.erp.module.inventory.repository;

import com.silverline.erp.domain.inventory.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    List<Batch> findByProductId(Long productId);

    List<Batch> findByBranchId(Long branchId);

    List<Batch> findByProductIdAndBranchId(Long productId, Long branchId);

    Optional<Batch> findByBatchCode(String batchCode);

    List<Batch> findByExpiryDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT b FROM InventoryBatch b WHERE b.branchId = :branchId AND b.expiryDate <= :date")
    List<Batch> findExpiringBatches(@Param("branchId") Long branchId, @Param("date") LocalDate date);

    @Query("SELECT b FROM InventoryBatch b WHERE b.branchId = :branchId AND b.qty > 0 ORDER BY b.expiryDate ASC")
    List<Batch> findAvailableBatchesByBranch(@Param("branchId") Long branchId);

    @Query("SELECT b FROM InventoryBatch b WHERE b.productId = :productId AND b.branchId = :branchId AND b.qty > 0 ORDER BY b.expiryDate ASC")
    List<Batch> findAvailableBatchesByProduct(@Param("productId") Long productId, @Param("branchId") Long branchId);

    // Method for finding batches by branch, product, and batch code with ordering
    @Query("SELECT b FROM InventoryBatch b WHERE b.branchId = :branchId AND b.productId = :productId AND b.batchCode = :batchCode ORDER BY b.expiryDate ASC, b.manufacturingDate ASC, b.batchId ASC")
    List<Batch> findAllByBranchIdAndProductIdAndBatchCodeOrdered(@Param("branchId") Long branchId, @Param("productId") Long productId, @Param("batchCode") String batchCode);

    // Method for finding expired batches
    @Query("SELECT b FROM InventoryBatch b WHERE b.expiryDate < :date AND b.qty > 0")
    List<Batch> findExpiredBatches(@Param("date") LocalDate date);

    // Method for finding batches expiring soon
    @Query("SELECT b FROM InventoryBatch b WHERE b.expiryDate BETWEEN :startDate AND :endDate AND b.qty > 0 ORDER BY b.expiryDate ASC")
    List<Batch> findExpiringSoonBatches(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT b FROM InventoryBatch b WHERE (:branchId IS NULL OR b.branchId = :branchId) AND b.expiryDate BETWEEN :startDate AND :endDate AND b.qty > 0 ORDER BY b.expiryDate ASC")
    List<Batch> findExpiringSoonBatchesByBranch(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("branchId") Long branchId);

    // Method for finding available batches by branch and product using FEFO (First Expired First Out)
    @Query("SELECT b FROM InventoryBatch b WHERE b.branchId = :branchId AND b.productId = :productId AND b.qty > 0 AND (b.expiryDate IS NULL OR b.expiryDate >= :currentDate) ORDER BY b.expiryDate ASC NULLS LAST, b.manufacturingDate ASC NULLS LAST, b.batchId ASC")
    List<Batch> findAvailableByBranchAndProductFefo(@Param("branchId") Long branchId, @Param("productId") Long productId, @Param("currentDate") LocalDate currentDate);

    // Method for finding expired batches by branch and product
    @Query("SELECT b FROM InventoryBatch b WHERE b.branchId = :branchId AND b.productId = :productId AND b.expiryDate < :date AND b.qty > 0")
    List<Batch> findExpiredByBranchAndProduct(@Param("branchId") Long branchId, @Param("productId") Long productId, @Param("date") LocalDate date);

    // Method for finding batches expiring soon by branch and product
    @Query("SELECT b FROM InventoryBatch b WHERE b.branchId = :branchId AND b.productId = :productId AND b.expiryDate BETWEEN :startDate AND :endDate AND b.qty > 0 ORDER BY b.expiryDate ASC")
    List<Batch> findExpiringSoonByBranchAndProduct(@Param("branchId") Long branchId, @Param("productId") Long productId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}


