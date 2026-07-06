package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.dto.PagedResponse;
import com.silverline.erp.module.inventory.dto.ProductDTO;
import com.silverline.erp.module.inventory.dto.ProductDetailsDTO;
import com.silverline.erp.module.inventory.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Products", description = "Product catalog management APIs including CRUD, search, and SKU/barcode lookups")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get all products", description = "Returns a paginated list of all products in the catalog")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getAllProducts(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductDTO> pageInfo = productService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Get active products", description = "Returns a paginated list of active products")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active products retrieved successfully")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getActiveProducts(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductDTO> pageInfo = productService.getActiveProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success("Active products retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Get product by ID", description = "Retrieves a specific product by its database ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getProductById(@PathVariable Long id) {
        ProductDTO product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }

    @Operation(summary = "Get product by SKU", description = "Retrieves a product by its unique SKU code")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ApiResponse<?>> getProductBySku(@PathVariable String sku) {
        ProductDTO product = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }

    @Operation(summary = "Get product by barcode", description = "Retrieves a product by its barcode string")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ApiResponse<?>> getProductByBarcode(@PathVariable String barcode) {
        ProductDTO product = productService.getProductByBarcode(barcode);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }

    @Operation(summary = "Get products by category", description = "Returns a paginated list of products under a specific category")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductDTO> pageInfo = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Get products by subcategory", description = "Returns a paginated list of products under a subcategory")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    @GetMapping("/subcategory/{subCategoryId}")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getProductsBySubCategory(
            @PathVariable Long subCategoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductDTO> pageInfo = productService.getProductsBySubCategory(subCategoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Get products by brand", description = "Returns a paginated list of products under a brand")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    @GetMapping("/brand/{brandId}")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getProductsByBrand(
            @PathVariable Long brandId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductDTO> pageInfo = productService.getProductsByBrand(brandId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Search products by keyword", description = "Finds products matching name, SKU, description, or barcode keyword")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> searchProducts(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductDTO> pageInfo = productService.searchProducts(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Get next auto-generated SKU", description = "Computes the next sequence number for a new product SKU code")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Next SKU retrieved successfully")
    @GetMapping("/next-sku")
    public ResponseEntity<ApiResponse<?>> getNextSku() {
        String nextSku = productService.getNextSku();
        return ResponseEntity.ok(ApiResponse.success("Next SKU retrieved successfully", nextSku));
    }

    @Operation(summary = "Create a new product", description = "Creates a new product catalog record")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request details")
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        ProductDTO createdProduct = productService.createProduct(productDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", createdProduct));
    }

    @Operation(summary = "Update an existing product", description = "Updates a product catalog record by ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductDTO productDTO) {
        ProductDTO updatedProduct = productService.updateProduct(id, productDTO);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updatedProduct));
    }

    @Operation(summary = "Delete a product", description = "Deletes a product record by ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "244", description = "Product deleted successfully (No Content)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get detailed product information", description = "Retrieves a product details object including current stock level and brand info")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product details retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/{id}/details")
    public ResponseEntity<ProductDetailsDTO> getProductDetails(@PathVariable Long id) {
        ProductDetailsDTO details = productService.getProductDetails(id);
        return ResponseEntity.ok(details);
    }
}


