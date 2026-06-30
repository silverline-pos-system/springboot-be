package com.nsbm.rocs.modules.inventory.repository;

import com.nsbm.rocs.entity.inventory.SupplierBranch;
import com.nsbm.rocs.entity.inventory.SupplierBranchId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierBranchRepository extends JpaRepository<SupplierBranch, SupplierBranchId> {
}


