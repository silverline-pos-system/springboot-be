package com.silverline.erp.domain.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_order_payments", indexes = {
    @Index(name = "idx_po_payments_po", columnList = "po_id"),
    @Index(name = "idx_po_payments_paid_at", columnList = "paid_at")
})
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrderPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_payment_id")
    private Long poPaymentId;

    @Column(name = "po_id", nullable = false)
    private Long poId;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;

    @Column(name = "payment_reference", length = 120)
    private String paymentReference;

    @Column(name = "amount_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt = LocalDateTime.now();

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "paid_by")
    private Long paidBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (paidAt == null) paidAt = LocalDateTime.now();
    }
}

