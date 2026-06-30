package com.nsbm.rocs.modules.auth.dto;

import com.nsbm.rocs.entity.enums.AccountStatus;
import com.nsbm.rocs.entity.enums.Role;
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

