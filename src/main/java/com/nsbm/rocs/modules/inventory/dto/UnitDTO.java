package com.nsbm.rocs.modules.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitDTO {
    private Long unitId;

    @NotBlank(message = "Name is required")
    private String name;

    private String symbol;
}


