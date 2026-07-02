package com.silverline.erp.module.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class StockTransferRequestDTO {

    // Getters and Setters
    private Long fromBranch;
    private Long toBranch;

    @NotNull(message = "Product is required")
    private Long productId;

    private Long batchId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private LocalDate transferDate;
    private String remarks;
    private String transferStatus;
    // Add the corresponding setter method
    // Add the missing getter method
    private Long requestedBy; // Assuming requestedBy is of type Long, add this line

    @NotNull(message = "Origin branch is required")
    private Long fromBranchId;  // was: fromBranch

    @NotNull(message = "Destination branch is required")
    private Long toBranchId;    // was: toBranch

}

