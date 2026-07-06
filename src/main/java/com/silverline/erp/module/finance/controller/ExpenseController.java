package com.silverline.erp.module.finance.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.dto.PagedResponse;
import com.silverline.erp.module.auth.repository.UserRepository;
import com.silverline.erp.module.finance.dto.ExpenseDTO;
import com.silverline.erp.module.finance.dto.ExpenseDashboardDTO;
import com.silverline.erp.module.finance.dto.ExpensePaymentDTO;
import com.silverline.erp.module.finance.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manager/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    private final UserRepository userRepository;

    // --- Expenses ---

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ExpenseDTO>>> getAllExpenses(
            @RequestParam(required = false) Long branchId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ExpenseDTO> pageInfo = expenseService.getAllExpenses(pageable);
        return ResponseEntity.ok(ApiResponse.success("Expenses retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseDTO> getExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.getExpenseById(id));
    }

    @PostMapping
    public ResponseEntity<?> createExpense(@RequestBody ExpenseDTO dto) {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok(expenseService.createExpense(dto, userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateExpense(@PathVariable Long id, @RequestBody ExpenseDTO dto) {
        try {
            return ResponseEntity.ok(expenseService.updateExpense(id, dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable Long id) {
        try {
            expenseService.deleteExpense(id);
            return ResponseEntity.ok("Expense deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- Expense Payments ---

    @PostMapping("/payments")
    public ResponseEntity<?> addPayment(@RequestBody ExpensePaymentDTO dto) {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok(expenseService.createPayment(dto, userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/payments/{id}")
    public ResponseEntity<?> deletePayment(@PathVariable Long id) {
        try {
            expenseService.deletePayment(id);
            return ResponseEntity.ok("Payment deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- Dashboard ---

    @GetMapping("/dashboard")
    public ResponseEntity<ExpenseDashboardDTO> getDashboardMetrics() {
        return ResponseEntity.ok(expenseService.getDashboardMetrics());
    }

    // --- Helpers ---

    private Long getCurrentUserId() {
        Long userId = com.silverline.erp.common.security.SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("User is not authenticated");
        }
        return userId;
    }
}

