package com.api_portal.backend.modules.billing.model;

import com.api_portal.backend.modules.billing.model.enums.WithdrawalMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "withdrawal_fee_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalFeeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private WithdrawalMethod withdrawalMethod;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal feePercentage; // ex: 2.50

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fixedFee; // ex: 0.50

    @Column(nullable = false, length = 3)
    private String fixedFeeCurrency; // USD, CVE

    @Column(precision = 10, scale = 2)
    private BigDecimal minimumAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal maximumAmount;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private UUID updatedBy;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
