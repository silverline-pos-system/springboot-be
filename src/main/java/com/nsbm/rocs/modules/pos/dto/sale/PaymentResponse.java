package com.nsbm.rocs.modules.pos.dto.sale;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
public class PaymentResponse {

    private Long paymentId;
    private String paymentType;
    private BigDecimal amount;
    private String referenceNo;
    private String cardLast4;
    private LocalDateTime createdAt;

}
