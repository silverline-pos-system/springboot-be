package com.nsbm.rocs.modules.manager.dto;

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

