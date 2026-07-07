package com.silverline.erp.module.repair.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.dto.PagedResponse;
import com.silverline.erp.domain.pos.SaleService;
import com.silverline.erp.domain.repair.RepairJob;
import com.silverline.erp.module.repair.dto.RepairJobRequestDTO;
import com.silverline.erp.module.repair.dto.SaleServiceRequestDTO;
import com.silverline.erp.module.repair.service.DtvService;
import com.silverline.erp.module.repair.service.RepairService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Tag(name = "Repair Center Services", description = "APIs for tracking device repair jobs, logging diagnostic notes, allocating technicians, and processing service payments")
public class RepairController {

    private final RepairService repairService;
    private final DtvService dtvService;

    @Operation(summary = "Log new repair job", description = "Registers a new customer device repair job entry in PENDING status")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Repair job logged successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payload or validation error")
    @PostMapping("/repairs")
    public ResponseEntity<RepairJob> logRepairJob(@Valid @RequestBody RepairJobRequestDTO requestDTO) {
        return ResponseEntity.ok(repairService.logRepairJob(requestDTO));
    }

    @Operation(summary = "Get all repairs", description = "Retrieves a paginated list of all repair jobs in the system with optional branch filtering")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Repairs list retrieved successfully")
    @GetMapping("/repairs")
    public ResponseEntity<ApiResponse<PagedResponse<RepairJob>>> getRepairs(
            @RequestParam(required = false) Long branchId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<RepairJob> pageInfo = repairService.getAllRepairs(pageable);
        return ResponseEntity.ok(ApiResponse.success("Repairs retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Search repairs", description = "Performs keyword searches on repairs matching customer phone, device name, or job codes")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Repairs list retrieved successfully")
    @GetMapping("/repairs/search")
    public ResponseEntity<List<Map<String, Object>>> searchRepairs(@RequestParam String query) {
        return ResponseEntity.ok(repairService.searchRepairs(query));
    }

    @Operation(summary = "Create DTV service request", description = "Logs a Dialog TV connection/service checkout request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "DTV service created successfully")
    @PostMapping("/dtv")
    public ResponseEntity<SaleService> createDtvService(@Valid @RequestBody SaleServiceRequestDTO requestDTO) {
        return ResponseEntity.ok(dtvService.requestDtvService(requestDTO));
    }

    @Operation(summary = "Get DTV services", description = "Lists all DTV requests, with optional filtering by assigned technician ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "DTV services list retrieved successfully")
    @GetMapping("/dtv")
    public ResponseEntity<List<SaleService>> getDtvServices(@RequestParam(required = false) Long technicianId) {
        if (technicianId != null) {
            return ResponseEntity.ok(dtvService.getDtvServicesByTechnician(technicianId));
        }
        return ResponseEntity.ok(dtvService.getAllDtvServices());
    }

    @Operation(summary = "Update DTV status", description = "Updates status of a DTV connection request, recording assigned technicians or collections balance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "DTV service status updated successfully")
    @PutMapping("/dtv/{id}/status")
    public ResponseEntity<SaleService> updateDtvStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        String status = (String) payload.get("status");
        Long technicianId = null;
        if (payload.containsKey("technicianId") && payload.get("technicianId") != null) {
            technicianId = Long.valueOf(payload.get("technicianId").toString());
        }

        BigDecimal balanceCollected = null;
        if (payload.containsKey("balanceCollected") && payload.get("balanceCollected") != null) {
            balanceCollected = new BigDecimal(payload.get("balanceCollected").toString());
        }

        String additionalItems = null;
        if (payload.containsKey("additionalItems")) {
            additionalItems = (String) payload.get("additionalItems");
        }

        return ResponseEntity.ok(dtvService.updateDtvStatus(id, status, technicianId, balanceCollected, additionalItems));
    }

    @Operation(summary = "Update repair job status", description = "Updates status of a repair job, recording assigned technician ID or diagnosis notes")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Repair job status updated successfully")
    @PutMapping("/repairs/{id}/status")
    public ResponseEntity<RepairJob> updateRepairStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        String status = (String) payload.get("status");
        Long technicianId = null;
        if (payload.containsKey("technicianId") && payload.get("technicianId") != null) {
            technicianId = Long.valueOf(payload.get("technicianId").toString());
        }
        String notes = (String) payload.get("notes");
        return ResponseEntity.ok(repairService.updateRepairStatus(id, status, technicianId, notes));
    }

    @Operation(summary = "Request final cost approval", description = "Technician requests finalized price evaluation from manager before closing job")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Finalization request logged successfully")
    @PutMapping("/repairs/{id}/request-finalize")
    public ResponseEntity<RepairJob> requestFinalizeCost(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        Long managerId = Long.valueOf(payload.get("managerId").toString());
        BigDecimal estimatedCost = BigDecimal.ZERO;
        if (payload.containsKey("estimatedCost") && payload.get("estimatedCost") != null && !payload.get("estimatedCost").toString().isEmpty()) {
            estimatedCost = new BigDecimal(payload.get("estimatedCost").toString());
        }
        String costNote = (String) payload.get("costNote");

        return ResponseEntity.ok(repairService.requestFinalizeCost(id, managerId, estimatedCost, costNote));
    }

    @Operation(summary = "Finalize repair cost", description = "Manager reviews and finalizes the repair cost, transitioning state to COMPLETED")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Repair cost finalized successfully")
    @PutMapping("/repairs/{id}/finalize")
    public ResponseEntity<RepairJob> finalizeRepairCost(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        BigDecimal finalCost = payload.get("finalCost") != null
                ? new BigDecimal(payload.get("finalCost").toString()) : BigDecimal.ZERO;
        Long managerId = payload.get("managerId") != null
                ? Long.valueOf(payload.get("managerId").toString()) : null;
        return ResponseEntity.ok(repairService.finalizeRepairCost(id, finalCost, managerId));
    }

    @Operation(summary = "Mark repair job as paid", description = "Records customer checkout payment details and marks the job as DELIVERED/PAID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Repair job billed and closed successfully")
    @PutMapping("/repairs/{id}/pay")
    public ResponseEntity<RepairJob> markRepairPaid(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());
        String paymentMethod = payload.containsKey("paymentMethod") ? (String) payload.get("paymentMethod") : "CASH";
        Long receivedBy = payload.containsKey("receivedBy") && payload.get("receivedBy") != null
                ? Long.valueOf(payload.get("receivedBy").toString()) : null;
        return ResponseEntity.ok(repairService.markRepairPaid(id, amount, paymentMethod, receivedBy));
    }
}

