package com.nsbm.rocs.modules.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispatchUpdateRequestDTO {

    private LocalDate dispatchDate;
    private String invoiceNo;
    private LocalDate invoiceDate;
    private List<DispatchCreateRequestDTO.DispatchItemCreateDTO> items;
}

