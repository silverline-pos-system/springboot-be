package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.dto.PagedResponse;
import com.silverline.erp.module.inventory.dto.SupplierRequestDTO;
import com.silverline.erp.module.inventory.dto.SupplierResponseDTO;
import com.silverline.erp.module.inventory.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/inventory/suppliers", "/api/inventory/suppliers"})
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SupplierResponseDTO>>> getAllSuppliers(
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Fetching all suppliers with pageable: {}", pageable);
        Page<SupplierResponseDTO> pageInfo = supplierService.getAllSuppliers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Suppliers retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponseDTO>> getSupplierById(@PathVariable Long id) {
        log.info("Fetching supplier ID: {}", id);
        SupplierResponseDTO supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(ApiResponse.success("Supplier retrieved successfully", supplier));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierResponseDTO>> createSupplier(@Valid @RequestBody SupplierRequestDTO requestDTO) {
        log.info("Creating supplier: {}", requestDTO.getName());
        // Ensure collections are not null
        if (requestDTO.getContacts() == null) requestDTO.setContacts(new ArrayList<>());
        if (requestDTO.getBranches() == null) requestDTO.setBranches(new ArrayList<>());

        SupplierResponseDTO createdSupplier = supplierService.createSupplier(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Supplier created successfully", createdSupplier));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponseDTO>> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequestDTO requestDTO) {
        log.info("Updating supplier ID: {}", id);
        if (requestDTO.getContacts() == null) requestDTO.setContacts(new ArrayList<>());
        if (requestDTO.getBranches() == null) requestDTO.setBranches(new ArrayList<>());

        SupplierResponseDTO updatedSupplier = supplierService.updateSupplier(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Supplier updated successfully", updatedSupplier));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable Long id) {
        log.info("Deleting supplier ID: {}", id);
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponse.success("Supplier deleted successfully"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<SupplierResponseDTO>>> getActiveSuppliers() {
        log.info("Fetching active suppliers");
        List<SupplierResponseDTO> suppliers = supplierService.getActiveSuppliers();
        return ResponseEntity.ok(ApiResponse.success("Active suppliers retrieved successfully", suppliers));
    }
}
