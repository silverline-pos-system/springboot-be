package com.silverline.erp.module.procurement.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.dto.PagedResponse;
import com.silverline.erp.domain.procurement.PurchaseOrder;
import com.silverline.erp.module.inventory.dto.ProcessPORequest;
import com.silverline.erp.module.procurement.dto.PurchaseOrderDTO;
import com.silverline.erp.module.procurement.dto.PurchaseOrderResponse;
import com.silverline.erp.module.procurement.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/procurement/po", "/api/inventory/po"})
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    @PostMapping
    public ResponseEntity<?> createPurchaseOrder(@Valid @RequestBody PurchaseOrderDTO dto) {
        try {
            PurchaseOrder po = poService.createPurchaseOrder(dto);
            return ResponseEntity.ok(po);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating PO: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PurchaseOrderResponse>>> getAllPurchaseOrders(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PurchaseOrderResponse> pageInfo = poService.getAllPurchaseOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success("Purchase orders retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PurchaseOrderResponse>> getManagerApprovals() {
        return ResponseEntity.ok(poService.getManagerApprovals());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PurchaseOrderResponse>> getPurchaseOrdersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(poService.getPurchaseOrdersByStatus(status));
    }

    @PostMapping("/{poId}/process")
    public ResponseEntity<?> processPOPayment(@PathVariable Long poId, @Valid @RequestBody ProcessPORequest request) {
        try {
            PurchaseOrder po = poService.processPO(poId, request);
            return ResponseEntity.ok(po);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing PO: " + e.getMessage());
        }
    }

    @GetMapping("/{poId}/items")
    public ResponseEntity<?> getPurchaseOrderItems(@PathVariable Long poId) {
        try {
            return ResponseEntity.ok(poService.getPurchaseOrderItems(poId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching PO items: " + e.getMessage());
        }
    }

    @GetMapping("/{poId}/payments")
    public ResponseEntity<?> getPurchaseOrderPayments(@PathVariable Long poId) {
        try {
            return ResponseEntity.ok(poService.getPurchaseOrderPayments(poId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching PO payments: " + e.getMessage());
        }
    }
}

