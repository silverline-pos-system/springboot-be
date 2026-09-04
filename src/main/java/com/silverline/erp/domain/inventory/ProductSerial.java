package com.silverline.erp.domain.inventory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity(name = "ProductSerial")
@Table(name = "product_serials")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSerial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "serial_id")
    private Long serialId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "serial_no", unique = true, nullable = false, length = 100)
    private String serialNo;

    @Column(name = "serial_no_suffix", length = 100)
    private String serialNoSuffix;

    public static int suffixLength = 9;

    @PrePersist
    @PreUpdate
    public void populateSuffix() {
        if (this.serialNo != null) {
            int len = this.serialNo.length();
            if (len <= suffixLength) {
                this.serialNoSuffix = this.serialNo;
            } else {
                this.serialNoSuffix = this.serialNo.substring(len - suffixLength);
            }
        }
    }

    @Column(name = "barcode", unique = true, length = 60)
    private String barcode;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "status", length = 20)
    private String status = "IN_STOCK"; // IN_STOCK, SOLD, DAMAGED, RETURNED

    @Column(name = "grn_id")
    private Long grnId;

    @Column(name = "sale_id")
    private Long saleId;

    @Column(name = "transfer_id")
    private Long transferId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;
}

