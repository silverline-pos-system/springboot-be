package com.nsbm.rocs.modules.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerCustomerDTO {
    private Long id;
    private String name;
    private String phone;
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

