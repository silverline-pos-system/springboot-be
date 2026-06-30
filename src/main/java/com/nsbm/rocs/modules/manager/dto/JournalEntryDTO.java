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
public class JournalEntryDTO {
    private Long entryId;
    private String date;
    private String description;
    private List<JournalLineDTO> lines;
    private String createdAt;
    private String createdBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JournalLineDTO {
        private String account;
        private BigDecimal dr;
        private BigDecimal cr;
    }
}


