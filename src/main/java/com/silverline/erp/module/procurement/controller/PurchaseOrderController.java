package com.silverline.erp.module.procurement.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.dto.PagedResponse;
import com.silverline.erp.domain.procurement.PurchaseOrder;
import com.silverline.erp.module.inventory.dto.ProcessPORequest;
import com.silverline.erp.module.procurement.dto.PurchaseOrderDTO;
import com.silverline.erp.module.procurement.dto.PurchaseOrderResponse;
import com.silverline.erp.module.procurement.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Procurement Purchase Orders", description = "APIs for creating, approving, listing, and processing payment receipts for purchasing vendor supplies")
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    @Operation(summary = "Create a purchase order", description = "Generates a new draft purchase order detailing supplier target, item SKUs, costs, and requested delivery branch")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Purchase order created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error during database save")
    @PostMapping
    public ResponseEntity<?> createPurchaseOrder(@Valid @RequestBody PurchaseOrderDTO dto) {
        try {
            PurchaseOrder po = poService.createPurchaseOrder(dto);
            return ResponseEntity.ok(po);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating PO: " + e.getMessage());
        }
    }

    @Operation(summary = "Get all purchase orders", description = "Retrieves a paginated list of all purchase orders in the system")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Purchase orders list retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PurchaseOrderResponse>>> getAllPurchaseOrders(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PurchaseOrderResponse> pageInfo = poService.getAllPurchaseOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success("Purchase orders retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Get pending manager approvals", description = "Lists purchase orders that are in PENDING status, awaiting review and approval by a manager")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending approvals list retrieved successfully")
    @GetMapping("/pending")
    public ResponseEntity<List<PurchaseOrderResponse>> getManagerApprovals() {
        return ResponseEntity.ok(poService.getManagerApprovals());
    }

    @Operation(summary = "Get purchase orders by status", description = "Retrieves purchase orders matching a specific status code (e.g. APPROVED, RECIEVED, PAID)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Filtered purchase orders list retrieved successfully")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PurchaseOrderResponse>> getPurchaseOrdersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(poService.getPurchaseOrdersByStatus(status));
    }

    @Operation(summary = "Process purchase order payment/receipt", description = "Updates status of a purchase order, logging paid invoice numbers, total payment values, and receipt logs")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Purchase order processed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error processing PO payment or updating stocks")
    @PostMapping("/{poId}/process")
    public ResponseEntity<?> processPOPayment(@PathVariable Long poId, @Valid @RequestBody ProcessPORequest request) {
        try {
            PurchaseOrder po = poService.processPO(poId, request);
            return ResponseEntity.ok(po);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing PO: " + e.getMessage());
        }
    }

    @Operation(summary = "Get purchase order items list", description = "Retrieves items, quantities, and cost values included in a purchase order")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Purchase order items list retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Purchase order record not found")
    @GetMapping("/{poId}/items")
    public ResponseEntity<?> getPurchaseOrderItems(@PathVariable Long poId) {
        try {
            return ResponseEntity.ok(poService.getPurchaseOrderItems(poId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching PO items: " + e.getMessage());
        }
    }

    @Operation(summary = "Get purchase order payments history", description = "Retrieves a list of installment payment logs associated with a purchase order")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payments history list retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Purchase order record not found")
    @GetMapping("/{poId}/payments")
    public ResponseEntity<?> getPurchaseOrderPayments(@PathVariable Long poId) {
        try {
            return ResponseEntity.ok(poService.getPurchaseOrderPayments(poId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching PO payments: " + e.getMessage());
        }
    }
}

