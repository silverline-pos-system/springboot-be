package com.nsbm.rocs.repository.finance;

import com.nsbm.rocs.entity.finance.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    
    Optional<Expense> findByExpenseNo(String expenseNo);
    
    List<Expense> findByBranchIdOrderByExpenseDateDesc(Long branchId);
    
    @Query("SELECT MAX(CAST(SUBSTRING(e.expenseNo, 5) AS int)) FROM Expense e WHERE e.expenseNo LIKE 'EXP-%'")
    Integer findMaxExpenseNoSequence();
    
    // For queries related to reports
    @Query("SELECT e FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    List<Expense> findByExpenseDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Dashboard metrics
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate = :date")
    Double sumAmountByDate(@Param("date") LocalDate date);
    
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE MONTH(e.expenseDate) = :month AND YEAR(e.expenseDate) = :year")
    Double sumAmountByMonthAndYear(@Param("month") int month, @Param("year") int year);
}
