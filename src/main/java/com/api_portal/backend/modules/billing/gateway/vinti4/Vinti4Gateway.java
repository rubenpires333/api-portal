package com.api_portal.backend.modules.billing.gateway.vinti4;

import com.api_portal.backend.modules.billing.gateway.PaymentGateway;
import com.api_portal.backend.modules.billing.gateway.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Implementação do gateway Vinti4 (Cabo Verde)
 * Suporta Multicaixa e pagamentos móveis
 */
@Component
@Slf4j
public class Vinti4Gateway implements PaymentGateway {

    @Value("${billing.vinti4.api-key:}")
    private String apiKey;

    @Value("${billing.vinti4.merchant-id:}")
    private String merchantId;

    @Value("${billing.vinti4.webhook-secret:}")
    private String webhookSecret;

    @Override
    public String getType() {
        return "VINTI4";
    }

    @Override
    public CheckoutSession createCheckoutSession(CheckoutRequest request) {
        // TODO: Implementar integração com API Vinti4
        log.warn("Vinti4 integration not yet implemented");
        throw new UnsupportedOperationException("Vinti4 integration pending");
    }

    @Override
    public WebhookEvent parseWebhook(String payload, String signature) {
        // TODO: Implementar validação HMAC-SHA256
        log.warn("Vinti4 webhook parsing not yet implemented");
        throw new UnsupportedOperationException("Vinti4 webhook parsing pending");
    }

    @Override
    public boolean isHealthy() {
        return apiKey != null && !apiKey.isEmpty() && merchantId != null;
    }

    @Override
    public GatewayMetadata getMetadata() {
        return GatewayMetadata.builder()
            .displayName("Vinti4")
            .logoUrl("https://vinti4.com/logo.png")
            .supportedCurrencies(Arrays.asList("CVE"))
            .supportsSubscriptions(true)
            .supportsRefunds(false)
            .supportsWebhooks(true)
            .build();
    }
}
