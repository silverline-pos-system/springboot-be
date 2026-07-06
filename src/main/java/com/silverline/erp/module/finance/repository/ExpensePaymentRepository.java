package com.silverline.erp.module.finance.repository;

import com.silverline.erp.domain.finance.ExpensePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpensePaymentRepository extends JpaRepository<ExpensePayment, Long> {

    List<ExpensePayment> findByExpense_ExpenseId(Long expenseId);

    @Query("SELECT COALESCE(SUM(ep.amount), 0) FROM ExpensePayment ep WHERE ep.expense.expenseId = :expenseId")
    Double sumPaymentsByExpenseId(@Param("expenseId") Long expenseId);
}
