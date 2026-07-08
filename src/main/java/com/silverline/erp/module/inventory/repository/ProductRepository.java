package com.silverline.erp.module.inventory.repository;

import com.silverline.erp.domain.product.Product;
import com.silverline.erp.module.inventory.dto.projection.ProductNameProjection;
import com.silverline.erp.module.inventory.dto.projection.ProductStockProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByIsActiveTrue();

    List<ProductStockProjection> findActiveProjectionsByIsActiveTrue();

    List<ProductNameProjection> findByProductIdIn(Collection<Long> productIds);

    @EntityGraph(attributePaths = {"category", "subCategory", "brand", "unit"})
    @Override
    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"category", "subCategory", "brand", "unit"})
    Page<Product> findByIsActiveTrue(Pageable pageable);

    Optional<Product> findBySku(String sku);

    Optional<Product> findByBarcode(String barcode);

    List<Product> findByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = {"category", "subCategory", "brand", "unit"})
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    List<Product> findByCategoryIdAndIsActiveTrue(Long categoryId);

    List<Product> findBySubcategoryId(Long subcategoryId);

    @EntityGraph(attributePaths = {"category", "subCategory", "brand", "unit"})
    Page<Product> findBySubcategoryId(Long subcategoryId, Pageable pageable);

    List<Product> findByBrandId(Long brandId);

    @EntityGraph(attributePaths = {"category", "subCategory", "brand", "unit"})
    Page<Product> findByBrandId(Long brandId, Pageable pageable);

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    @EntityGraph(attributePaths = {"category", "subCategory", "brand", "unit"})
    @Query("SELECT p FROM Product p WHERE " +
            "p.isActive = true AND (" +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.barcode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchProducts(@Param("keyword") String keyword);

    @EntityGraph(attributePaths = {"category", "subCategory", "brand", "unit"})
    @Query("SELECT p FROM Product p WHERE " +
            "p.isActive = true AND (" +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.barcode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT MAX(p.productId) FROM Product p")
    Long getMaxProductId();

    @Query("SELECT COUNT(poi) > 0 FROM PurchaseOrderItem poi WHERE poi.productId = :productId")
    boolean isUsedInPurchaseOrders(@Param("productId") Long productId);

    @Query("SELECT COUNT(di) > 0 FROM DispatchItem di WHERE di.productId = :productId")
    boolean isUsedInDispatches(@Param("productId") Long productId);

    @Query("SELECT COUNT(si) > 0 FROM SaleItem si WHERE si.productId = :productId")
    boolean isUsedInSales(@Param("productId") Long productId);
}

