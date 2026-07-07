package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.inventory.dto.DamagedProductDTO;
import com.silverline.erp.module.inventory.service.DamageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/inventory/damages", "/api/inventory/damages"})
@RequiredArgsConstructor
@Tag(name = "Damaged Items Logging", description = "APIs for registering damaged products, deducting them from available branch stocks, and auditing shrinkage reasons")
public class DamageController {

    private final DamageService damageService;

    @Operation(summary = "Get all damaged logs", description = "Retrieves a list of all recorded damaged items entries, with optional filters for branch location and product ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Damaged logs list retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllDamages(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long productId) {
        List<DamagedProductDTO> damages = damageService.getAllDamages(branchId, productId);
        return ResponseEntity.ok(ApiResponse.success("Damages retrieved successfully", damages));
    }

    @Operation(summary = "Log damaged items", description = "Creates a new damage record log, automatically decrementing available stock counts in the respective branch location")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Damaged product logged and deducted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload or insufficient available inventory to deduct damages")
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createDamage(@Valid @RequestBody DamagedProductDTO damageDTO) {
        DamagedProductDTO created = damageService.createDamage(damageDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Damage entry created successfully", created));
    }
}

