package com.nsbm.rocs.modules.pos.controller;

import com.nsbm.rocs.shared.response.ApiResponse;
import com.nsbm.rocs.modules.pos.dto.customer.CreateCustomerRequest;
import com.nsbm.rocs.modules.pos.service.PosService;
import com.nsbm.rocs.entity.pos.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pos/customers")
@CrossOrigin
public class PosCustomerController {

    private final PosService posService;

    @Autowired
    public PosCustomerController(PosService posService) {
        this.posService = posService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Customer>> createCustomer(@RequestBody CreateCustomerRequest request) {
        try {
            Customer customer = posService.createCustomer(request);
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
            posService.updateLoyaltyPoints(id, payload.get("points"));
            return ResponseEntity.ok(ApiResponse.success("Loyalty points updated", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<java.util.List<Customer>>> searchCustomers(@RequestParam String query) {
        try {
            java.util.List<Customer> customers = posService.searchCustomers(query);
            return ResponseEntity.ok(ApiResponse.success("Customers found", customers));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<Customer>> getCustomerByCode(@PathVariable String code) {
        Customer customer = posService.getCustomerByCode(code);
        if (customer != null) {
            return ResponseEntity.ok(ApiResponse.success("Customer found", customer));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Customer not found"));
        }
    }
}


