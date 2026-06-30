package com.nsbm.rocs.modules.inventory.repository;

import com.nsbm.rocs.entity.inventory.PurchaseOrderPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderPaymentRepository extends JpaRepository<PurchaseOrderPayment, Long> {
    List<PurchaseOrderPayment> findByPoIdOrderByPaidAtDesc(Long poId);
}


