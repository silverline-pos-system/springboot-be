package com.silverline.erp.module.auth.dto;

import com.silverline.erp.module.inventory.dto.BranchDTO;
import com.silverline.erp.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LogInResponseDTO {

    private Long userId;
    private String username;
    private String email;
    private String token;
    private Role role;
    private String redirectPath;
    private String message;
    private Long branchId;
    private String branchName;
    private java.util.List<com.silverline.erp.module.admin.dto.BranchDTO> branches;

    public LogInResponseDTO(String message) {
        this.message = message;
    }

    public LogInResponseDTO(Long userId, String username, String email, String token,
                            Role role, Long branchId, String branchName, String message) {

        this.userId = userId;
        this.username = username;
        this.email = email;
        this.token = token;
        this.role = role;
        this.branchId = branchId;
        this.branchName = branchName;
        this.redirectPath = determineRedirectPath(role);
        this.message = message;
    }

    public static String determineRedirectPath(Role role) {

        if (role == null) {
            return "/pending-approval";
        }

        return switch (role) {
            case SUPER_ADMIN -> "/admin";
            case MANAGER -> "/manager";
            case CASHIER, SUPERVISOR -> "/pos";
            case STORE_KEEPER -> "/inventory";
            case DTV_TECHNICIAN -> "/dtv-tech";
            case MOBILE_TECHNICIAN -> "/mobile-tech";
        };
    }
}
