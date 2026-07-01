package com.silverline.erp.module.pos.service;

import com.silverline.erp.module.analytics.dto.LoyaltyStatsDTO;
import com.silverline.erp.module.manager.dto.ManagerCustomerDTO;
import com.silverline.erp.module.manager.dto.ManagerSaleDTO;

import java.util.List;
import java.util.Map;

public interface CustomerService {
    List<LoyaltyStatsDTO> getLoyaltyStats();
    List<ManagerCustomerDTO> getAllCustomers();
    Map<String, Double> getTierRules();
    void updateTierRules(Map<String, Double> newRules);
    void addPoints(Long customerId, int points, String reason);
    void updateCustomer(Long id, ManagerCustomerDTO dto);
    List<ManagerSaleDTO> getCustomerSales(Long customerId);
}
