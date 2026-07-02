package com.silverline.erp.module.finance.service;

import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.finance.Expense;
import com.silverline.erp.domain.finance.ExpenseCategory;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.auth.repository.UserRepository;
import com.silverline.erp.module.finance.dto.ExpenseCategoryDTO;
import com.silverline.erp.module.finance.dto.ExpenseDTO;
import com.silverline.erp.module.finance.repository.ExpenseCategoryRepository;
import com.silverline.erp.module.finance.repository.ExpensePaymentRepository;
import com.silverline.erp.module.finance.repository.ExpenseRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseCategoryRepository categoryRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private ExpensePaymentRepository paymentRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private ExpenseCategory category;
    private ExpenseCategoryDTO categoryDTO;
    private ExpenseDTO expenseDTO;
    private Branch branch;

    @BeforeEach
    void setUp() {
        category = new ExpenseCategory();
        category.setCategoryId(1L);
        category.setName("Office Supplies");
        category.setDescription("Pens, papers, etc.");
        category.setIsActive(true);

        categoryDTO = new ExpenseCategoryDTO();
        categoryDTO.setCategoryId(1L);
        categoryDTO.setName("Office Supplies");
        categoryDTO.setDescription("Pens, papers, etc.");
        categoryDTO.setIsActive(true);

        expenseDTO = new ExpenseDTO();
        expenseDTO.setCategoryId(1L);
        expenseDTO.setBranchId(10L);
        expenseDTO.setAmount(BigDecimal.valueOf(150.50));
        expenseDTO.setPaymentMethod("CASH");
        expenseDTO.setExpenseDate(LocalDate.now());

        branch = new Branch();
        branch.setBranchId(10L);
        branch.setName("Main Branch");
        branch.setIsActive(true);
    }

    @Test
    void createCategory_Success() {
        // Arrange
        when(categoryRepository.findByName("Office Supplies")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(ExpenseCategory.class))).thenReturn(category);

        // Act
        ExpenseCategoryDTO result = expenseService.createCategory(categoryDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Office Supplies", result.getName());
        verify(categoryRepository).save(any(ExpenseCategory.class));
    }

    @Test
    void createCategory_DuplicateName_ThrowsException() {
        // Arrange
        when(categoryRepository.findByName("Office Supplies")).thenReturn(Optional.of(category));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                expenseService.createCategory(categoryDTO)
        );
        assertTrue(exception.getMessage().contains("Category name already exists"));
        verify(categoryRepository, never()).save(any(ExpenseCategory.class));
    }

    @Test
    void createExpense_Success() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));

        Expense mockExpense = new Expense();
        mockExpense.setExpenseId(100L);
        mockExpense.setExpenseNo("EXP-001");
        mockExpense.setCategory(category);
        mockExpense.setBranchId(10L);
        mockExpense.setAmount(BigDecimal.valueOf(150.50));
        mockExpense.setPaymentMethod("CASH");
        mockExpense.setExpenseDate(LocalDate.now());

        when(expenseRepository.save(any(Expense.class))).thenReturn(mockExpense);
        when(expenseRepository.findById(100L)).thenReturn(Optional.of(mockExpense));

        // Act
        ExpenseDTO result = expenseService.createExpense(expenseDTO, 5L);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(150.50), result.getAmount());
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    void createExpense_ZeroAmount_ThrowsException() {
        // Arrange
        expenseDTO.setAmount(BigDecimal.ZERO);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                expenseService.createExpense(expenseDTO, 5L)
        );
        assertTrue(exception.getMessage().contains("Amount must be greater than zero"));
    }

    @Test
    void createExpense_CategoryNotFound_ThrowsException() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () ->
                expenseService.createExpense(expenseDTO, 5L)
        );
    }
}
