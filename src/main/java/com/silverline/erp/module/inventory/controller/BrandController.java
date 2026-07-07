package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.inventory.dto.BrandDTO;
import com.silverline.erp.module.inventory.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/inventory/brands", "/api/inventory/brands"})
@RequiredArgsConstructor
@Tag(name = "Product Brands", description = "APIs for registering, editing, search listing, and deactivating product brands")
public class BrandController {

    private final BrandService brandService;

    @Operation(summary = "Get all brands", description = "Retrieves a list of all registered product brands")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brands list retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllBrands() {
        List<BrandDTO> brands = brandService.getAllBrands();
        return ResponseEntity.ok(ApiResponse.success("Brands retrieved successfully", brands));
    }

    @Operation(summary = "Get active brands", description = "Retrieves a list of all active product brands")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active brands list retrieved successfully")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<?>> getActiveBrands() {
        List<BrandDTO> brands = brandService.getActiveBrands();
        return ResponseEntity.ok(ApiResponse.success("Active brands retrieved successfully", brands));
    }

    @Operation(summary = "Get brand by ID", description = "Retrieves brand name, code, status, and description by brand database ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brand retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Brand not found")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getBrandById(@PathVariable Long id) {
        BrandDTO brand = brandService.getBrandById(id);
        return ResponseEntity.ok(ApiResponse.success("Brand retrieved successfully", brand));
    }

    @Operation(summary = "Create a brand", description = "Registers a new product brand in the inventory catalog")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "21", description = "Brand created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload or schema validation error")
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createBrand(@Valid @RequestBody BrandDTO brandDTO) {
        BrandDTO createdBrand = brandService.createBrand(brandDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Brand created successfully", createdBrand));
    }

    @Operation(summary = "Update brand details", description = "Modifies name, code, status, or description for an existing brand ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brand details updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Brand not found")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody BrandDTO brandDTO) {
        BrandDTO updatedBrand = brandService.updateBrand(id, brandDTO);
        return ResponseEntity.ok(ApiResponse.success("Brand updated successfully", updatedBrand));
    }

    @Operation(summary = "Delete brand", description = "Removes a brand record from the registry database")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brand deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Brand not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponse.success("Brand deleted successfully"));
    }

    @Operation(summary = "Deactivate brand", description = "Marks a brand as inactive. Inactive brands cannot be selected for new products.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brand deactivated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Brand not found")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<?>> deactivateBrand(@PathVariable Long id) {
        brandService.deactivateBrand(id);
        return ResponseEntity.ok(ApiResponse.success("Brand deactivated successfully"));
    }
}

