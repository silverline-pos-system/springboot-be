package com.silverline.erp.module.pos.dto.sale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "Payment type is required")
    private String paymentType; // CASH, CARD, QR, BANK_TRANSFER

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String referenceNo;
    private String cardLast4;
    private String bankName; // Added field

    @Override
    public String toString() {
        return "PaymentRequest{" +
                "paymentType='" + paymentType + '\'' +
                ", amount=" + amount +
                ", referenceNo='" + referenceNo + '\'' +
                '}';
    }
}
