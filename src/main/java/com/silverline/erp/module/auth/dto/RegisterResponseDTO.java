package com.silverline.erp.module.auth.dto;

import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterResponseDTO {
    private Long userId;
    private String email;
    private String fullName;
    private Role role;
    private AccountStatus accountStatus;
    private String message;

    public RegisterResponseDTO(String message) {
        this.message = message;
    }
}

