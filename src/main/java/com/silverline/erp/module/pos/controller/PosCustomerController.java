package com.silverline.erp.module.pos.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.pos.dto.customer.CreateCustomerRequest;
import com.silverline.erp.module.pos.service.LoyaltyService;
import com.silverline.erp.domain.pos.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pos/customers")
@RequiredArgsConstructor
@CrossOrigin
public class PosCustomerController {

    private final LoyaltyService loyaltyService;

    @PostMapping
    public ResponseEntity<ApiResponse<Customer>> createCustomer(@RequestBody CreateCustomerRequest request) {
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

    @PatchMapping("/{id}/loyalty")
    public ResponseEntity<ApiResponse<Void>> updateLoyaltyPoints(@PathVariable Long id, @RequestBody java.util.Map<String, Integer> payload) {
        try {
            loyaltyService.updateLoyaltyPoints(id, payload.get("points"));
            return ResponseEntity.ok(ApiResponse.success("Loyalty points updated", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<java.util.List<Customer>>> searchCustomers(@RequestParam String query) {
        try {
            java.util.List<Customer> customers = loyaltyService.searchCustomers(query);
            return ResponseEntity.ok(ApiResponse.success("Customers found", customers));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
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


