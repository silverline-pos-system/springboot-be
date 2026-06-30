package com.silverline.erp.domain.service;

import com.silverline.erp.domain.enums.ServiceStatus;
import com.silverline.erp.domain.enums.ServiceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sale_services")
@Getter
@Setter
@NoArgsConstructor
public class SaleService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "sale_id")
    private Long saleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 50)
    private ServiceType serviceType;

    @Column(name = "installation_required")
    private Boolean installationRequired = false;

    @Column(name = "technician_id")
    private Long technicianId;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_status", length = 30)
    private ServiceStatus serviceStatus = ServiceStatus.PENDING;

    @Column(name = "service_charge", precision = 15, scale = 2)
    private BigDecimal serviceCharge = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "contact_no", length = 20)
    private String contactNo;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "balance_collected", precision = 15, scale = 2)
    private BigDecimal balanceCollected = BigDecimal.ZERO;

    @Column(name = "additional_items", columnDefinition = "TEXT")
    private String additionalItems;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
