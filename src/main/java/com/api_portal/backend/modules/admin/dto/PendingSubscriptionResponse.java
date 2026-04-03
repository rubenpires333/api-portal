package com.api_portal.backend.modules.admin.dto;

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
public class PendingSubscriptionResponse {
    
    private UUID id;
    private String apiName;
    private String apiSlug;
    private UUID apiId;
    private String consumerName;
    private String consumerEmail;
    private String consumerId;
    private String providerName;
    private String providerId;
    private LocalDateTime requestedAt;
    private Long daysWaiting;
}
