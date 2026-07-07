package com.silverline.erp.module.manager.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.dto.PagedResponse;
import com.silverline.erp.module.analytics.dto.LoyaltyStatsDTO;
import com.silverline.erp.module.manager.dto.ManagerCustomerDTO;
import com.silverline.erp.module.manager.dto.ManagerSaleDTO;
import com.silverline.erp.module.pos.service.CustomerService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/manager/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Base Management", description = "APIs for managers to view customer lists, adjust loyalty points, update details, edit tier threshold rules, and view history")
public class ManagerCustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Get all customers", description = "Retrieves a paginated list of all registered customers in the system")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customers list retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ManagerCustomerDTO>>> getAllCustomers(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ManagerCustomerDTO> pageInfo = customerService.getAllCustomers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Get loyalty stats", description = "Retrieves stats on customer distributions across tiers (Silver, Gold, Platinum)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Loyalty stats retrieved successfully")
    @GetMapping("/stats")
    public ResponseEntity<List<LoyaltyStatsDTO>> getLoyaltyStats() {
        return ResponseEntity.ok(customerService.getLoyaltyStats());
    }

    @Operation(summary = "Adjust customer loyalty points", description = "Manually adds/deducts points from a customer's loyalty profile, recording reasons")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Loyalty points adjusted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Customer not found")
    @PostMapping("/{id}/adjust-points")
    public ResponseEntity<?> adjustPoints(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Integer points = (Integer) payload.get("points");
        String reason = (String) payload.get("reason");

        customerService.addPoints(id, points, reason);
        return ResponseEntity.ok(Map.of("message", "Points adjusted successfully"));
    }

    @Operation(summary = "Update customer details", description = "Updates profile details (name, email, phone, tier) for a customer ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customer details updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Customer not found")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable Long id, @Valid @RequestBody ManagerCustomerDTO dto) {
        customerService.updateCustomer(id, dto);
        return ResponseEntity.ok(Map.of("message", "Customer updated successfully"));
    }

    @Operation(summary = "Get loyalty tier threshold rules", description = "Retrieves the minimum spend rules required for Gold and Platinum tiers")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tier rules retrieved successfully")
    @GetMapping("/active-tier-rules")
    public ResponseEntity<Map<String, Double>> getTierRules() {
        return ResponseEntity.ok(customerService.getTierRules());
    }

    @Operation(summary = "Update loyalty tier rules", description = "Updates the required minimum spending thresholds for various loyalty tiers")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tier rules updated successfully")
    @PostMapping("/active-tier-rules")
    public ResponseEntity<?> updateTierRules(@RequestBody Map<String, Double> rules) {
        customerService.updateTierRules(rules);
        return ResponseEntity.ok(Map.of("message", "Tier rules updated"));
    }

    @Operation(summary = "Get customer sales history", description = "Retrieves a historical list of checkout sale transactions completed by a customer")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customer sales history retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Customer not found")
    @GetMapping("/{id}/sales")
    public ResponseEntity<List<ManagerSaleDTO>> getCustomerSales(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerSales(id));
    }
}

