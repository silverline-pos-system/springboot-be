package com.silverline.erp.module.inventory.service;

import com.silverline.erp.domain.product.Product;
import com.silverline.erp.module.inventory.dto.ProductDTO;
import com.silverline.erp.module.inventory.dto.ProductDetailsDTO;

import java.util.List;

public interface ProductService {
    List<ProductDTO> getAllProducts();
    List<ProductDTO> getActiveProducts();
    ProductDTO getProductById(Long id);
    ProductDTO getProductBySku(String sku);
    ProductDTO getProductByBarcode(String barcode);
    List<ProductDTO> getProductsByCategory(Long categoryId);
    List<ProductDTO> getProductsBySubCategory(Long subCategoryId);
    List<ProductDTO> getProductsByBrand(Long brandId);
    List<ProductDTO> searchProducts(String keyword);
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
}
