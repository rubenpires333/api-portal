package com.api_portal.backend.modules.billing.gateway;

import com.api_portal.backend.modules.billing.gateway.dto.CheckoutRequest;
import com.api_portal.backend.modules.billing.gateway.dto.CheckoutSession;
import com.api_portal.backend.modules.billing.gateway.dto.GatewayMetadata;
import com.api_portal.backend.modules.billing.gateway.dto.WebhookEvent;

/**
 * Interface central para integração com gateways de pagamento.
 * Qualquer novo gateway (Stripe, Vinti4, PayPal, etc.) deve implementar esta interface.
 */
public interface PaymentGateway {

    /**
     * Identificador único do gateway (ex: "STRIPE", "VINTI4", "PAYPAL")
     */
    String getType();

    /**
     * Cria uma sessão de checkout e retorna URL de pagamento
     */
    CheckoutSession createCheckoutSession(CheckoutRequest request);

    /**
     * Valida e parseia webhook recebido do gateway
     * @throws SecurityException se a assinatura for inválida
     */
    WebhookEvent parseWebhook(String payload, String signature);

    /**
     * Verifica se o gateway está operacional
     */
    boolean isHealthy();

    /**
     * Retorna metadados do gateway (nome, logo, moedas suportadas)
     */
    GatewayMetadata getMetadata();
}
