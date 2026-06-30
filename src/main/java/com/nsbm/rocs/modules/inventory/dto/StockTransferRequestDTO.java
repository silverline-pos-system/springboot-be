package com.nsbm.rocs.modules.inventory.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class StockTransferRequestDTO {

    // Getters and Setters
    private Long fromBranch;
    private Long toBranch;
    private Long productId;
    private Long batchId;
    private Integer quantity;
    private LocalDate transferDate;
    private String remarks;
    private String transferStatus;
    // Add the corresponding setter method
    // Add the missing getter method
    private Long requestedBy; // Assuming requestedBy is of type Long, add this line

    private Long fromBranchId;  // was: fromBranch
    private Long toBranchId;    // was: toBranch

}

