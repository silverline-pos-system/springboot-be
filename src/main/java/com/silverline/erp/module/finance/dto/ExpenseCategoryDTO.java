package com.silverline.erp.module.finance.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExpenseCategoryDTO {
    private Long categoryId;
    private String name;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
}

