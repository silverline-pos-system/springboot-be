package com.silverline.erp.module.pos.repository;

import com.silverline.erp.domain.pos.QuickPickItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuickPickRepository extends JpaRepository<QuickPickItem, Long> {
    List<QuickPickItem> findByBranchId(Long branchId);

    Optional<QuickPickItem> findByBranchIdAndProductId(Long branchId, Long productId);

    void deleteByBranchIdAndProductId(Long branchId, Long productId);
}

