package com.api_portal.backend.modules.subscription.domain.entity;

import com.api_portal.backend.modules.api.domain.Api;
import com.api_portal.backend.modules.subscription.domain.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscriptions", indexes = {
    @Index(name = "idx_subscription_consumer", columnList = "consumer_id"),
    @Index(name = "idx_subscription_api", columnList = "api_id"),
    @Index(name = "idx_subscription_status", columnList = "status")
})
public class Subscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id", nullable = false)
    private Api api;
    
    @Column(name = "api_version_id")
    private UUID apiVersionId;
    
    @Column(name = "consumer_id", nullable = false)
    private String consumerId; // Keycloak user ID
    
    @Column(name = "consumer_email", nullable = false)
    private String consumerEmail;
    
    @Column(name = "consumer_name")
    private String consumerName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;
    
    @Column(name = "api_key", unique = true, nullable = false, length = 64)
    private String apiKey;
    
    @Column(name = "requests_used")
    @Builder.Default
    private Integer requestsUsed = 0;
    
    @Column(name = "requests_limit")
    private Integer requestsLimit;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    @Column(name = "revoke_reason", length = 500)
    private String revokeReason;
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
