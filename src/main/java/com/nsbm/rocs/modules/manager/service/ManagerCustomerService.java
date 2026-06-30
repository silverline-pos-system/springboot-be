package com.nsbm.rocs.modules.manager.service;

import com.nsbm.rocs.entity.pos.Customer;
import com.nsbm.rocs.entity.pos.Sale;
import com.nsbm.rocs.modules.manager.dto.LoyaltyStatsDTO;
import com.nsbm.rocs.modules.manager.dto.ManagerCustomerDTO;
import com.nsbm.rocs.modules.manager.repository.ManagerCustomerRepository;
import com.nsbm.rocs.modules.manager.repository.ManagerSaleRepository;
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
public class ManagerCustomerService {

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
        
        // In a real app, fetching last purchase for ALL customers N+1 is bad.
        // For this project scale (100-1000 customers), it's acceptable.
        // Optimization: Find all sales and group by customer in memory or complex query.
        
        // Let's assume we do a simple map for now.
        return customers.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private ManagerCustomerDTO mapToDTO(Customer customer) {
        BigDecimal spend = customer.getTotalPurchases() != null ? customer.getTotalPurchases() : BigDecimal.ZERO;
        String tier = calculateTier(spend);
        
        // Note: Visit count and Last Purchase would ideally come from DB queries.
        // For now, we will leave them generic or would need saleRepository calls.
        
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
                .visitCount(0) // Requires heavy query, skipping for list view performance for now
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
    
    public List<com.nsbm.rocs.modules.manager.dto.ManagerSaleDTO> getCustomerSales(Long customerId) {
        return saleRepository.findTop10ByCustomerIdOrderBySaleDateDesc(customerId).stream()
            .map(s -> com.nsbm.rocs.modules.manager.dto.ManagerSaleDTO.builder()
                .id(s.getSaleId())
                .date(s.getSaleDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .amount(s.getNetTotal())
                .paymentStatus(s.getPaymentStatus())
                .invoiceNo(s.getInvoiceNo())
                .build())
            .collect(Collectors.toList());
    }
}

