package com.nsbm.rocs.repository.finance;

import com.nsbm.rocs.entity.finance.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Optional<JournalEntry> findByJournalNo(String journalNo);
    List<JournalEntry> findByBranchId(Long branchId);
    List<JournalEntry> findByStatus(String status);
    List<JournalEntry> findByBranchIdAndEntryDateBetween(Long branchId, LocalDate startDate, LocalDate endDate);
}

