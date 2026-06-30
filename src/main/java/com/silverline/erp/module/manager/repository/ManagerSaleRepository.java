package com.silverline.erp.module.manager.repository;

import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.pos.Sale;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ManagerSaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByBranchId(Long branchId);

    @Query("SELECT s FROM Sale s WHERE s.branchId = :branchId AND s.saleDate BETWEEN :startDate AND :endDate")
    List<Sale> findByBranchIdAndDateRange(@Param("branchId") Long branchId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT s FROM Sale s WHERE s.saleDate BETWEEN :startDate AND :endDate ORDER BY s.saleDate DESC")
    List<Sale> findByDateRange(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(s.netTotal), 0) FROM Sale s WHERE s.branchId = :branchId AND s.saleDate BETWEEN :startDate AND :endDate")
    BigDecimal sumNetTotalByBranchAndDateRange(@Param("branchId") Long branchId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(s.netTotal), 0) FROM Sale s WHERE s.saleDate BETWEEN :startDate AND :endDate")
    BigDecimal sumNetTotalByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.branchId = :branchId AND s.saleDate BETWEEN :startDate AND :endDate")
    Long countByBranchAndDateRange(@Param("branchId") Long branchId,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.saleDate BETWEEN :startDate AND :endDate")
    Long countByDateRange(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    List<Sale> findTop10ByCustomerIdOrderBySaleDateDesc(Long customerId);
    
    // Recent sales with limit
    @Query("SELECT s FROM Sale s ORDER BY s.saleDate DESC")
    List<Sale> findRecentSales(Pageable pageable);

    @Query("SELECT s FROM Sale s WHERE s.branchId = :branchId ORDER BY s.saleDate DESC")
    List<Sale> findRecentSalesByBranch(@Param("branchId") Long branchId, Pageable pageable);
    
    // Hourly sales data
    @Query(value = """
        SELECT EXTRACT(HOUR FROM s.sale_date) as hour_of_day, 
               COALESCE(SUM(s.net_total), 0) as total_sales,
               COUNT(s.sale_id) as transaction_count
        FROM sales s 
        WHERE CAST(s.sale_date AS date) = CAST(:targetDate AS date)
        AND (:branchId IS NULL OR s.branch_id = :branchId)
        GROUP BY EXTRACT(HOUR FROM s.sale_date)
        ORDER BY hour_of_day
        """, nativeQuery = true)
    List<Object[]> findHourlySales(@Param("targetDate") LocalDateTime targetDate, @Param("branchId") Long branchId);
    
    // Count distinct customers served
    @Query("SELECT COUNT(DISTINCT s.customerId) FROM Sale s WHERE s.saleDate BETWEEN :startDate AND :endDate AND s.customerId IS NOT NULL AND (:branchId IS NULL OR s.branchId = :branchId)")
    Long countDistinctCustomers(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate,
                                @Param("branchId") Long branchId);
    
    // Get gross total sum (before discounts)
    @Query("SELECT COALESCE(SUM(s.grossTotal), 0) FROM Sale s WHERE s.saleDate BETWEEN :startDate AND :endDate AND (:branchId IS NULL OR s.branchId = :branchId)")
    BigDecimal sumGrossTotalByDateRange(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        @Param("branchId") Long branchId);
    
    // Get discounts total
    @Query("SELECT COALESCE(SUM(s.discount), 0) FROM Sale s WHERE s.saleDate BETWEEN :startDate AND :endDate AND (:branchId IS NULL OR s.branchId = :branchId)")
    BigDecimal sumDiscountsByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       @Param("branchId") Long branchId);

    // Branch performance analysis
    @Query(value = """
        SELECT b.branch_id, b.name, b.code,
               COALESCE(SUM(s.net_total), 0) as revenue,
               COUNT(s.sale_id) as transactions
        FROM branches b
        LEFT JOIN sales s ON b.branch_id = s.branch_id 
             AND s.sale_date BETWEEN :startDate AND :endDate
        WHERE b.is_active = true
        GROUP BY b.branch_id, b.name, b.code
        ORDER BY revenue DESC
        """, nativeQuery = true)
    List<Object[]> findRevenueByBranch(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);
}

