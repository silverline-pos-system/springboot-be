package com.silverline.erp.module.pos.dto.customer;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateCustomerRequest {
    private String name;
    private String phone;
    private String email;
    private String address;
    private String city;
    private LocalDate dateOfBirth;
}

