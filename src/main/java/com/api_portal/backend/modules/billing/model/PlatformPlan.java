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
@Table(name = "platform_plans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // STARTER, GROWTH, BUSINESS

    @Column(nullable = false)
    private String displayName; // "Starter", "Growth", "Business"

    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(nullable = false)
    private String currency; // USD

    // Limites do plano
    private Integer maxApis;
    private Integer maxRequestsPerMonth;
    private Integer maxTeamMembers;

    // Features
    private boolean customDomain;
    private boolean prioritySupport;
    private boolean advancedAnalytics;

    // IDs dos preços nos gateways
    private String stripePriceId;
    private String vinti4PriceId;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Integer displayOrder; // Ordem de exibição (1, 2, 3...)

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
