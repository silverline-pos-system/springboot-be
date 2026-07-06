package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.common.audit.AuditLogService;
import com.silverline.erp.domain.pos.Customer;
import com.silverline.erp.domain.pos.LoyaltyOtp;
import com.silverline.erp.infrastructure.email.EmailService;
import com.silverline.erp.module.pos.dto.customer.CreateCustomerRequest;
import com.silverline.erp.module.pos.repository.CustomerRepository;
import com.silverline.erp.module.pos.repository.LoyaltyOtpRepository;
import com.silverline.erp.module.pos.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoyaltyServiceImpl implements LoyaltyService {

    private final CustomerRepository customerRepository;
    private final EmailService emailService;
    private final AuditLogService activityLogService;
    private final LoyaltyOtpRepository loyaltyOtpRepository;

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
    @Transactional
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

        // Save to DB and set expiry for 10 minutes
        loyaltyOtpRepository.deleteByCustomerId(customerId);
        loyaltyOtpRepository.save(new LoyaltyOtp(customerId, otp, LocalDateTime.now().plusMinutes(10)));

        String subject = "ROCS POS - Loyalty Points Redemption";
        String htmlContent = com.silverline.erp.infrastructure.email.TemplateEngine.loadAndResolve(
                "loyalty_otp",
                Map.of(
                        "customerName", customer.getName(),
                        "pointsReq", pointsReq,
                        "otpCode", otp
                )
        );
        emailService.sendHtmlMessage(customer.getEmail(), subject, htmlContent);
    }

    @Override
    @Transactional
    public BigDecimal verifyLoyaltyRedemption(Long customerId, Integer pointsReq, String otpCode) {
        // Retrieve valid OTP from database
        LoyaltyOtp storedOtp = loyaltyOtpRepository.findTopByCustomerIdAndExpiresAtAfterOrderByCreatedAtDesc(customerId, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired OTP code"));

        if (!storedOtp.getOtpCode().equals(otpCode)) {
            throw new RuntimeException("Invalid or expired OTP code");
        }

        // Clean up the OTP once verified
        loyaltyOtpRepository.deleteByCustomerId(customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getLoyaltyPoints() == null || customer.getLoyaltyPoints() < pointsReq) {
            throw new RuntimeException("Customer does not have enough points.");
        }

        customer.setLoyaltyPoints(customer.getLoyaltyPoints() - pointsReq);
        customerRepository.save(customer);

        return BigDecimal.valueOf(pointsReq);
    }

    /**
     * Periodically purge expired OTPs (every 10 minutes)
     */
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void purgeExpiredOtps() {
        log.info("Running scheduled cleanup to purge expired loyalty OTPs");
        loyaltyOtpRepository.deleteByExpiresAtBefore(LocalDateTime.now());
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
