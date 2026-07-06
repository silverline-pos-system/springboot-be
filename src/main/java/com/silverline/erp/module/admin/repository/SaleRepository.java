package com.silverline.erp.module.admin.repository;

import com.silverline.erp.domain.pos.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Sale entity operations
 * Provides methods for querying sales data for the manager dashboard
 */
@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    /**
     * Get total sales for a branch on a specific date
     */
    @Query("SELECT COALESCE(SUM(s.netTotal), 0) FROM Sale s WHERE s.branchId = :branchId AND s.saleDate >= :startDate AND s.saleDate < :endDate")
    BigDecimal getTotalSalesByBranchAndDate(@Param("branchId") Long branchId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Get count of sales for a branch on a specific date
     */
    @Query("SELECT COUNT(s) FROM Sale s WHERE s.branchId = :branchId AND s.saleDate >= :startDate AND s.saleDate < :endDate")
    Long getCountByBranchAndDate(@Param("branchId") Long branchId,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Get daily sales totals for the last N days
     */
    @Query(value = "SELECT COALESCE(SUM(net_total), 0) FROM sales WHERE branch_id = :branchId AND CAST(sale_date AS date) = :date", nativeQuery = true)
    BigDecimal getDailySales(@Param("branchId") Long branchId, @Param("date") LocalDate date);

    /**
     * Find all sales for a branch within a date range (existing convenience method)
     */
    List<Sale> findByBranchIdAndSaleDateBetweenOrderBySaleDateDesc(Long branchId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find by date range and optional branch. When branchId is null, return across all branches.
     */
    @Query("SELECT s FROM Sale s WHERE (:branchId IS NULL OR s.branchId = :branchId) AND s.saleDate >= :start AND s.saleDate <= :end ORDER BY s.saleDate DESC")
    List<Sale> findByDateRange(@Param("branchId") Long branchId,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);
}

