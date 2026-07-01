package com.silverline.erp.domain.pos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CashShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "shift_no", unique = true, length = 50)
    private String shiftNo;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    // NOTE: terminal_id REMOVED â€” shifts are tied to branches, not terminals

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "opening_cash", precision = 15, scale = 2)
    private BigDecimal openingCash = BigDecimal.ZERO;

    @Column(name = "closing_cash", precision = 15, scale = 2)
    private BigDecimal closingCash = BigDecimal.ZERO;

    @Column(name = "expected_cash", precision = 15, scale = 2)
    private BigDecimal expectedCash = BigDecimal.ZERO;

    @Column(name = "cash_difference", precision = 15, scale = 2)
    private BigDecimal cashDifference = BigDecimal.ZERO;

    @Column(name = "total_sales", precision = 15, scale = 2)
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(name = "total_returns", precision = 15, scale = 2)
    private BigDecimal totalReturns = BigDecimal.ZERO;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ShiftStatus status = ShiftStatus.PENDING_APPROVAL;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (openedAt == null) openedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ShiftStatus {
        PENDING_APPROVAL, OPEN, CLOSED, SUSPENDED
    }

    @Override
    public String toString() {
        return "CashShift{" +
                "shiftId=" + shiftId +
                ", shiftNo='" + shiftNo + '\'' +
                ", branchId=" + branchId +
                ", cashierId=" + cashierId +
                ", openedAt=" + openedAt +
                ", closedAt=" + closedAt +
                ", openingCash=" + openingCash +
                ", closingCash=" + closingCash +
                ", status=" + status +
                '}';
    }

}