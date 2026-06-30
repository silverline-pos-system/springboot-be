package com.nsbm.rocs.modules.pos.dto.shift;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
public class CloseShiftRequest {

    @NotNull(message = "Closing cash is required")
    @PositiveOrZero(message = "Closing cash cannot be negative")
    private BigDecimal closingCash;

    private String notes; // Optional notes
    private String supervisorUsername;
    private String supervisorPassword;


    public CloseShiftRequest(BigDecimal closingCash, String notes) {
        this.closingCash = closingCash;
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "CloseShiftRequest{" +
                "closingCash=" + closingCash +
                ", notes='" + notes + '\'' +
                ", supervisorUsername='" + supervisorUsername + '\'' +
                '}';
    }
}
