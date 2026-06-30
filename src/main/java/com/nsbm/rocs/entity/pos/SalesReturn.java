package com.nsbm.rocs.entity.pos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_returns")
@Getter
@Setter
@NoArgsConstructor
public class SalesReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id")
    private Long returnId;

    @Column(name = "return_no", unique = true, nullable = false, length = 50)
    private String returnNo;

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "refund_method", length = 30)
    private String refundMethod;

    @Column(length = 20)
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "salesReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesReturnItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        returnDate = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }
}

