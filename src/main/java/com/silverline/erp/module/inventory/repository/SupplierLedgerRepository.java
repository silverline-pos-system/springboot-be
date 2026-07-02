package com.silverline.erp.module.inventory.repository;

import com.silverline.erp.domain.procurement.SupplierLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SupplierLedgerRepository extends JpaRepository<SupplierLedger, Long> {
    List<SupplierLedger> findBySupplierId(Long supplierId);
    List<SupplierLedger> findBySupplierIdAndBranchId(Long supplierId, Long branchId);
    List<SupplierLedger> findBySupplierIdAndTransactionDateBetween(Long supplierId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(sl.debit) - SUM(sl.credit), 0) FROM SupplierLedger sl WHERE sl.supplierId = :supplierId")
    BigDecimal getSupplierBalance(Long supplierId);
}


