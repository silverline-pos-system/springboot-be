package com.silverline.erp.domain.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "item_dispatch_lines")
@Getter
@Setter
@NoArgsConstructor
public class DispatchItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dispatch_line_id")
    private Long dispatchItemId;

    @Column(name = "dispatch_id", nullable = false)
    private Long dispatchId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "to_branch_id", nullable = false)
    private Long toBranchId;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "batch_code", length = 60)
    private String batchCode;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "qty_dispatched", precision = 15, scale = 3, nullable = false)
    private BigDecimal qtyDispatched;

    @Column(name = "unit_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "selling_price", precision = 15, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "mrp", precision = 15, scale = 2)
    private BigDecimal mrp;

    @Column(precision = 15, scale = 2)
    private BigDecimal total;

    @Column(name = "item_type", length = 20)
    private String itemType;

    @Column(name = "serial_no", length = 100)
    private String serialNo;
}
