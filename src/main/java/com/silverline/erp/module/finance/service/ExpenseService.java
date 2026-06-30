package com.silverline.erp.module.finance.service;

import com.silverline.erp.domain.pos.Category;
import com.silverline.erp.domain.pos.Payment;
import com.silverline.erp.domain.finance.Expense;
import com.silverline.erp.domain.finance.ExpenseCategory;
import com.silverline.erp.domain.finance.ExpensePayment;
import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.finance.dto.ExpenseCategoryDTO;
import com.silverline.erp.module.finance.dto.ExpenseDTO;
import com.silverline.erp.module.finance.dto.ExpenseDashboardDTO;
import com.silverline.erp.module.finance.dto.ExpensePaymentDTO;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.auth.repository.UserRepository;
import com.silverline.erp.module.finance.repository.ExpenseCategoryRepository;
import com.silverline.erp.module.finance.repository.ExpensePaymentRepository;
import com.silverline.erp.module.finance.repository.ExpenseRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseCategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpensePaymentRepository paymentRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    // --- Expense Categories ---
    
    public List<ExpenseCategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToCategoryDTO)
                .collect(Collectors.toList());
    }
    
    public List<ExpenseCategoryDTO> getActiveCategories() {
        return categoryRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .map(this::mapToCategoryDTO)
                .collect(Collectors.toList());
    }

    public ExpenseCategoryDTO createCategory(ExpenseCategoryDTO dto) {
        if (categoryRepository.findByName(dto.getName()).isPresent()) {
            throw new RuntimeException("Category name already exists");
        }
        ExpenseCategory category = new ExpenseCategory();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        return mapToCategoryDTO(categoryRepository.save(category));
    }

    public ExpenseCategoryDTO updateCategory(Long id, ExpenseCategoryDTO dto) {
        ExpenseCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        
        Optional<ExpenseCategory> existing = categoryRepository.findByName(dto.getName());
        if (existing.isPresent() && !existing.get().getCategoryId().equals(id)) {
            throw new RuntimeException("Category name already exists");
        }
        
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        if (dto.getIsActive() != null) {
            category.setIsActive(dto.getIsActive());
        }
        
        return mapToCategoryDTO(categoryRepository.save(category));
    }
    
    public void toggleCategoryStatus(Long id) {
        ExpenseCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        category.setIsActive(!category.getIsActive());
        categoryRepository.save(category);
    }

    // --- Expenses ---

    public List<ExpenseDTO> getAllExpenses() {
        return expenseRepository.findAll().stream()
                .sorted(Comparator.comparing(Expense::getExpenseDate).reversed())
                .map(this::mapToExpenseDTO)
                .collect(Collectors.toList());
    }

    public List<ExpenseDTO> getExpensesByBranch(Long branchId) {
        return expenseRepository.findByBranchIdOrderByExpenseDateDesc(branchId).stream()
                .map(this::mapToExpenseDTO)
                .collect(Collectors.toList());
    }

    public ExpenseDTO getExpenseById(Long id) {
        return expenseRepository.findById(id)
                .map(this::mapToExpenseDTO)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));
    }

    @Transactional
    public ExpenseDTO createExpense(ExpenseDTO dto, Long userId) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }
        
        ExpenseCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
                
        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Branch not found"));
                
        Expense expense = new Expense();
        expense.setExpenseNo(generateExpenseNo());
        expense.setBranchId(branch.getBranchId());
        expense.setCategory(category);
        expense.setExpenseDate(dto.getExpenseDate() != null ? dto.getExpenseDate() : LocalDate.now());
        expense.setAmount(dto.getAmount());
        expense.setPaymentMethod(dto.getPaymentMethod());
        expense.setReferenceNo(dto.getReferenceNo());
        expense.setDescription(dto.getDescription());
        expense.setStatus("APPROVED");
        expense.setCreatedBy(userId);
        expense.setApprovedBy(userId);
        expense.setApprovedAt(LocalDateTime.now());
        
        Expense savedExpense = expenseRepository.save(expense);
        
        // Auto-create payment if method is specified and amounts to full
        if (dto.getPaymentMethod() != null && !dto.getPaymentMethod().isEmpty() && !dto.getPaymentMethod().equalsIgnoreCase("UNPAID")) {
            ExpensePayment payment = new ExpensePayment();
            payment.setExpense(savedExpense);
            payment.setPaymentDate(savedExpense.getExpenseDate());
            payment.setPaymentMethod(savedExpense.getPaymentMethod());
            payment.setAmount(savedExpense.getAmount());
            payment.setReferenceNo(savedExpense.getReferenceNo());
            payment.setNotes("Initial payment");
            payment.setCreatedBy(userId);
            paymentRepository.save(payment);
        }
        
        return getExpenseById(savedExpense.getExpenseId()); // Fetch to get fully structured DTO
    }

    @Transactional
    public ExpenseDTO updateExpense(Long id, ExpenseDTO dto) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));
                
        if (dto.getCategoryId() != null) {
            ExpenseCategory category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));
            expense.setCategory(category);
        }
        
        if (dto.getAmount() != null && dto.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            // Check if amount is less than total paid
            Double totalPaid = paymentRepository.sumPaymentsByExpenseId(id);
            if (dto.getAmount().compareTo(BigDecimal.valueOf(totalPaid)) < 0) {
                throw new RuntimeException("Amount cannot be less than total payments already made");
            }
            expense.setAmount(dto.getAmount());
        }
        
        if (dto.getExpenseDate() != null) expense.setExpenseDate(dto.getExpenseDate());
        if (dto.getDescription() != null) expense.setDescription(dto.getDescription());
        if (dto.getReferenceNo() != null) expense.setReferenceNo(dto.getReferenceNo());
        if (dto.getPaymentMethod() != null) expense.setPaymentMethod(dto.getPaymentMethod());
        
        expenseRepository.save(expense);
        return getExpenseById(expense.getExpenseId());
    }
    
    @Transactional
    public void deleteExpense(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));
                
        // Delete associated payments first via cascade or manually
        List<ExpensePayment> payments = paymentRepository.findByExpense_ExpenseId(id);
        paymentRepository.deleteAll(payments);
        
        expenseRepository.delete(expense);
    }

    // --- Expense Payments ---

    @Transactional
    public ExpensePaymentDTO createPayment(ExpensePaymentDTO dto, Long userId) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Payment amount must be greater than zero");
        }
        
        Expense expense = expenseRepository.findById(dto.getExpenseId())
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));
                
        Double totalPaid = paymentRepository.sumPaymentsByExpenseId(expense.getExpenseId());
        BigDecimal balance = expense.getAmount().subtract(BigDecimal.valueOf(totalPaid));
        
        if (dto.getAmount().compareTo(balance) > 0) {
            throw new RuntimeException("Payment amount exceeds outstanding balance of " + balance);
        }
        
        ExpensePayment payment = new ExpensePayment();
        payment.setExpense(expense);
        payment.setPaymentDate(dto.getPaymentDate() != null ? dto.getPaymentDate() : LocalDate.now());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setAmount(dto.getAmount());
        payment.setReferenceNo(dto.getReferenceNo());
        payment.setNotes(dto.getNotes());
        payment.setCreatedBy(userId);
        
        return mapToPaymentDTO(paymentRepository.save(payment));
    }
    
    @Transactional
    public void deletePayment(Long id) {
        ExpensePayment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));
        paymentRepository.delete(payment);
    }

    // --- Dashboard & Reports ---

    public ExpenseDashboardDTO getDashboardMetrics() {
        ExpenseDashboardDTO dashboard = new ExpenseDashboardDTO();
        LocalDate today = LocalDate.now();
        
        // Today & Monthly totals
        dashboard.setTodayExpenses(BigDecimal.valueOf(expenseRepository.sumAmountByDate(today)));
        dashboard.setMonthlyExpenses(BigDecimal.valueOf(expenseRepository.sumAmountByMonthAndYear(today.getMonthValue(), today.getYear())));
        
        // Get all expenses for rest of stats
        List<Expense> expenses = expenseRepository.findAll();
        
        // Unpaid calculations
        BigDecimal totalUnpaid = BigDecimal.ZERO;
        
        for(Expense e : expenses) {
            Double paid = paymentRepository.sumPaymentsByExpenseId(e.getExpenseId());
            BigDecimal balance = e.getAmount().subtract(BigDecimal.valueOf(paid));
            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                totalUnpaid = totalUnpaid.add(balance);
            }
        }
        dashboard.setUnpaidExpenses(totalUnpaid);
        
        // Category breakdown (current month)
        List<Expense> thisMonthExpenses = expenseRepository.findByExpenseDateBetween(
                today.withDayOfMonth(1), 
                today.withDayOfMonth(today.lengthOfMonth())
        );
        
        Map<String, BigDecimal> categoryTotals = new HashMap<>();
        for (Expense e : thisMonthExpenses) {
            String catName = e.getCategory().getName();
            categoryTotals.put(catName, categoryTotals.getOrDefault(catName, BigDecimal.ZERO).add(e.getAmount()));
        }
        
        // Find top category
        String topCategory = null;
        BigDecimal maxAmount = BigDecimal.ZERO;
        List<Map<String, Object>> catBreakdown = new ArrayList<>();
        
        for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
            if (entry.getValue().compareTo(maxAmount) > 0) {
                maxAmount = entry.getValue();
                topCategory = entry.getKey();
            }
            Map<String, Object> map = new HashMap<>();
            map.put("category", entry.getKey());
            map.put("amount", entry.getValue());
            catBreakdown.add(map);
        }
        
        catBreakdown.sort((a, b) -> ((BigDecimal)b.get("amount")).compareTo((BigDecimal)a.get("amount")));
        
        dashboard.setTopCategoryName(topCategory != null ? topCategory : "N/A");
        dashboard.setTopCategoryAmount(maxAmount);
        dashboard.setMonthlyCategoryBreakdown(catBreakdown);
        
        // Branch breakdown (current month)
        Map<Long, BigDecimal> branchTotalsMap = new HashMap<>();
        for (Expense e : thisMonthExpenses) {
            branchTotalsMap.put(e.getBranchId(), branchTotalsMap.getOrDefault(e.getBranchId(), BigDecimal.ZERO).add(e.getAmount()));
        }
        
        List<Map<String, Object>> branchBreakdown = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : branchTotalsMap.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            branchRepository.findById(entry.getKey()).ifPresent(branch -> {
                map.put("branchId", branch.getBranchId());
                map.put("branchName", branch.getName());
                map.put("amount", entry.getValue());
                branchBreakdown.add(map);
            });
        }
        
        dashboard.setBranchExpenseBreakdown(branchBreakdown);
        
        return dashboard;
    }

    // --- Helpers ---

    private synchronized String generateExpenseNo() {
        Integer lastSequence = expenseRepository.findMaxExpenseNoSequence();
        int nextId = (lastSequence != null ? lastSequence : 0) + 1;
        return String.format("EXP-%05d", nextId);
    }

    private ExpenseCategoryDTO mapToCategoryDTO(ExpenseCategory category) {
        ExpenseCategoryDTO dto = new ExpenseCategoryDTO();
        dto.setCategoryId(category.getCategoryId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setIsActive(category.getIsActive());
        dto.setCreatedAt(category.getCreatedAt());
        return dto;
    }

    private ExpenseDTO mapToExpenseDTO(Expense expense) {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setExpenseId(expense.getExpenseId());
        dto.setExpenseNo(expense.getExpenseNo());
        dto.setBranchId(expense.getBranchId());
        
        branchRepository.findById(expense.getBranchId())
                .ifPresent(branch -> dto.setBranchName(branch.getName()));
                
        dto.setCategoryId(expense.getCategory().getCategoryId());
        dto.setCategoryName(expense.getCategory().getName());
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setDescription(expense.getDescription());
        dto.setAmount(expense.getAmount());
        dto.setPaymentMethod(expense.getPaymentMethod());
        dto.setReferenceNo(expense.getReferenceNo());
        dto.setStatus(expense.getStatus());
        dto.setCreatedBy(expense.getCreatedBy());
        
        if (expense.getCreatedBy() != null) {
            userRepository.findById(expense.getCreatedBy())
                    .ifPresent(u -> dto.setCreatedByName(u.getFullName()));
        }
        
        dto.setApprovedBy(expense.getApprovedBy());
        dto.setCreatedAt(expense.getCreatedAt());
        dto.setApprovedAt(expense.getApprovedAt());
        
        // Payments
        List<ExpensePayment> payments = paymentRepository.findByExpense_ExpenseId(expense.getExpenseId());
        List<ExpensePaymentDTO> paymentDTOs = payments.stream().map(this::mapToPaymentDTO).collect(Collectors.toList());
        dto.setPayments(paymentDTOs);
        
        Double totalPaidDouble = paymentRepository.sumPaymentsByExpenseId(expense.getExpenseId());
        BigDecimal totalPaid = BigDecimal.valueOf(totalPaidDouble);
        dto.setTotalPaid(totalPaid);
        dto.setBalance(expense.getAmount().subtract(totalPaid));
        
        return dto;
    }

    private ExpensePaymentDTO mapToPaymentDTO(ExpensePayment payment) {
        ExpensePaymentDTO dto = new ExpensePaymentDTO();
        dto.setPaymentId(payment.getPaymentId());
        dto.setExpenseId(payment.getExpense().getExpenseId());
        dto.setExpenseNo(payment.getExpense().getExpenseNo());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setAmount(payment.getAmount());
        dto.setReferenceNo(payment.getReferenceNo());
        dto.setNotes(payment.getNotes());
        dto.setCreatedBy(payment.getCreatedBy());
        
        if (payment.getCreatedBy() != null) {
            userRepository.findById(payment.getCreatedBy())
                    .ifPresent(u -> dto.setCreatedByName(u.getFullName()));
        }
        
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }
}

