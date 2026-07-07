package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.inventory.dto.UnitDTO;
import com.silverline.erp.module.inventory.service.UnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/inventory/units", "/api/inventory/units"})
@RequiredArgsConstructor
@Tag(name = "Product Units", description = "APIs for mapping, defining, editing, and deleting product measurement units (e.g. PCS, BOX, KG)")
public class UnitController {

    private final UnitService unitService;

    @Operation(summary = "Get all units", description = "Retrieves a list of all registered product units of measure")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Units list retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllUnits() {
        List<UnitDTO> units = unitService.getAllUnits();
        return ResponseEntity.ok(ApiResponse.success("Units retrieved successfully", units));
    }

    @Operation(summary = "Get unit by ID", description = "Retrieves product unit details by unit database ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Unit details retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Unit database record not found")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getUnitById(@PathVariable Long id) {
        UnitDTO unit = unitService.getUnitById(id);
        return ResponseEntity.ok(ApiResponse.success("Unit retrieved successfully", unit));
    }

    @Operation(summary = "Create a product unit", description = "Registers a new product unit of measure in the catalog")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Unit created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload or validation error")
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createUnit(@Valid @RequestBody UnitDTO unitDTO) {
        UnitDTO createdUnit = unitService.createUnit(unitDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Unit created successfully", createdUnit));
    }

    @Operation(summary = "Update unit details", description = "Modifies name or description details for an existing unit ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Unit details updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Unit database record not found")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateUnit(
            @PathVariable Long id,
            @Valid @RequestBody UnitDTO unitDTO) {
        UnitDTO updatedUnit = unitService.updateUnit(id, unitDTO);
        return ResponseEntity.ok(ApiResponse.success("Unit updated successfully", updatedUnit));
    }

    @Operation(summary = "Delete unit", description = "Removes a product unit record from the database")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Unit deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Unit database record not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteUnit(@PathVariable Long id) {
        unitService.deleteUnit(id);
        return ResponseEntity.ok(ApiResponse.success("Unit deleted successfully"));
    }
}

