package com.silverline.erp.module.pos.repository;

import com.silverline.erp.domain.pos.CashFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CashFlowRepository extends JpaRepository<CashFlow, Long> {
    List<CashFlow> findByShiftId(Long shiftId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(cf.amount), 0) FROM CashFlow cf WHERE cf.shiftId = :shiftId AND cf.type = :type AND cf.status = :status")
    java.math.BigDecimal getTotalByTypeAndStatus(@org.springframework.data.repository.query.Param("shiftId") Long shiftId,
                                                 @org.springframework.data.repository.query.Param("type") String type,
                                                 @org.springframework.data.repository.query.Param("status") String status);

    long countByShiftIdAndStatus(Long shiftId, String status);
}


