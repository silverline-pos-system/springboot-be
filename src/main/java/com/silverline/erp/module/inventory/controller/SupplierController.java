package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.dto.PagedResponse;
import com.silverline.erp.module.inventory.dto.SupplierRequestDTO;
import com.silverline.erp.module.inventory.dto.SupplierResponseDTO;
import com.silverline.erp.module.inventory.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Inventory Suppliers", description = "APIs for registering, modifying, listing, and tracking supplier companies, contact persons, and target branches")
public class SupplierController {

    private final SupplierService supplierService;

    @Operation(summary = "Get all suppliers", description = "Retrieves a paginated list of all suppliers in the database")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Suppliers list retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SupplierResponseDTO>>> getAllSuppliers(
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Fetching all suppliers with pageable: {}", pageable);
        Page<SupplierResponseDTO> pageInfo = supplierService.getAllSuppliers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Suppliers retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Get supplier by ID", description = "Looks up supplier profile details, contacts, and branch permissions by supplier ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Supplier profile retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Supplier not found")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponseDTO>> getSupplierById(@PathVariable Long id) {
        log.info("Fetching supplier ID: {}", id);
        SupplierResponseDTO supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(ApiResponse.success("Supplier retrieved successfully", supplier));
    }

    @Operation(summary = "Register a supplier", description = "Creates a new supplier vendor profile in the database layout")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Supplier profile registered successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload or schema validation error")
    @PostMapping
    public ResponseEntity<ApiResponse<SupplierResponseDTO>> createSupplier(@Valid @RequestBody SupplierRequestDTO requestDTO) {
        log.info("Creating supplier: {}", requestDTO.getName());
        if (requestDTO.getContacts() == null) requestDTO.setContacts(new ArrayList<>());
        if (requestDTO.getBranches() == null) requestDTO.setBranches(new ArrayList<>());

        SupplierResponseDTO createdSupplier = supplierService.createSupplier(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Supplier created successfully", createdSupplier));
    }

    @Operation(summary = "Update supplier details", description = "Modifies supplier details (name, code, contacts list, branch permissions) for a supplier ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Supplier details updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Supplier not found")
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

    @Operation(summary = "Delete supplier record", description = "Removes a supplier record from the registry database")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Supplier profile deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Supplier not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable Long id) {
        log.info("Deleting supplier ID: {}", id);
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponse.success("Supplier deleted successfully"));
    }

    @Operation(summary = "Get active suppliers", description = "Lists active suppliers in the system database (frequently used for purchase order inputs)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active suppliers list retrieved successfully")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<SupplierResponseDTO>>> getActiveSuppliers() {
        log.info("Fetching active suppliers");
        List<SupplierResponseDTO> suppliers = supplierService.getActiveSuppliers();
        return ResponseEntity.ok(ApiResponse.success("Active suppliers retrieved successfully", suppliers));
    }
}

