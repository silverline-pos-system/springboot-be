package com.nsbm.rocs.repository.finance;

import com.nsbm.rocs.entity.finance.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {
    Optional<SupplierPayment> findByPaymentNo(String paymentNo);
    List<SupplierPayment> findBySupplierId(Long supplierId);
    List<SupplierPayment> findByBranchId(Long branchId);
    List<SupplierPayment> findBySupplierIdAndPaymentDateBetween(Long supplierId, LocalDate startDate, LocalDate endDate);
}
