package com.silverline.erp.domain.procurement;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * GRN line item. Goods are received into the GRN's branch (no per-line branch).
 */
@Entity
@Table(name = "grn_items")
@Getter
@Setter
@NoArgsConstructor
public class GrnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grn_item_id")
    private Long grnItemId;

    @Column(name = "grn_id", nullable = false)
    private Long grnId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "batch_code", length = 60)
    private String batchCode;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "qty_received", precision = 15, scale = 3, nullable = false)
    private BigDecimal qtyReceived;

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
