package com.nsbm.rocs.entity.pos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Setter
@Getter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "sale_id")
    private Long saleId;

    @Column(name = "payment_type")
    private String paymentType; // CASH, CARD, QR, BANK_TRANSFER

    private BigDecimal amount;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(name = "card_last4")
    private String cardLast4;

    @Column(name = "bank_name")
    private String bankName; // New field for card payments

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Payment{" +
                "paymentId=" + paymentId +
                ", paymentType='" + paymentType + '\'' +
                ", amount=" + amount +
                '}';
    }
}