package com.silverline.erp.module.pos.repository;

import com.silverline.erp.domain.pos.SalesReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SalesReturnItemRepository extends JpaRepository<SalesReturnItem, Long> {
    List<SalesReturnItem> findBySalesReturn_ReturnId(Long returnId);

    /**
     * Total quantity already returned against a given original sale item across all prior returns.
     * Used to prevent returning more than was sold (SEC-15/16, DI-08).
     */
    @Query("SELECT COALESCE(SUM(i.qty), 0) FROM SalesReturnItem i WHERE i.saleItemId = :saleItemId")
    BigDecimal sumReturnedQtyBySaleItemId(@Param("saleItemId") Long saleItemId);
}

