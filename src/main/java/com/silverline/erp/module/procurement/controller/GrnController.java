package com.silverline.erp.module.procurement.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.security.SecurityUtils;
import com.silverline.erp.module.procurement.dto.*;
import com.silverline.erp.module.procurement.service.GrnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController("grnController")
@RequestMapping({"/api/v1/procurement/grn", "/api/inventory/grn"})
@RequiredArgsConstructor
@Validated
@Tag(name = "Goods Received Notes", description = "APIs for receiving supplier deliveries against POs into a branch, posting stock, and history")
public class GrnController {

    private final GrnService grnService;

    @Operation(summary = "Create a GRN", description = "Records a supplier delivery received into a branch as a draft GRN")
    @PostMapping
    public ResponseEntity<ApiResponse<GrnResponseDTO>> createGrn(@Valid @RequestBody GrnCreateRequestDTO request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        GrnResponseDTO result = grnService.createGrn(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("GRN created successfully", result));
    }

    @Operation(summary = "Get GRN by ID")
    @GetMapping("/{grnId}")
    public ResponseEntity<ApiResponse<GrnResponseDTO>> getGrnById(@PathVariable @NotNull Long grnId) {
        return ResponseEntity.ok(ApiResponse.success("GRN retrieved successfully", grnService.getGrnById(grnId)));
    }

    @Operation(summary = "Get GRNs by branch")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<List<GrnResponseDTO>>> getGrnsByBranch(@PathVariable @NotNull Long branchId) {
        return ResponseEntity.ok(ApiResponse.success("GRNs retrieved successfully", grnService.getGrnsByBranch(branchId)));
    }

    @Operation(summary = "Search GRNs by filter")
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<GrnResponseDTO>>> searchGrns(@RequestBody GrnFilterDTO filter) {
        return ResponseEntity.ok(ApiResponse.success("GRNs retrieved successfully", grnService.searchGrns(filter)));
    }

    @Operation(summary = "Post a GRN", description = "Confirms a draft GRN: updates branch stock, per-branch price and PO received quantities")
    @PutMapping("/{grnId}/post")
    public ResponseEntity<ApiResponse<GrnResponseDTO>> postGrn(@PathVariable @NotNull Long grnId) {
        Long postedBy = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("GRN posted successfully", grnService.postGrn(grnId, postedBy)));
    }

    @Operation(summary = "Update GRN payment status")
    @PutMapping("/{grnId}/payment-status")
    public ResponseEntity<ApiResponse<GrnResponseDTO>> updatePaymentStatus(
            @PathVariable @NotNull Long grnId, @RequestParam String paymentStatus) {
        return ResponseEntity.ok(ApiResponse.success("Payment status updated successfully",
                grnService.updatePaymentStatus(grnId, paymentStatus)));
    }

    @Operation(summary = "Cancel a draft GRN")
    @PutMapping("/{grnId}/cancel")
    public ResponseEntity<ApiResponse<GrnResponseDTO>> cancelGrn(
            @PathVariable @NotNull Long grnId, @RequestParam(required = false) String reason) {
        Long cancelledBy = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("GRN cancelled successfully",
                grnService.cancelGrn(grnId, cancelledBy, reason)));
    }

    @Operation(summary = "Delete a draft GRN")
    @DeleteMapping("/{grnId}")
    public ResponseEntity<ApiResponse<Void>> deleteGrn(@PathVariable @NotNull Long grnId) {
        grnService.deleteGrn(grnId);
        return ResponseEntity.ok(ApiResponse.success("GRN deleted successfully"));
    }

    @Operation(summary = "Get GRN items by product ID")
    @GetMapping("/product/{productId}/items")
    public ResponseEntity<ApiResponse<List<GrnItemDTO>>> getGrnItemsByProduct(
            @PathVariable @NotNull Long productId, @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(ApiResponse.success("GRN items retrieved successfully",
                grnService.getGrnItemsByProduct(productId, branchId)));
    }

    @Operation(summary = "Verify GRN number uniqueness")
    @GetMapping("/check-number/{grnNo}")
    public ResponseEntity<ApiResponse<Boolean>> checkGrnNumber(@PathVariable @NotNull String grnNo) {
        return ResponseEntity.ok(ApiResponse.success("GRN number check completed", grnService.isGrnNumberExists(grnNo)));
    }

    @Operation(summary = "Get GRNs by supplier")
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ApiResponse<List<GrnResponseDTO>>> getGrnsBySupplier(@PathVariable @NotNull Long supplierId) {
        GrnFilterDTO filter = new GrnFilterDTO();
        filter.setSupplierId(supplierId);
        return ResponseEntity.ok(ApiResponse.success("GRNs retrieved successfully", grnService.searchGrns(filter)));
    }

    @Operation(summary = "Get draft GRNs", description = "Lists GRNs awaiting posting, with optional branch filter")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<GrnResponseDTO>>> getPendingGrns(@RequestParam(required = false) Long branchId) {
        GrnFilterDTO filter = new GrnFilterDTO();
        filter.setStatus("DRAFT");
        if (branchId != null) {
            filter.setBranchId(branchId);
        }
        return ResponseEntity.ok(ApiResponse.success("Draft GRNs retrieved successfully", grnService.searchGrns(filter)));
    }
}
