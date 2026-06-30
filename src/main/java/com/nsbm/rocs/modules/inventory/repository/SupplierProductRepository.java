package com.nsbm.rocs.modules.inventory.repository;

import com.nsbm.rocs.entity.inventory.SupplierProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Long> {
    List<SupplierProduct> findBySupplierId(Long supplierId);
    List<SupplierProduct> findByProductId(Long productId);
    Optional<SupplierProduct> findBySupplierIdAndProductId(Long supplierId, Long productId);
    List<SupplierProduct> findByProductIdAndIsPreferredTrue(Long productId);
}


