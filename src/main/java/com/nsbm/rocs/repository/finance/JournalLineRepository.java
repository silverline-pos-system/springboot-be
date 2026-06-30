package com.nsbm.rocs.repository.finance;

import com.nsbm.rocs.entity.finance.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {
    List<JournalLine> findByJournalId(Long journalId);
    List<JournalLine> findByAccountId(Long accountId);

    @Query("SELECT COALESCE(SUM(jl.debit) - SUM(jl.credit), 0) FROM JournalLine jl WHERE jl.accountId = :accountId")
    BigDecimal getAccountBalance(Long accountId);
}

