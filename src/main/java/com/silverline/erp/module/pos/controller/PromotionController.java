package com.silverline.erp.module.pos.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.security.SecurityUtils;
import com.silverline.erp.module.pos.dto.PromotionDTO;
import com.silverline.erp.module.pos.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/promotions", "/api/inventory/promotions"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','SUPER_ADMIN')")
@Tag(name = "Promotions", description = "Manage promotion/offer campaigns (manager per branch, admin global)")
public class PromotionController {

    private final PromotionService promotionService;

    @Operation(summary = "List promotions", description = "All promotions, or (with branchId) that branch plus all-branch ones")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PromotionDTO>>> list(@RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(ApiResponse.success("Promotions fetched", promotionService.list(branchId)));
    }

    @Operation(summary = "Get a promotion")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionDTO>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Promotion fetched", promotionService.get(id)));
    }

    @Operation(summary = "Create a promotion")
    @PostMapping
    public ResponseEntity<ApiResponse<PromotionDTO>> create(@RequestBody PromotionDTO dto) {
        PromotionDTO created = promotionService.create(dto, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Promotion created", created));
    }

    @Operation(summary = "Update a promotion")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionDTO>> update(@PathVariable Long id, @RequestBody PromotionDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Promotion updated", promotionService.update(id, dto)));
    }

    @Operation(summary = "Toggle a promotion active/inactive")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<PromotionDTO>> toggle(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", Boolean.TRUE);
        return ResponseEntity.ok(ApiResponse.success("Promotion updated", promotionService.setActive(id, active)));
    }

    @Operation(summary = "Delete a promotion")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Promotion deleted"));
    }
}
