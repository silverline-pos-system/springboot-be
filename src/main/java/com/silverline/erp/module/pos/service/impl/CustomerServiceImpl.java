package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.domain.pos.Customer;
import com.silverline.erp.module.analytics.dto.LoyaltyStatsDTO;
import com.silverline.erp.module.manager.dto.ManagerCustomerDTO;
import com.silverline.erp.module.manager.dto.ManagerSaleDTO;
import com.silverline.erp.module.pos.repository.CustomerRepository;
import com.silverline.erp.module.pos.service.CustomerService;
import com.silverline.erp.module.pos.service.SaleQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final SaleQueryService saleQueryService;

    private Pageable capPageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20);
        }
        int cappedSize = Math.min(pageable.getPageSize(), 100);
        return PageRequest.of(pageable.getPageNumber(), cappedSize, pageable.getSort());
    }

    private static final Map<String, Double> TIER_THRESHOLDS = new HashMap<>();
    static {
        TIER_THRESHOLDS.put("Platinum", 100000.0);
        TIER_THRESHOLDS.put("Gold", 50000.0);
        TIER_THRESHOLDS.put("Silver", 10000.0);
    }

    @Override
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

    @Override
    public Page<ManagerCustomerDTO> getAllCustomers(Pageable pageable) {
        List<Customer> customers = customerRepository.findAll();
        List<ManagerCustomerDTO> allDtos = customers.stream().map(this::mapToDTO).collect(Collectors.toList());
        if (pageable.isUnpaged()) {
            return new PageImpl<>(allDtos);
        }
        Pageable capped = capPageable(pageable);
        int start = (int) capped.getOffset();
        int end = Math.min((start + capped.getPageSize()), allDtos.size());
        List<ManagerCustomerDTO> content = start < allDtos.size() ? allDtos.subList(start, end) : List.of();
        return new PageImpl<>(content, capped, allDtos.size());
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

    private String calculateTier(BigDecimal totalSpend) {
        double amount = totalSpend.doubleValue();
        if (amount > TIER_THRESHOLDS.getOrDefault("Platinum", 100000.0)) return "Platinum";
        if (amount > TIER_THRESHOLDS.getOrDefault("Gold", 50000.0)) return "Gold";
        if (amount > TIER_THRESHOLDS.getOrDefault("Silver", 10000.0)) return "Silver";
        return "Bronze";
    }

    @Override
    public Map<String, Double> getTierRules() {
        return TIER_THRESHOLDS;
    }

    @Override
    public void updateTierRules(Map<String, Double> newRules) {
        TIER_THRESHOLDS.putAll(newRules);
    }
    
    @Override
    public void addPoints(Long customerId, int points, String reason) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        customer.setLoyaltyPoints((customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0) + points);
        customerRepository.save(customer);
        log.info("Adjusted points for customer {}: {} ({})", customerId, points, reason);
    }

    @Override
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
    
    @Override
    public List<ManagerSaleDTO> getCustomerSales(Long customerId) {
        return saleQueryService.findTop10ByCustomerIdOrderBySaleDateDesc(customerId).stream()
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
