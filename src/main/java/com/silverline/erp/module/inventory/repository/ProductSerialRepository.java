package com.silverline.erp.module.inventory.repository;

import com.silverline.erp.domain.inventory.ProductSerial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSerialRepository extends JpaRepository<ProductSerial, Long> {

    Optional<ProductSerial> findBySerialNo(String serialNo);

    Optional<ProductSerial> findByBarcode(String barcode);

    List<ProductSerial> findByProductId(Long productId);

    List<ProductSerial> findByBranchId(Long branchId);

    List<ProductSerial> findByStatus(String status);

    List<ProductSerial> findByBranchIdAndStatus(Long branchId, String status);

    List<ProductSerial> findByProductIdAndBranchIdAndStatus(Long productId, Long branchId, String status);

    List<ProductSerial> findByBranchIdAndProductIdAndStatus(Long branchId, Long productId, String status);

    boolean existsBySerialNo(String serialNo);

    @Query("SELECT ps FROM InventoryProductSerial ps WHERE ps.branchId = :branchId AND ps.status = 'IN_STOCK'")
    List<ProductSerial> findAvailableSerials(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(ps) FROM InventoryProductSerial ps WHERE ps.productId = :productId AND ps.branchId = :branchId AND ps.status = 'IN_STOCK'")
    Long countAvailableSerialsByProduct(@Param("productId") Long productId, @Param("branchId") Long branchId);

        @Query("SELECT ps FROM InventoryProductSerial ps WHERE " +
            "(:branchId IS NULL OR ps.branchId = :branchId) AND " +
            "(:productId IS NULL OR ps.productId = :productId) AND " +
            "(:status IS NULL OR ps.status = :status) AND " +
            "(:search IS NULL OR lower(ps.serialNo) LIKE lower(concat('%', :search, '%')))")
        Page<ProductSerial> findByLookupFilters(
            @Param("branchId") Long branchId,
            @Param("productId") Long productId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);

        @Query("SELECT ps FROM InventoryProductSerial ps WHERE " +
            "(:status IS NULL OR ps.status = :status) AND " +
            "(:search IS NULL OR lower(ps.serialNo) LIKE lower(concat('%', :search, '%'))) AND " +
            "ps.branchId IN :branchIds AND ps.productId IN :productIds")
        Page<ProductSerial> findByLookupFiltersWithLists(
            @Param("branchIds") List<Long> branchIds,
            @Param("productIds") List<Long> productIds,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
}


