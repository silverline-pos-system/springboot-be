package com.nsbm.rocs.modules.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRequestDTO {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
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
    private Long createdBy;

    private List<SupplierContactDTO> contacts;
    private List<SupplierBranchDTO> branches;
}


