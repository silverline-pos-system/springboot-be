package com.silverline.erp.module.manager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerCustomerDTO {
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;
    private String tier;          // Silver, Gold, Platinum
    private Integer points;
    private String availablePoints; // Formatted or calculated
    private String totalSpend;    // Formatted currency
    private String lastPurchase;  // Formatted date relative
    private Integer visitCount;
    private String status;        // Active, Inactive
    private String address;
    private String city;
    private String dateOfBirth;
}

