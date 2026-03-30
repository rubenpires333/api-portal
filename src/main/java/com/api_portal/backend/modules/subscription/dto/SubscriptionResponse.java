package com.api_portal.backend.modules.subscription.dto;

import com.api_portal.backend.modules.subscription.domain.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    
    private UUID id;
    private UUID apiId;
    private String apiName;
    private String apiSlug;
    private String apiVersion;
    private String consumerId;
    private String consumerEmail;
    private String consumerName;
    private SubscriptionStatus status;
    private String apiKey;
    private Integer requestsUsed;
    private Integer requestsLimit;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime expiresAt;
    private LocalDateTime approvedAt;
    private LocalDateTime revokedAt;
    private String revokeReason;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
