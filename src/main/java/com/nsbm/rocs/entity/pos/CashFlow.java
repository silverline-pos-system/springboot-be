package com.nsbm.rocs.entity.pos;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_flows")
@Data
public class CashFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flow_id")
    private Long flowId;

    private BigDecimal amount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    // Map to 'type' column in database
    @Column(name = "type")
    private String type;

    private String reason;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "status")
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED
}