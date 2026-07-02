package com.silverline.erp.module.inventory.repository;

import com.silverline.erp.domain.procurement.SupplierBranch;
import com.silverline.erp.domain.procurement.SupplierBranchId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierBranchRepository extends JpaRepository<SupplierBranch, SupplierBranchId> {
}


