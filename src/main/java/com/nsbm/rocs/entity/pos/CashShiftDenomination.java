package com.nsbm.rocs.entity.pos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_shift_denominations")
@Getter
@Setter
@NoArgsConstructor
public class CashShiftDenomination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "denom_id")
    private Long denomId;

    @Column(name = "shift_id", nullable = false)
    private Long shiftId;

    @Column(name = "denomination_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal denominationValue;

    @Column(nullable = false)
    private Integer quantity;

    // Note: 'amount' is a generated column in DB (denomination_value * quantity)
    // We make it insertable=false, updatable=false since DB calculates it
    @Column(insertable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DenominationType type;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum DenominationType {
        OPENING, CLOSING
    }
}

