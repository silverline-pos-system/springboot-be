package com.silverline.erp.module.inventory.repository;

import com.silverline.erp.domain.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByIsActiveTrue();

    Page<Product> findByIsActiveTrue(Pageable pageable);

    Optional<Product> findBySku(String sku);

    Optional<Product> findByBarcode(String barcode);

    List<Product> findByCategoryId(Long categoryId);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    List<Product> findByCategoryIdAndIsActiveTrue(Long categoryId);

    List<Product> findBySubcategoryId(Long subcategoryId);

    Page<Product> findBySubcategoryId(Long subcategoryId, Pageable pageable);

    List<Product> findByBrandId(Long brandId);

    Page<Product> findByBrandId(Long brandId, Pageable pageable);

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    @Query("SELECT p FROM Product p WHERE " +
           "p.isActive = true AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.barcode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchProducts(@Param("keyword") String keyword);

    @Query("SELECT p FROM Product p WHERE " +
           "p.isActive = true AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.barcode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT MAX(p.productId) FROM Product p")
    Long getMaxProductId();
}

