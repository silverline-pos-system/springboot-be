package com.silverline.erp.module.manager.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.dto.PagedResponse;
import com.silverline.erp.module.analytics.dto.LoyaltyStatsDTO;
import com.silverline.erp.module.manager.dto.ManagerCustomerDTO;
import com.silverline.erp.module.manager.dto.ManagerSaleDTO;
import com.silverline.erp.module.pos.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/manager/customers")
@RequiredArgsConstructor
public class ManagerCustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ManagerCustomerDTO>>> getAllCustomers(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ManagerCustomerDTO> pageInfo = customerService.getAllCustomers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @GetMapping("/stats")
    public ResponseEntity<List<LoyaltyStatsDTO>> getLoyaltyStats() {
        return ResponseEntity.ok(customerService.getLoyaltyStats());
    }

    @PostMapping("/{id}/adjust-points")
    public ResponseEntity<?> adjustPoints(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Integer points = (Integer) payload.get("points");
        String reason = (String) payload.get("reason");

        customerService.addPoints(id, points, reason);
        return ResponseEntity.ok(Map.of("message", "Points adjusted successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable Long id, @Valid @RequestBody ManagerCustomerDTO dto) {
        customerService.updateCustomer(id, dto);
        return ResponseEntity.ok(Map.of("message", "Customer updated successfully"));
    }

    @GetMapping("/active-tier-rules")
    public ResponseEntity<Map<String, Double>> getTierRules() {
        return ResponseEntity.ok(customerService.getTierRules());
    }

    @PostMapping("/active-tier-rules")
    public ResponseEntity<?> updateTierRules(@RequestBody Map<String, Double> rules) {
        customerService.updateTierRules(rules);
        return ResponseEntity.ok(Map.of("message", "Tier rules updated"));
    }

    @GetMapping("/{id}/sales")
    public ResponseEntity<List<ManagerSaleDTO>> getCustomerSales(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerSales(id));
    }
}
