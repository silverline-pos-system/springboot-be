package com.nsbm.rocs.modules.inventory.controller;

import com.nsbm.rocs.shared.response.ApiResponse;

import com.nsbm.rocs.modules.inventory.dto.UnitDTO;
import com.nsbm.rocs.modules.inventory.service.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllUnits() {
        List<UnitDTO> units = unitService.getAllUnits();
        return ResponseEntity.ok(ApiResponse.success("Units retrieved successfully", units));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getUnitById(@PathVariable Long id) {
        UnitDTO unit = unitService.getUnitById(id);
        return ResponseEntity.ok(ApiResponse.success("Unit retrieved successfully", unit));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createUnit(@Valid @RequestBody UnitDTO unitDTO) {
        UnitDTO createdUnit = unitService.createUnit(unitDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Unit created successfully", createdUnit));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateUnit(
            @PathVariable Long id,
            @Valid @RequestBody UnitDTO unitDTO) {
        UnitDTO updatedUnit = unitService.updateUnit(id, unitDTO);
        return ResponseEntity.ok(ApiResponse.success("Unit updated successfully", updatedUnit));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteUnit(@PathVariable Long id) {
        unitService.deleteUnit(id);
        return ResponseEntity.ok(ApiResponse.success("Unit deleted successfully"));
    }
}


