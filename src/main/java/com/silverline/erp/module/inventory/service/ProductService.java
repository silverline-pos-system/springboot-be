package com.silverline.erp.module.inventory.service;

import com.silverline.erp.domain.product.Product;
import com.silverline.erp.module.inventory.dto.ProductDTO;
import com.silverline.erp.module.inventory.dto.ProductDetailsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    Page<ProductDTO> getAllProducts(Pageable pageable);
    Page<ProductDTO> getActiveProducts(Pageable pageable);
    ProductDTO getProductById(Long id);
    ProductDTO getProductBySku(String sku);
    ProductDTO getProductByBarcode(String barcode);
    Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable);
    Page<ProductDTO> getProductsBySubCategory(Long subCategoryId, Pageable pageable);
    Page<ProductDTO> getProductsByBrand(Long brandId, Pageable pageable);
    Page<ProductDTO> searchProducts(String keyword, Pageable pageable);
    String getNextSku();
    ProductDTO createProduct(ProductDTO productDTO);
    ProductDTO updateProduct(Long id, ProductDTO productDTO);
    void deleteProduct(Long id);
    void deactivateProduct(Long id);
    ProductDetailsDTO getProductDetails(Long productId);

    // Cross-module APIs
    Product findById(Long id);
    List<Product> findByBranch(Long branchId);
    Product findByBarcode(String barcode);
    List<Product> findProductsByIds(List<Long> ids);
    Product findBySku(String sku);
    List<Product> searchProductEntities(String keyword);
    List<Product> getActiveProductsLimit(int limit);
}
