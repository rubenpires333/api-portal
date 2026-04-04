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
@Table(name = "provider_wallets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID providerId;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal reservedBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal lifetimeEarned = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "EUR";

    // Preferências de levantamento
    @Enumerated(EnumType.STRING)
    private WithdrawalMethod preferredMethod;

    @Column(columnDefinition = "TEXT")
    private String payoutDetails; // Encriptado: email, IBAN, número Vinti4

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minimumPayout = new BigDecimal("10.00");

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
