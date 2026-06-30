package com.nsbm.rocs.entity.pos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "sales_return_items")
@Getter
@Setter
@NoArgsConstructor
public class SalesReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_item_id")
    private Long returnItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private SalesReturn salesReturn;

    @Column(name = "sale_item_id", nullable = false)
    private Long saleItemId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(precision = 15, scale = 3, nullable = false)
    private BigDecimal qty;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal total;

    @PrePersist
    @PreUpdate
    protected void calculateTotal() {
        if (qty != null && unitPrice != null) {
            total = qty.multiply(unitPrice);
        }
    }
}

