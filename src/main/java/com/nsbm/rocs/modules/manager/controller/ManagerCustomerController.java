package com.nsbm.rocs.modules.manager.controller;

import com.nsbm.rocs.modules.manager.dto.LoyaltyStatsDTO;
import com.nsbm.rocs.modules.manager.dto.ManagerCustomerDTO;
import com.nsbm.rocs.modules.manager.service.ManagerCustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/manager/customers")
@RequiredArgsConstructor
public class ManagerCustomerController {

    private final ManagerCustomerService customerService;

    @GetMapping
    public ResponseEntity<List<ManagerCustomerDTO>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
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
    public ResponseEntity<?> updateCustomer(@PathVariable Long id, @RequestBody ManagerCustomerDTO dto) {
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
    public ResponseEntity<List<com.nsbm.rocs.modules.manager.dto.ManagerSaleDTO>> getCustomerSales(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerSales(id));
    }
}

