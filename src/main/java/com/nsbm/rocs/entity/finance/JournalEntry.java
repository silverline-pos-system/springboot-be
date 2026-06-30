package com.nsbm.rocs.entity.finance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@NoArgsConstructor
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "journal_id")
    private Long journalId;

    @Column(name = "journal_no", unique = true, nullable = false, length = 50)
    private String journalNo;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "transaction_type", length = 50)
    private String transactionType;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @Column()
    private String memo;

    @Column(name = "total_debit", precision = 15, scale = 2)
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Column(name = "total_credit", precision = 15, scale = 2)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(length = 20)
    private String status = "DRAFT";

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "posted_by")
    private Long postedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (entryDate == null) entryDate = LocalDate.now();
    }
}

