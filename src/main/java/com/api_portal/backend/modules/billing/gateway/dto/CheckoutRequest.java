package com.api_portal.backend.modules.billing.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String gatewayPriceId; // ID do preço no gateway (ex: Stripe Price ID)
    private String successUrl;
    private String cancelUrl;
    private String webhookUrl;
    private Map<String, String> metadata;
}
