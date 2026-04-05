package com.api_portal.backend.modules.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSubscriptionDTO {
    
    private UUID id;
    private String planName;
    private String status;
    private BigDecimal amount;
    private String currency;
    
    // Período
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;
    
    // Provider
    private ProviderInfo provider;
    
    // Stripe
    private String stripeSubscriptionId;
    private String stripeCustomerId;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderInfo {
        private UUID id;
        private String name;
        private String email;
        private String username;
    }
}
