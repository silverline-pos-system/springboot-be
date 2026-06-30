package com.nsbm.rocs.modules.inventory.repository;

import com.nsbm.rocs.entity.inventory.PurchaseOrder;
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
}

