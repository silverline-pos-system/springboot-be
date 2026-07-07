package com.silverline.erp.module.finance.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.dto.PagedResponse;
import com.silverline.erp.module.auth.repository.UserRepository;
import com.silverline.erp.module.finance.dto.ExpenseDTO;
import com.silverline.erp.module.finance.dto.ExpenseDashboardDTO;
import com.silverline.erp.module.finance.dto.ExpensePaymentDTO;
import com.silverline.erp.module.finance.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manager/expenses")
@RequiredArgsConstructor
@Tag(name = "Branch Expenses", description = "APIs for logging branch operational expenditures, processing installment payments, and loading expense metrics summaries")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final UserRepository userRepository;

    // --- Expenses ---

    @Operation(summary = "Get all operational expenses", description = "Retrieves a paginated list of all logged expenses with optional branch filtering")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expenses list retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ExpenseDTO>>> getAllExpenses(
            @RequestParam(required = false) Long branchId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ExpenseDTO> pageInfo = expenseService.getAllExpenses(pageable);
        return ResponseEntity.ok(ApiResponse.success("Expenses retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Get expense by ID", description = "Looks up details, amounts, and payments for a specific expense entry by ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expense details retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Expense record not found")
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseDTO> getExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.getExpenseById(id));
    }

    @Operation(summary = "Log a new operational expense", description = "Creates a new operational expenditure item (rent, utility, salary, general) and triggers journal posting")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expense logged successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid expense input or business rule validation failure")
    @PostMapping
    public ResponseEntity<?> createExpense(@RequestBody ExpenseDTO dto) {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok(expenseService.createExpense(dto, userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Update expense details", description = "Modifies values (amount, notes, category) of a draft/unsettled expense entry")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expense details updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot update fully paid expense records")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Expense record not found")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateExpense(@PathVariable Long id, @RequestBody ExpenseDTO dto) {
        try {
            return ResponseEntity.ok(expenseService.updateExpense(id, dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Delete expense entry", description = "Removes an operational expense entry from the database ledger")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expense deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot delete expenses that have payments attached")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Expense record not found")
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

    @Operation(summary = "Add expense payment installment", description = "Logs a payment payment transaction against an outstanding expense item")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment installment logged successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Payment amount exceeds remaining due amount")
    @PostMapping("/payments")
    public ResponseEntity<?> addPayment(@RequestBody ExpensePaymentDTO dto) {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok(expenseService.createPayment(dto, userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Delete expense payment installment", description = "Removes a payment entry and rolls back the expense payment status")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment installment deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment entry not found")
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

    @Operation(summary = "Get expense dashboard stats", description = "Retrieves summarized figures (weekly spending, monthly comparison, spending by category)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expense dashboard data compiled successfully")
    @GetMapping("/dashboard")
    public ResponseEntity<ExpenseDashboardDTO> getDashboardMetrics() {
        return ResponseEntity.ok(expenseService.getDashboardMetrics());
    }

    // --- Helpers ---

    private Long getCurrentUserId() {
        Long userId = com.silverline.erp.common.security.SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new com.silverline.erp.common.exception.UnauthorizedException("User is not authenticated");
        }
        return userId;
    }
}

