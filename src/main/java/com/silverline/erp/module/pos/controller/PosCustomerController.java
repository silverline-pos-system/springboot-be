package com.silverline.erp.module.pos.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.domain.pos.Customer;
import com.silverline.erp.module.pos.dto.customer.CreateCustomerRequest;
import com.silverline.erp.module.pos.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pos/customers")
@RequiredArgsConstructor
@Tag(name = "POS Customer Inquiries", description = "APIs for cashiers to register new customer profiles at the POS terminal, search loyalty tiers, and add points")
public class PosCustomerController {

    private final LoyaltyService loyaltyService;

    @Operation(summary = "Register customer at checkout", description = "Creates a new customer profile directly from the POS checkout window")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Customer registered successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Duplicate phone/email or schema validation failure")
    @PostMapping
    public ResponseEntity<ApiResponse<Customer>> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        try {
            Customer customer = loyaltyService.createCustomer(request);
            return new ResponseEntity<>(
                    ApiResponse.success("Customer created successfully", customer),
                    HttpStatus.CREATED
            );
        } catch (Exception e) {
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Update customer loyalty points", description = "Increments customer loyalty points for a specific customer ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Loyalty points updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Customer not found")
    @PatchMapping("/{id}/loyalty")
    public ResponseEntity<ApiResponse<Void>> updateLoyaltyPoints(@PathVariable Long id, @RequestBody Map<String, Integer> payload) {
        try {
            loyaltyService.updateLoyaltyPoints(id, payload.get("points"));
            return ResponseEntity.ok(ApiResponse.success("Loyalty points updated", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Search customers by query", description = "Queries customers matching search keyword (checks phone, name, or code)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customer list retrieved successfully")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Customer>>> searchCustomers(@RequestParam String query) {
        try {
            List<Customer> customers = loyaltyService.searchCustomers(query);
            return ResponseEntity.ok(ApiResponse.success("Customers found", customers));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Get customer by barcode/code", description = "Retrieves a customer profile matching their unique customer code barcode")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customer profile retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Customer not found")
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<Customer>> getCustomerByCode(@PathVariable String code) {
        Customer customer = loyaltyService.getCustomerByCode(code);
        if (customer != null) {
            return ResponseEntity.ok(ApiResponse.success("Customer found", customer));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Customer not found"));
        }
    }
}

