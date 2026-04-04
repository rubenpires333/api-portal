package com.api_portal.backend.modules.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPlanDTO {
    private String name;
    private String displayName;
    private String description;
    private BigDecimal monthlyPrice;
    private String currency;
    
    // Limites
    private Integer maxApis;
    private Integer maxRequestsPerMonth;
    private Integer maxTeamMembers;
    
    // Features
    private boolean customDomain;
    private boolean prioritySupport;
    private boolean advancedAnalytics;
    
    // Gateway Price IDs
    private String stripePriceId;
    private String vinti4PriceId;
    
    private boolean active;
    private Integer displayOrder;
}
