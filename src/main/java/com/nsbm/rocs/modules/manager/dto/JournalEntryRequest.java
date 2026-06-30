package com.nsbm.rocs.modules.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryRequest {
    private String date;
    private String description;
    private List<LineRequest> lines;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineRequest {
        private String account;
        private BigDecimal dr;
        private BigDecimal cr;
    }
}


