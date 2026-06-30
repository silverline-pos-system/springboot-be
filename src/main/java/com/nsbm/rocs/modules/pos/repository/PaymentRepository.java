package com.nsbm.rocs.modules.pos.repository;

import com.nsbm.rocs.entity.pos.Payment;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PURPOSE: Interface for payments operations
 */
@Repository
public interface PaymentRepository extends JpaRepository<@NonNull Payment, @NonNull Long>, PaymentRepositoryCustom {

    /**
     * Get all payments for a sale
     * @param saleId - Sale ID
     * @return List of payments
     */
    List<Payment> findBySaleId(Long saleId);

    /**
     * Delete payments by sale ID
     * @param saleId - Sale ID
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteBySaleId(Long saleId);

    /**
     * Get payments by shift ID (via Sale)
     * @param shiftId - Shift ID
     * @return List of payments
     */
    @Query("SELECT p FROM Payment p JOIN Sale s ON p.saleId = s.saleId WHERE s.shiftId = :shiftId")
    List<Payment> findByShiftId(Long shiftId);

    @Query("SELECT SUM(p.amount) FROM Payment p JOIN Sale s ON p.saleId = s.saleId WHERE s.shiftId = :shiftId AND p.paymentType = :paymentType")
    java.math.BigDecimal sumTotalByShiftIdAndType(Long shiftId, String paymentType);

    @Query("SELECT SUM(p.amount) FROM Payment p JOIN Sale s ON p.saleId = s.saleId WHERE s.shiftId = :shiftId AND p.paymentType NOT IN ('CASH', 'CARD')")
    java.math.BigDecimal sumOtherPaymentsByShiftId(Long shiftId);
    
    /**
     * Get payment breakdown by type for a date range
     */
    @Query(value = """
        SELECT p.payment_type, 
               COALESCE(SUM(p.amount), 0) as total_amount,
               COUNT(p.payment_id) as payment_count
        FROM payments p
        JOIN sales s ON p.sale_id = s.sale_id
        WHERE s.sale_date BETWEEN :startDate AND :endDate
        AND (:branchId IS NULL OR s.branch_id = :branchId)
        GROUP BY p.payment_type
        ORDER BY total_amount DESC
        """, nativeQuery = true)
    List<Object[]> findPaymentBreakdownByDateRange(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate,
                                                   @Param("branchId") Long branchId);
    
    /**
     * Sum payments by type for date range
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p JOIN Sale s ON p.saleId = s.saleId WHERE s.saleDate BETWEEN :startDate AND :endDate AND p.paymentType = :paymentType AND (:branchId IS NULL OR s.branchId = :branchId)")
    BigDecimal sumByTypeAndDateRange(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate,
                                     @Param("paymentType") String paymentType,
                                     @Param("branchId") Long branchId);
}

