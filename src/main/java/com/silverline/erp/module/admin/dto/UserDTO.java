package com.silverline.erp.module.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long userId;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    private String employeeId;
    private String role;
    private Long branchId;
    private String branchName;
    private String status;
    private String password; // Only for creation
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
