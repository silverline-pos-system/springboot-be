package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;

import com.silverline.erp.module.inventory.dto.DamagedProductDTO;
import com.silverline.erp.module.inventory.service.DamageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/damages")
@RequiredArgsConstructor
public class DamageController {

    private final DamageService damageService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllDamages(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long productId) {
        List<DamagedProductDTO> damages = damageService.getAllDamages(branchId, productId);
        return ResponseEntity.ok(ApiResponse.success("Damages retrieved successfully", damages));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createDamage(@Valid @RequestBody DamagedProductDTO damageDTO) {
        DamagedProductDTO created = damageService.createDamage(damageDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Damage entry created successfully", created));
    }
}



