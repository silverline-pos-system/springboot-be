package com.nsbm.rocs.modules.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponseDTO {
    private Long supplierId;
    private String code;
    private String name;
    private String companyName;
    private String contactPerson;
    private String phone;
    private String mobile;
    private String email;
    private String website;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String country;
    private String taxId;
    private Integer creditDays;
    private BigDecimal creditLimit;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;

    private List<SupplierContactDTO> contacts;
    private List<SupplierBranchDTO> branches;
}


