package com.silverline.erp.domain.procurement;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_item_id")
    private Long poItemId;

    @Column(name = "po_id", nullable = false)
    private Long poId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "qty_ordered", precision = 15, scale = 3, nullable = false)
    private BigDecimal qtyOrdered;

    @Column(name = "qty_dispatched", precision = 15, scale = 3)
    private BigDecimal qtyDispatched = BigDecimal.ZERO;

    @Column(name = "unit_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal unitPrice;


    @Column(precision = 15, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "selling_price", precision = 15, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "mrp", precision = 15, scale = 2)
    private BigDecimal mrp;

    @Column(precision = 15, scale = 2)
    private BigDecimal total;
}

