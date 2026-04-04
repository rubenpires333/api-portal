package com.api_portal.backend.modules.billing.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {
    private String eventId;
    private String eventType; // payment_intent.succeeded, subscription.created, etc.
    private String gatewayType; // STRIPE, VINTI4, etc.
    private String paymentId;
    private String subscriptionId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime timestamp;
    private Map<String, String> metadata;
}
