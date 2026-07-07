package com.silverline.erp.module.finance.controller;

import com.silverline.erp.module.finance.dto.ExpenseCategoryDTO;
import com.silverline.erp.module.finance.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manager/expense-categories")
@RequiredArgsConstructor
@Tag(name = "Expense Categories", description = "APIs for managers to define, edit, and toggle active status on store expenditure categories")
public class ExpenseCategoryController {

    private final ExpenseService expenseService;

    @Operation(summary = "Get all expense categories", description = "Retrieves a list of all defined expense categories")
    @ApiResponse(responseCode = "200", description = "Expense categories list retrieved successfully")
    @GetMapping
    public ResponseEntity<List<ExpenseCategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(expenseService.getAllCategories());
    }

    @Operation(summary = "Get active expense categories", description = "Retrieves a list of categories currently active and available for tracking new expenses")
    @ApiResponse(responseCode = "200", description = "Active categories list retrieved successfully")
    @GetMapping("/active")
    public ResponseEntity<List<ExpenseCategoryDTO>> getActiveCategories() {
        return ResponseEntity.ok(expenseService.getActiveCategories());
    }

    @Operation(summary = "Create an expense category", description = "Creates a new category key for cataloging branch expenditures")
    @ApiResponse(responseCode = "200", description = "Expense category created successfully")
    @ApiResponse(responseCode = "400", description = "Duplicate category name or schema validation failure")
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody ExpenseCategoryDTO dto) {
        try {
            return ResponseEntity.ok(expenseService.createCategory(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Update expense category details", description = "Modifies name, code description, or parameters of an existing category ID")
    @ApiResponse(responseCode = "200", description = "Expense category updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request payload or schema validation failure")
    @ApiResponse(responseCode = "404", description = "Category ID not found")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody ExpenseCategoryDTO dto) {
        try {
            return ResponseEntity.ok(expenseService.updateCategory(id, dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Toggle category active status", description = "Toggles category status between active and inactive states")
    @ApiResponse(responseCode = "200", description = "Category status toggled successfully")
    @ApiResponse(responseCode = "404", description = "Category ID not found")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleCategoryStatus(@PathVariable Long id) {
        try {
            expenseService.toggleCategoryStatus(id);
            return ResponseEntity.ok("Status toggled successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
