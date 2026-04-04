package com.api_portal.backend.modules.billing.gateway.stripe;

import com.api_portal.backend.modules.billing.gateway.PaymentGateway;
import com.api_portal.backend.modules.billing.gateway.dto.*;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementação completa do gateway Stripe
 */
@Component
@Slf4j
public class StripeGateway implements PaymentGateway {

    @Value("${billing.stripe.api-key:}")
    private String apiKey;

    @Value("${billing.stripe.webhook-secret:}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isEmpty()) {
            Stripe.apiKey = apiKey;
            log.info("Stripe gateway initialized with API key");
        } else {
            log.warn("Stripe API key not configured");
        }
    }

    @Override
    public String getType() {
        return "STRIPE";
    }

    @Override
    public CheckoutSession createCheckoutSession(CheckoutRequest request) {
        try {
            log.info("Creating Stripe checkout session for order: {}", request.getOrderId());

            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(request.getSuccessUrl())
                .setCancelUrl(request.getCancelUrl())
                .addLineItem(SessionCreateParams.LineItem.builder()
                    .setPrice(request.getGatewayPriceId())
                    .setQuantity(1L)
                    .build());

            // Adicionar metadata se fornecido
            if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
                paramsBuilder.putAllMetadata(request.getMetadata());
            }

            SessionCreateParams params = paramsBuilder.build();
            Session session = Session.create(params);

            log.info("Stripe checkout session created: sessionId={}, url={}", 
                session.getId(), session.getUrl());

            return new CheckoutSession(session.getId(), session.getUrl());

        } catch (StripeException e) {
            log.error("Error creating Stripe checkout session", e);
            throw new RuntimeException("Failed to create Stripe checkout session: " + e.getMessage(), e);
        }
    }

    @Override
    public WebhookEvent parseWebhook(String payload, String signature) {
        try {
            log.debug("Parsing Stripe webhook with signature");

            // Validar assinatura do webhook
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);

            log.info("Stripe webhook validated: eventId={}, type={}", 
                event.getId(), event.getType());

            return convertStripeEvent(event);

        } catch (Exception e) {
            log.error("Error parsing Stripe webhook", e);
            throw new SecurityException("Invalid Stripe webhook signature: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isHealthy() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Stripe health check failed: API key not configured");
            return false;
        }

        try {
            // Testar conexão com Stripe
            Stripe.apiKey = apiKey;
            // Balance.retrieve() é uma chamada leve para testar a API
            com.stripe.model.Balance.retrieve();
            log.debug("Stripe health check passed");
            return true;
        } catch (Exception e) {
            log.error("Stripe health check failed", e);
            return false;
        }
    }

    @Override
    public GatewayMetadata getMetadata() {
        return GatewayMetadata.builder()
            .displayName("Stripe")
            .logoUrl("https://stripe.com/img/v3/home/social.png")
            .supportedCurrencies(Arrays.asList("USD", "EUR", "GBP", "BRL", "CVE"))
            .supportsSubscriptions(true)
            .supportsRefunds(true)
            .supportsWebhooks(true)
            .build();
    }

    /**
     * Converte evento do Stripe para formato interno
     */
    private WebhookEvent convertStripeEvent(Event event) {
        Map<String, String> metadata = new HashMap<>();

        // Extrair dados específicos baseado no tipo de evento
        String paymentId = null;
        String subscriptionId = null;
        String customerId = null;
        BigDecimal amount = null;
        String currency = null;
        String status = null;

        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    var session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (session != null) {
                        subscriptionId = session.getSubscription();
                        customerId = session.getCustomer();
                        amount = session.getAmountTotal() != null 
                            ? BigDecimal.valueOf(session.getAmountTotal()).divide(BigDecimal.valueOf(100)) 
                            : null;
                        currency = session.getCurrency();
                        status = session.getPaymentStatus();
                        if (session.getMetadata() != null) {
                            metadata.putAll(session.getMetadata());
                        }
                    }
                    break;

                case "invoice.payment_succeeded":
                case "invoice.paid":
                    var invoice = (com.stripe.model.Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (invoice != null) {
                        paymentId = invoice.getPaymentIntent();
                        subscriptionId = invoice.getSubscription();
                        customerId = invoice.getCustomer();
                        amount = invoice.getAmountPaid() != null 
                            ? BigDecimal.valueOf(invoice.getAmountPaid()).divide(BigDecimal.valueOf(100)) 
                            : null;
                        currency = invoice.getCurrency();
                        status = invoice.getStatus();
                        if (invoice.getMetadata() != null) {
                            metadata.putAll(invoice.getMetadata());
                        }
                    }
                    break;

                case "customer.subscription.created":
                case "customer.subscription.updated":
                case "customer.subscription.deleted":
                    var subscription = (com.stripe.model.Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (subscription != null) {
                        subscriptionId = subscription.getId();
                        customerId = subscription.getCustomer();
                        status = subscription.getStatus();
                        if (subscription.getMetadata() != null) {
                            metadata.putAll(subscription.getMetadata());
                        }
                    }
                    break;

                case "payment_intent.succeeded":
                    var paymentIntent = (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (paymentIntent != null) {
                        paymentId = paymentIntent.getId();
                        customerId = paymentIntent.getCustomer();
                        amount = paymentIntent.getAmount() != null 
                            ? BigDecimal.valueOf(paymentIntent.getAmount()).divide(BigDecimal.valueOf(100)) 
                            : null;
                        currency = paymentIntent.getCurrency();
                        status = paymentIntent.getStatus();
                        if (paymentIntent.getMetadata() != null) {
                            metadata.putAll(paymentIntent.getMetadata());
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            log.error("Error extracting data from Stripe event: {}", event.getType(), e);
        }

        return WebhookEvent.builder()
            .eventId(event.getId())
            .eventType(event.getType())
            .gatewayType("STRIPE")
            .paymentId(paymentId)
            .subscriptionId(subscriptionId)
            .customerId(customerId)
            .amount(amount)
            .currency(currency)
            .status(status)
            .timestamp(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(event.getCreated()), 
                ZoneId.systemDefault()))
            .metadata(metadata)
            .build();
    }
}
