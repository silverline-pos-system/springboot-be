package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.dto.PagedResponse;
import com.silverline.erp.module.inventory.dto.ProductDTO;
import com.silverline.erp.module.inventory.dto.ProductDetailsDTO;
import com.silverline.erp.module.inventory.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/inventory/products", "/api/inventory/products"})
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getAllProducts(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductDTO> pageInfo = productService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getActiveProducts(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductDTO> pageInfo = productService.getActiveProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success("Active products retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getProductById(@PathVariable Long id) {
        ProductDTO product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ApiResponse<?>> getProductBySku(@PathVariable String sku) {
        ProductDTO product = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ApiResponse<?>> getProductByBarcode(@PathVariable String barcode) {
        ProductDTO product = productService.getProductByBarcode(barcode);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductDTO> pageInfo = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @GetMapping("/subcategory/{subCategoryId}")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getProductsBySubCategory(
            @PathVariable Long subCategoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductDTO> pageInfo = productService.getProductsBySubCategory(subCategoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @GetMapping("/brand/{brandId}")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getProductsByBrand(
            @PathVariable Long brandId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductDTO> pageInfo = productService.getProductsByBrand(brandId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> searchProducts(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductDTO> pageInfo = productService.searchProducts(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @GetMapping("/next-sku")
    public ResponseEntity<ApiResponse<?>> getNextSku() {
        String nextSku = productService.getNextSku();
        return ResponseEntity.ok(ApiResponse.success("Next SKU retrieved successfully", nextSku));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        ProductDTO createdProduct = productService.createProduct(productDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", createdProduct));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductDTO productDTO) {
        ProductDTO updatedProduct = productService.updateProduct(id, productDTO);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updatedProduct));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<ProductDetailsDTO> getProductDetails(@PathVariable Long id) {
        ProductDetailsDTO details = productService.getProductDetails(id);
        return ResponseEntity.ok(details);
    }
}


