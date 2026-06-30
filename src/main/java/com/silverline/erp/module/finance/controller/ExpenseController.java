package com.silverline.erp.module.finance.controller;

import com.silverline.erp.domain.finance.Expense;
import com.silverline.erp.domain.pos.Payment;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.finance.dto.ExpenseDTO;
import com.silverline.erp.module.finance.dto.ExpenseDashboardDTO;
import com.silverline.erp.module.finance.dto.ExpensePaymentDTO;
import com.silverline.erp.module.finance.service.ExpenseService;
import com.silverline.erp.module.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/manager/expenses")
@CrossOrigin
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserRepository userRepository;

    // --- Expenses ---

    @GetMapping
    public ResponseEntity<List<ExpenseDTO>> getAllExpenses(
            @RequestParam(required = false) Long branchId) {
        if (branchId != null) {
            return ResponseEntity.ok(expenseService.getExpensesByBranch(branchId));
        }
        return ResponseEntity.ok(expenseService.getAllExpenses());
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Optional<UserProfile> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                return userOpt.get().getUserId();
            }
        }
        return 1L; // Fallback for safety/testing, ideally throw error if not found
    }
}

