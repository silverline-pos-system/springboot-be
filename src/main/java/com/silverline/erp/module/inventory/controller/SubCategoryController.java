package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.inventory.dto.SubCategoryDTO;
import com.silverline.erp.module.inventory.service.SubCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/inventory/subcategories", "/api/inventory/subcategories"})
@RequiredArgsConstructor
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllSubCategories() {
        List<SubCategoryDTO> subCategories = subCategoryService.getAllSubCategories();
        return ResponseEntity.ok(ApiResponse.success("Subcategories retrieved successfully", subCategories));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<?>> getActiveSubCategories() {
        List<SubCategoryDTO> subCategories = subCategoryService.getActiveSubCategories();
        return ResponseEntity.ok(ApiResponse.success("Active subcategories retrieved successfully", subCategories));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<?>> getSubCategoriesByCategoryId(@PathVariable Long categoryId) {
        List<SubCategoryDTO> subCategories = subCategoryService.getSubCategoriesByCategoryId(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Subcategories retrieved successfully", subCategories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getSubCategoryById(@PathVariable Long id) {
        SubCategoryDTO subCategory = subCategoryService.getSubCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Subcategory retrieved successfully", subCategory));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createSubCategory(@Valid @RequestBody SubCategoryDTO subCategoryDTO) {
        SubCategoryDTO createdSubCategory = subCategoryService.createSubCategory(subCategoryDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subcategory created successfully", createdSubCategory));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateSubCategory(
            @PathVariable Long id,
            @Valid @RequestBody SubCategoryDTO subCategoryDTO) {
        SubCategoryDTO updatedSubCategory = subCategoryService.updateSubCategory(id, subCategoryDTO);
        return ResponseEntity.ok(ApiResponse.success("Subcategory updated successfully", updatedSubCategory));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteSubCategory(@PathVariable Long id) {
        subCategoryService.deleteSubCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Subcategory deleted successfully"));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<?>> deactivateSubCategory(@PathVariable Long id) {
        subCategoryService.deactivateSubCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Subcategory deactivated successfully"));
    }
}


