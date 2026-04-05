package com.api_portal.backend.modules.billing.model;

import com.api_portal.backend.modules.billing.model.enums.CheckoutSessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "checkout_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String stripeSessionId;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false)
    private UUID planId;

    @Column(nullable = false)
    private String planName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckoutSessionStatus status;

    private String stripeCustomerId;
    
    private String stripeSubscriptionId;

    private String stripePaymentIntentId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime expiredAt;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = CheckoutSessionStatus.PENDING;
        }
    }
}
