package com.silverline.erp.module.repair.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.dto.PagedResponse;
import com.silverline.erp.domain.service.RepairJob;
import com.silverline.erp.domain.service.SaleService;
import com.silverline.erp.module.repair.dto.RepairJobRequestDTO;
import com.silverline.erp.module.repair.dto.SaleServiceRequestDTO;
import com.silverline.erp.module.repair.service.DtvService;
import com.silverline.erp.module.repair.service.RepairService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class RepairController {

    private final RepairService repairService;
    private final DtvService dtvService;

    @PostMapping("/repairs")
    public ResponseEntity<RepairJob> logRepairJob(@Valid @RequestBody RepairJobRequestDTO requestDTO) {
        return ResponseEntity.ok(repairService.logRepairJob(requestDTO));
    }

    @GetMapping("/repairs")
    public ResponseEntity<ApiResponse<PagedResponse<RepairJob>>> getRepairs(
            @RequestParam(required = false) Long branchId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<RepairJob> pageInfo = repairService.getAllRepairs(pageable);
        return ResponseEntity.ok(ApiResponse.success("Repairs retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @GetMapping("/repairs/search")
    public ResponseEntity<List<Map<String, Object>>> searchRepairs(@RequestParam String query) {
        return ResponseEntity.ok(repairService.searchRepairs(query));
    }

    @PostMapping("/dtv")
    public ResponseEntity<SaleService> createDtvService(@Valid @RequestBody SaleServiceRequestDTO requestDTO) {
        return ResponseEntity.ok(dtvService.requestDtvService(requestDTO));
    }

    @GetMapping("/dtv")
    public ResponseEntity<List<SaleService>> getDtvServices(@RequestParam(required = false) Long technicianId) {
        if (technicianId != null) {
            return ResponseEntity.ok(dtvService.getDtvServicesByTechnician(technicianId));
        }
        return ResponseEntity.ok(dtvService.getAllDtvServices());
    }

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
