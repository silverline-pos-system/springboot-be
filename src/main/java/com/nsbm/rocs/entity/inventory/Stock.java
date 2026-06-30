package com.nsbm.rocs.entity.inventory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "InventoryStock")
@Table(name = "stock", uniqueConstraints = {
    @UniqueConstraint(name = "unique_branch_product", columnNames = {"branch_id", "product_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long stockId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", precision = 15, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "reserved_qty", precision = 15, scale = 3)
    private BigDecimal reservedQty = BigDecimal.ZERO;

    @Column(name = "available_qty", precision = 15, scale = 3)
    private BigDecimal availableQty = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}

