package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.domain.pos.Brand;
import com.silverline.erp.common.dto.ApiResponse;

import com.silverline.erp.module.inventory.dto.BrandDTO;
import com.silverline.erp.module.inventory.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllBrands() {
        List<BrandDTO> brands = brandService.getAllBrands();
        return ResponseEntity.ok(ApiResponse.success("Brands retrieved successfully", brands));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<?>> getActiveBrands() {
        List<BrandDTO> brands = brandService.getActiveBrands();
        return ResponseEntity.ok(ApiResponse.success("Active brands retrieved successfully", brands));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getBrandById(@PathVariable Long id) {
        BrandDTO brand = brandService.getBrandById(id);
        return ResponseEntity.ok(ApiResponse.success("Brand retrieved successfully", brand));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createBrand(@Valid @RequestBody BrandDTO brandDTO) {
        BrandDTO createdBrand = brandService.createBrand(brandDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Brand created successfully", createdBrand));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody BrandDTO brandDTO) {
        BrandDTO updatedBrand = brandService.updateBrand(id, brandDTO);
        return ResponseEntity.ok(ApiResponse.success("Brand updated successfully", updatedBrand));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponse.success("Brand deleted successfully"));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<?>> deactivateBrand(@PathVariable Long id) {
        brandService.deactivateBrand(id);
        return ResponseEntity.ok(ApiResponse.success("Brand deactivated successfully"));
    }
}


