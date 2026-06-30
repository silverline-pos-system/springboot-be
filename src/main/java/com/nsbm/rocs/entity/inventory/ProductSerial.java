package com.nsbm.rocs.entity.inventory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity(name = "InventoryProductSerial")
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

    @Column(name = "barcode", unique = true, length = 60)
    private String barcode;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "status", length = 20)
    private String status = "IN_STOCK"; // IN_STOCK, SOLD, DAMAGED, RETURNED

    @Column(name = "dispatch_id")
    private Long dispatchId;

    @Column(name = "sale_id")
    private Long saleId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;
}

