package com.silverline.erp.module.pos.repository;

import com.silverline.erp.domain.pos.SalesReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesReturnRepository extends JpaRepository<SalesReturn, Long> {
    Optional<SalesReturn> findByReturnNo(String returnNo);
    List<SalesReturn> findByBranchId(Long branchId);
    List<SalesReturn> findBySaleId(Long saleId);
    List<SalesReturn> findByStatus(String status);

    @Query("SELECT COALESCE(SUM(sr.totalAmount), 0) FROM SalesReturn sr WHERE sr.shiftId = :shiftId AND sr.status = 'APPROVED'")
    BigDecimal sumTotalAmountByShiftId(@Param("shiftId") Long shiftId);
    
    @Query("SELECT COALESCE(SUM(sr.totalAmount), 0) FROM SalesReturn sr WHERE sr.returnDate BETWEEN :startDate AND :endDate AND sr.status = 'APPROVED' AND (:branchId IS NULL OR sr.branchId = :branchId)")
    BigDecimal sumTotalAmountByDateRange(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         @Param("branchId") Long branchId);
    
    @Query("SELECT COUNT(sr) FROM SalesReturn sr WHERE sr.returnDate BETWEEN :startDate AND :endDate AND sr.status = 'APPROVED'")
    Long countByDateRange(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);
}

