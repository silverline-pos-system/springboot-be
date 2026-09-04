package com.silverline.erp.module.procurement.repository;

import com.silverline.erp.domain.procurement.GrnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrnItemRepository extends JpaRepository<GrnItem, Long> {

    List<GrnItem> findByGrnId(Long grnId);

    List<GrnItem> findByProductId(Long productId);

    @Query("SELECT gi FROM GrnItem gi JOIN Grn g ON gi.grnId = g.grnId WHERE g.branchId = :branchId AND gi.productId = :productId")
    List<GrnItem> findByBranchIdAndProductId(@Param("branchId") Long branchId, @Param("productId") Long productId);

    @Query("SELECT COUNT(gi) > 0 FROM GrnItem gi WHERE gi.productId = :productId")
    boolean existsByProductId(@Param("productId") Long productId);
}
