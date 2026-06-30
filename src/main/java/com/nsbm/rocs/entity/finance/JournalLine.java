package com.nsbm.rocs.entity.finance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "journal_lines")
@Getter
@Setter
@NoArgsConstructor
public class JournalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;

    @Column(name = "journal_id", nullable = false)
    private Long journalId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(precision = 15, scale = 2)
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal credit = BigDecimal.ZERO;

    @Column()
    private String description;
}

