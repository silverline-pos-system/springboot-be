package com.silverline.erp.module.procurement.repository;

import com.silverline.erp.domain.procurement.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByPoNo(String poNo);

    List<PurchaseOrder> findByBranchId(Long branchId);

    List<PurchaseOrder> findByStatus(String status);

    List<PurchaseOrder> findBySupplierId(Long supplierId);

    @org.springframework.data.jpa.repository.Query("SELECT p.poNo FROM PurchaseOrder p WHERE p.poNo LIKE 'PO-%'")
    List<String> findAllPoNumbersWithPrefix();
}

