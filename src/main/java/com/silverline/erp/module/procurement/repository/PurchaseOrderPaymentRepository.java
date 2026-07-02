package com.silverline.erp.module.procurement.repository;

import com.silverline.erp.domain.procurement.PurchaseOrderPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderPaymentRepository extends JpaRepository<PurchaseOrderPayment, Long> {
    List<PurchaseOrderPayment> findByPoIdOrderByPaidAtDesc(Long poId);
}


