package com.silverline.erp.module.pos.service;

import com.silverline.erp.domain.pos.Customer;
import com.silverline.erp.module.pos.dto.customer.CreateCustomerRequest;

import java.math.BigDecimal;
import java.util.List;

public interface LoyaltyService {
    Customer createCustomer(CreateCustomerRequest request);

    void updateLoyaltyPoints(Long customerId, Integer points);

    void requestLoyaltyRedemption(Long customerId, Integer pointsReq);

    BigDecimal verifyLoyaltyRedemption(Long customerId, Integer pointsReq, String otpCode);

    List<Customer> searchCustomers(String query);

    Customer getCustomerByCode(String code);
}
