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
public class SubscriptionStatusDTO {
    private UUID subscriptionId;
    private String planName;
    private String planDisplayName;
    private String status; // active, canceled, past_due, incomplete
    private BigDecimal amount;
    private String currency;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime cancelAt;
    private Boolean cancelAtPeriodEnd;
    private LocalDateTime createdAt;
    private String stripeSubscriptionId;
    
    // Limites do plano
    private Integer maxApis;
    private Integer maxRequestsPerMonth;
    private Integer maxTeamMembers;
    private Boolean customDomain;
    private Boolean prioritySupport;
    private Boolean advancedAnalytics;
    
    // Uso atual
    private Integer currentApis;
    private Integer currentRequests;
    private Integer currentTeamMembers;
}
