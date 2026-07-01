package com.silverline.erp.module.pos.service;

import com.silverline.erp.domain.pos.Customer;
import com.silverline.erp.module.analytics.dto.LoyaltyStatsDTO;
import com.silverline.erp.module.manager.dto.ManagerCustomerDTO;
import com.silverline.erp.module.manager.dto.ManagerSaleDTO;
import com.silverline.erp.module.manager.repository.ManagerCustomerRepository;
import com.silverline.erp.module.manager.repository.ManagerSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final ManagerCustomerRepository customerRepository;
    private final ManagerSaleRepository saleRepository;

    public List<LoyaltyStatsDTO> getLoyaltyStats() {
        List<LoyaltyStatsDTO> stats = new ArrayList<>();

        Long totalCustomers = customerRepository.count();
        Long activeCustomers = customerRepository.countActiveCustomers();
        Long totalPoints = customerRepository.sumTotalLoyaltyPoints();
        if (totalPoints == null) totalPoints = 0L;

        stats.add(LoyaltyStatsDTO.builder()
                .title("Total Customers")
                .value(String.valueOf(totalCustomers))
                .description(activeCustomers + " Active")
                .icon("users")
                .build());

        stats.add(LoyaltyStatsDTO.builder()
                .title("Loyalty Points")
                .value(String.format("%,d", totalPoints))
                .description("Total Issued")
                .icon("star")
                .build());
        
        // Mock data for now as we don't have redemption history table yet
        stats.add(LoyaltyStatsDTO.builder()
                .title("Redemption Rate")
                .value("12%")
                .description("Points redeemed")
                .trend("up")
                .icon("percent")
                .build());

        stats.add(LoyaltyStatsDTO.builder()
                .title("Avg. Visit Freq")
                .value("2.4")
                .description("Visits / Month")
                .icon("activity")
                .build());

        return stats;
    }

    public List<ManagerCustomerDTO> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        return customers.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private ManagerCustomerDTO mapToDTO(Customer customer) {
        BigDecimal spend = customer.getTotalPurchases() != null ? customer.getTotalPurchases() : BigDecimal.ZERO;
        String tier = calculateTier(spend);
        
        return ManagerCustomerDTO.builder()
                .id(customer.getCustomerId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .tier(tier)
                .points(customer.getLoyaltyPoints())
                .availablePoints(String.valueOf(customer.getLoyaltyPoints()))
                .totalSpend("LKR " + String.format("%,.2f", spend))
                .lastPurchase(customer.getUpdatedAt() != null ? customer.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "Never")
                .visitCount(0)
                .status(customer.getIsActive() ? "Active" : "Inactive")
                .address(customer.getAddress())
                .city(customer.getCity())
                .dateOfBirth(customer.getDateOfBirth() != null ? customer.getDateOfBirth().toString() : null)
                .build();
    }

    // In-memory tier config (would typically be in DB)
    public static Map<String, Double> TIER_THRESHOLDS = new HashMap<>();
    static {
        TIER_THRESHOLDS.put("Platinum", 100000.0);
        TIER_THRESHOLDS.put("Gold", 50000.0);
        TIER_THRESHOLDS.put("Silver", 10000.0);
    }

    private String calculateTier(BigDecimal totalSpend) {
        double amount = totalSpend.doubleValue();
        if (amount > TIER_THRESHOLDS.getOrDefault("Platinum", 100000.0)) return "Platinum";
        if (amount > TIER_THRESHOLDS.getOrDefault("Gold", 50000.0)) return "Gold";
        if (amount > TIER_THRESHOLDS.getOrDefault("Silver", 10000.0)) return "Silver";
        return "Bronze";
    }

    public Map<String, Double> getTierRules() {
        return TIER_THRESHOLDS;
    }

    public void updateTierRules(Map<String, Double> newRules) {
        TIER_THRESHOLDS.putAll(newRules);
    }
    
    public void addPoints(Long customerId, int points, String reason) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        customer.setLoyaltyPoints((customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0) + points);
        customerRepository.save(customer);
        log.info("Adjusted points for customer {}: {} ({})", customerId, points, reason);
    }

    public void updateCustomer(Long id, ManagerCustomerDTO dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setName(dto.getName());
        customer.setPhone(dto.getPhone());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());
        customer.setCity(dto.getCity());
        
        if (dto.getDateOfBirth() != null && !dto.getDateOfBirth().isEmpty()) {
             customer.setDateOfBirth(LocalDate.parse(dto.getDateOfBirth()));
        }

        if ("Active".equalsIgnoreCase(dto.getStatus())) {
            customer.setIsActive(true);
        } else if ("Inactive".equalsIgnoreCase(dto.getStatus())) {
             customer.setIsActive(false);
        }
        
        customerRepository.save(customer);
    }
    
    public List<ManagerSaleDTO> getCustomerSales(Long customerId) {
        return saleRepository.findTop10ByCustomerIdOrderBySaleDateDesc(customerId).stream()
            .map(s -> ManagerSaleDTO.builder()
                .id(s.getSaleId())
                .date(s.getSaleDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .amount(s.getNetTotal())
                .paymentStatus(s.getPaymentStatus())
                .invoiceNo(s.getInvoiceNo())
                .build())
            .collect(Collectors.toList());
    }
}
