package com.silverline.erp.module.finance.controller;

import com.silverline.erp.module.finance.dto.ExpenseCategoryDTO;
import com.silverline.erp.module.finance.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manager/expense-categories")
@RequiredArgsConstructor
public class ExpenseCategoryController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<List<ExpenseCategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(expenseService.getAllCategories());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ExpenseCategoryDTO>> getActiveCategories() {
        return ResponseEntity.ok(expenseService.getActiveCategories());
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody ExpenseCategoryDTO dto) {
        try {
            return ResponseEntity.ok(expenseService.createCategory(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody ExpenseCategoryDTO dto) {
        try {
            return ResponseEntity.ok(expenseService.updateCategory(id, dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

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

