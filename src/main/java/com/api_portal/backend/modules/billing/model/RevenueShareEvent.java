package com.api_portal.backend.modules.billing.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "revenue_share_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueShareEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID subscriptionId; // Subscription do consumer à API

    private UUID providerId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount; // Valor total pago pelo consumer

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal platformCommissionPercentage; // ex: 20.00

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal platformCommission; // Valor retido pela plataforma

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal providerShare; // Valor creditado ao provider

    @Column(nullable = false)
    private String currency;

    private UUID walletTransactionId; // Referência à transação criada

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
