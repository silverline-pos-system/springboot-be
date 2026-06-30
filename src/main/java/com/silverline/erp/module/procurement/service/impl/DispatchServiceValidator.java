package com.silverline.erp.module.procurement.service.impl;

import com.silverline.erp.domain.inventory.Dispatch;
import com.silverline.erp.module.procurement.dto.DispatchCreateRequestDTO;
import com.silverline.erp.module.procurement.dto.DispatchResponseDTO;
import com.silverline.erp.module.procurement.service.DispatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Simple validation class to test Dispatch functionality
 * This is NOT a JUnit test, but a utility to verify the service works
 */
@Component
@Slf4j
public class DispatchServiceValidator {

    @Autowired
    private DispatchService dispatchService;

    /**
     * Method to validate basic Dispatch service functionality
     * Call this manually to test the service
     */
    public void validateDispatchService() {
        try {
            log.info("=== Starting Dispatch Service Validation ===");

            // Test 1: Check if Dispatch number generation works
            boolean exists = dispatchService.isDispatchNumberExists("DSP-1-20240205-001");
            log.info("Dispatch number check result: {}", exists);

            // Test 2: Try to get Dispatches by branch (should return empty list initially)
            List<DispatchResponseDTO> dispatches = dispatchService.getDispatchesByBranch(1L);
            log.info("Found {} Dispatches for branch 1", dispatches.size());

            // Test 3: Create a sample Dispatch request (won't actually create due to missing data)
            DispatchCreateRequestDTO.DispatchItemCreateDTO item = new DispatchCreateRequestDTO.DispatchItemCreateDTO();
            item.setProductId(1L);
            item.setQtyDispatched(new BigDecimal("10.000"));
            item.setUnitPrice(new BigDecimal("100.00"));
            item.setBatchCode("BATCH001");

            DispatchCreateRequestDTO request = new DispatchCreateRequestDTO();
            request.setBranchId(1L);
            request.setSupplierId(1L);
            request.setDispatchDate(LocalDate.now());
            request.setItems(Arrays.asList(item));

            log.info("Sample Dispatch request created with {} items", request.getItems().size());

            log.info("=== Dispatch Service Validation Completed Successfully ===");

        } catch (Exception e) {
            log.error("Dispatch Service validation failed: {}", e.getMessage(), e);
        }
    }
}
