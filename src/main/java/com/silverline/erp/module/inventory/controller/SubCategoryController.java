package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.inventory.dto.SubCategoryDTO;
import com.silverline.erp.module.inventory.service.SubCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/inventory/subcategories", "/api/inventory/subcategories"})
@RequiredArgsConstructor
@Tag(name = "Product Subcategories", description = "APIs for defining, organizing, editing, and deactivating secondary (sub) categories of product inventory catalog")
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    @Operation(summary = "Get all subcategories", description = "Retrieves a list of all registered product subcategories")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subcategories list retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllSubCategories() {
        List<SubCategoryDTO> subCategories = subCategoryService.getAllSubCategories();
        return ResponseEntity.ok(ApiResponse.success("Subcategories retrieved successfully", subCategories));
    }

    @Operation(summary = "Get active subcategories", description = "Retrieves a list of all active product subcategories")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active subcategories list retrieved successfully")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<?>> getActiveSubCategories() {
        List<SubCategoryDTO> subCategories = subCategoryService.getActiveSubCategories();
        return ResponseEntity.ok(ApiResponse.success("Active subcategories retrieved successfully", subCategories));
    }

    @Operation(summary = "Get subcategories by parent category ID", description = "Lists all subcategories that belong to a specific primary category ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subcategories list retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Parent category not found")
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<?>> getSubCategoriesByCategoryId(@PathVariable Long categoryId) {
        List<SubCategoryDTO> subCategories = subCategoryService.getSubCategoriesByCategoryId(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Subcategories retrieved successfully", subCategories));
    }

    @Operation(summary = "Get subcategory by ID", description = "Retrieves subcategory details by subcategory database ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subcategory retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Subcategory not found")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getSubCategoryById(@PathVariable Long id) {
        SubCategoryDTO subCategory = subCategoryService.getSubCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Subcategory retrieved successfully", subCategory));
    }

    @Operation(summary = "Create a subcategory", description = "Registers a new product subcategory under a primary category")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Subcategory created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload or schema validation error")
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createSubCategory(@Valid @RequestBody SubCategoryDTO subCategoryDTO) {
        SubCategoryDTO createdSubCategory = subCategoryService.createSubCategory(subCategoryDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subcategory created successfully", createdSubCategory));
    }

    @Operation(summary = "Update subcategory details", description = "Modifies name, parent category, or status for an existing subcategory ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subcategory updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Subcategory not found")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateSubCategory(
            @PathVariable Long id,
            @Valid @RequestBody SubCategoryDTO subCategoryDTO) {
        SubCategoryDTO updatedSubCategory = subCategoryService.updateSubCategory(id, subCategoryDTO);
        return ResponseEntity.ok(ApiResponse.success("Subcategory updated successfully", updatedSubCategory));
    }

    @Operation(summary = "Delete subcategory", description = "Removes a subcategory record from the registry database")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subcategory deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Subcategory not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteSubCategory(@PathVariable Long id) {
        subCategoryService.deleteSubCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Subcategory deleted successfully"));
    }

    @Operation(summary = "Deactivate subcategory", description = "Marks a subcategory as inactive. Inactive subcategories block new products from being assigned to them.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subcategory deactivated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Subcategory not found")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<?>> deactivateSubCategory(@PathVariable Long id) {
        subCategoryService.deactivateSubCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Subcategory deactivated successfully"));
    }
}

