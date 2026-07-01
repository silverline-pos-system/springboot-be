package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.common.audit.AuditLogService;
import com.silverline.erp.common.email.EmailService;
import com.silverline.erp.domain.pos.Customer;
import com.silverline.erp.module.pos.dto.customer.CreateCustomerRequest;
import com.silverline.erp.module.pos.repository.CustomerRepository;
import com.silverline.erp.module.pos.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoyaltyServiceImpl implements LoyaltyService {

    private final CustomerRepository customerRepository;
    private final EmailService emailService;
    private final AuditLogService activityLogService;

    private final Map<Long, String> loyaltyOtpStore = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public Customer createCustomer(CreateCustomerRequest request) {
        if (request.getName() == null || request.getName().isEmpty()) {
            throw new RuntimeException("Customer Name is required");
        }
        if (request.getPhone() == null || request.getPhone().isEmpty()) {
            throw new RuntimeException("Phone is required");
        }
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new RuntimeException("Email is required");
        }

        if (customerRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new RuntimeException("Customer with this phone already exists");
        }

        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setLoyaltyPoints(0);
        customer.setTotalPurchases(BigDecimal.ZERO);
        customer.setIsActive(true);
        customer.setCode(generateCustomerCode(request.getPhone()));

        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void updateLoyaltyPoints(Long customerId, Integer points) {
        customerRepository.findById(customerId).ifPresent(customer -> {
            Integer current = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
            customer.setLoyaltyPoints(current + points);
            customerRepository.save(customer);
            
            try {
                activityLogService.logActivity(
                    1L,
                    null,
                    null,
                    "System",
                    "SYSTEM",
                    "LOYALTY_UPDATE",
                    "CUSTOMER",
                    customerId,
                    "Updated loyalty points for customer " + customer.getName() + ": " + (points > 0 ? "+" : "") + points,
                    "{\"newBalance\":" + customer.getLoyaltyPoints() + "}"
                );
            } catch (Exception e) {
                log.error("Failed to log loyalty update activity: {}", e.getMessage());
            }
        });
    }

    @Override
    public void requestLoyaltyRedemption(Long customerId, Integer pointsReq) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
                
        if (customer.getLoyaltyPoints() == null || customer.getLoyaltyPoints() < 100) {
            throw new RuntimeException("Minimum 100 points required to redeem.");
        }
        if (customer.getLoyaltyPoints() < pointsReq) {
            throw new RuntimeException("Insufficient loyalty points limit.");
        }
        if (customer.getEmail() == null || customer.getEmail().isEmpty()) {
            throw new RuntimeException("Customer email not configured. Please register customer email first.");
        }

        String otp = String.format("%04d", new Random().nextInt(10000));
        loyaltyOtpStore.put(customerId, otp);

        String subject = "ROCS POS - Loyalty Points Redemption";
        String body = String.format(
                "Hello %s,\n\n" +
                "You have requested to redeem %d loyalty points at ROCS POS.\n" +
                "Your authorization code is: %s\n\n" +
                 "If this was not you, please contact support.\n\n" +
                 "Thank you,\nROCS System",
                customer.getName(), pointsReq, otp
        );
        emailService.sendSimpleMessage(customer.getEmail(), subject, body);
    }

    @Override
    @Transactional
    public BigDecimal verifyLoyaltyRedemption(Long customerId, Integer pointsReq, String otpCode) {
        String storedOtp = loyaltyOtpStore.get(customerId);
        if (storedOtp == null || !storedOtp.equals(otpCode)) {
            throw new RuntimeException("Invalid or expired OTP code");
        }
        
        loyaltyOtpStore.remove(customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getLoyaltyPoints() == null || customer.getLoyaltyPoints() < pointsReq) {
            throw new RuntimeException("Customer does not have enough points.");
        }

        customer.setLoyaltyPoints(customer.getLoyaltyPoints() - pointsReq);
        customerRepository.save(customer);
        
        return BigDecimal.valueOf(pointsReq);
    }

    @Override
    public List<Customer> searchCustomers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Customer> byPhone = customerRepository.findByPhoneContaining(query);
        List<Customer> byName = customerRepository.findByNameContainingIgnoreCase(query);
        
        Set<Customer> merged = new HashSet<>(byPhone);
        merged.addAll(byName);
        
        return new ArrayList<>(merged);
    }

    @Override
    public Customer getCustomerByCode(String code) {
        return customerRepository.findByCode(code).orElse(null);
    }

    private String generateCustomerCode(String phone) {
        return "CUS-" + phone;
    }
}
