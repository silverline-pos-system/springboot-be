package com.silverline.erp.module.inventory.repository;

import com.silverline.erp.domain.inventory.BranchProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchProductRepository extends JpaRepository<BranchProduct, Long> {

    Optional<BranchProduct> findByBranchIdAndProductId(Long branchId, Long productId);

    List<BranchProduct> findByBranchId(Long branchId);

    List<BranchProduct> findByBranchIdAndIsActiveTrue(Long branchId);

    boolean existsByBranchIdAndProductId(Long branchId, Long productId);
}
