package com.nsbm.rocs.entity.pos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales")
@Setter
@Getter
@NoArgsConstructor
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_id")
    private Long saleId;

    @Column(name = "invoice_no", unique = true)
    private String invoiceNo;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "cashier_id")
    private Long cashierId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "sale_date")
    private LocalDateTime saleDate;

    // Money calculations
    @Column(name = "gross_total", precision = 12, scale = 2)
    private BigDecimal grossTotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "net_total", precision = 12, scale = 2)
    private BigDecimal netTotal;

    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "change_amount", precision = 10, scale = 2)
    private BigDecimal changeAmount;

    // Status
    @Column(name = "payment_status")
    private String paymentStatus; // PAID, PARTIAL, PENDING

    @Column(name = "sale_type")
    private String saleType; // RETAIL, WHOLESALE

    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (saleDate == null) saleDate = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Sale{" +
                "saleId=" + saleId +
                ", invoiceNo='" + invoiceNo + '\'' +
                ", netTotal=" + netTotal +
                ", paymentStatus='" + paymentStatus + '\'' +
                '}';
    }
}