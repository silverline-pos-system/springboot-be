package com.silverline.erp.domain.pos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_items", indexes = {
    @Index(name = "idx_sale_item_sale_product", columnList = "sale_id, product_id")
})
@Setter
@Getter
@NoArgsConstructor
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_item_id")
    private Long saleItemId;

    @Column(name = "sale_id")
    private Long saleId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "serial_id")
    private Long serialId; // For IMEI items

    @Column(precision = 12, scale = 3)
    private BigDecimal qty;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal discount;


    @Column(precision = 10, scale = 2)
    private BigDecimal total;


    @Override
    public String toString() {
        return "SaleItem{" +
                "saleItemId=" + saleItemId +
                ", productId=" + productId +
                ", qty=" + qty +
                ", total=" + total +
                '}';
    }
}