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
        log.info("Initializing Stripe gateway...");
        log.info("API Key from config: {}", apiKey != null ? apiKey.substring(0, Math.min(15, apiKey.length())) + "..." : "NULL");
        
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.isBlank()) {
            Stripe.apiKey = apiKey;
            log.info("✅ Stripe gateway initialized successfully with API key");
        } else {
            log.error("❌ Stripe API key is NULL or EMPTY - Check application-billing.properties");
        }
    }

    @Override
    public String getType() {
        return "STRIPE";
    }

    @Override
    public CheckoutSession createCheckoutSession(CheckoutRequest request) {
        // Garantir que a chave está setada antes de cada chamada
        if (apiKey != null && !apiKey.isEmpty()) {
            Stripe.apiKey = apiKey;
        } else {
            throw new RuntimeException("Stripe API key not configured");
        }
        
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
     * Criar Payment Intent para pagamento embutido
     */
    public Map<String, String> createPaymentIntent(BigDecimal amount, String currency, Map<String, String> metadata) {
        if (apiKey != null && !apiKey.isEmpty()) {
            Stripe.apiKey = apiKey;
            log.info("✅ Stripe API key set for Payment Intent creation");
        } else {
            log.error("❌ Stripe API key is NULL or EMPTY");
            throw new RuntimeException("Stripe API key not configured");
        }

        try {
            log.info("Creating Stripe Payment Intent: amount={}, currency={}, metadata={}", amount, currency, metadata);

            // Converter para centavos
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, Object> params = new HashMap<>();
            params.put("amount", amountInCents);
            params.put("currency", currency.toLowerCase());
            params.put("automatic_payment_methods", Map.of("enabled", true));
            
            if (metadata != null && !metadata.isEmpty()) {
                params.put("metadata", metadata);
                log.info("✅ Metadata added to Payment Intent params: {}", metadata);
            } else {
                log.warn("⚠️ No metadata provided for Payment Intent");
            }

            log.info("Calling Stripe API with params: {}", params);
            com.stripe.model.PaymentIntent paymentIntent = com.stripe.model.PaymentIntent.create(params);

            log.info("Payment Intent created: id={}, metadata={}", paymentIntent.getId(), paymentIntent.getMetadata());
            
            // Verificar imediatamente se metadata foi salvo
            if (paymentIntent.getMetadata() == null || paymentIntent.getMetadata().isEmpty()) {
                log.error("❌ CRITICAL: Payment Intent created WITHOUT metadata! This will cause webhook processing to fail!");
            } else {
                log.info("✅ Payment Intent metadata confirmed: {}", paymentIntent.getMetadata());
            }

            Map<String, String> result = new HashMap<>();
            result.put("clientSecret", paymentIntent.getClientSecret());
            result.put("paymentIntentId", paymentIntent.getId());
            
            return result;

        } catch (StripeException e) {
            log.error("Error creating Payment Intent", e);
            throw new RuntimeException("Failed to create Payment Intent: " + e.getMessage(), e);
        }
    }

    /**
     * Converte evento do Stripe para formato interno
     */
    private WebhookEvent convertStripeEvent(Event event) {
        log.info("Converting Stripe event: type={}, id={}", event.getType(), event.getId());
        
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
                        // Adicionar o ID da sessão Stripe ao metadata
                        metadata.put("stripeSessionId", session.getId());
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
                case "payment_intent.created":
                    var paymentIntent = (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (paymentIntent != null) {
                        paymentId = paymentIntent.getId();
                        customerId = paymentIntent.getCustomer();
                        amount = paymentIntent.getAmount() != null 
                            ? BigDecimal.valueOf(paymentIntent.getAmount()).divide(BigDecimal.valueOf(100)) 
                            : null;
                        currency = paymentIntent.getCurrency();
                        status = paymentIntent.getStatus();
                        
                        log.info("PaymentIntent do webhook: id={}, metadata={}", paymentId, paymentIntent.getMetadata());
                        
                        if (paymentIntent.getMetadata() != null && !paymentIntent.getMetadata().isEmpty()) {
                            metadata.putAll(paymentIntent.getMetadata());
                        }
                    } else {
                        // Fallback: extrair ID do evento e buscar da API (Stripe CLI forwarding)
                        log.warn("⚠️ PaymentIntent object is NULL, extracting ID from event");
                        try {
                            // O ID do Payment Intent está no raw JSON
                            String rawJson = event.getData().toJson();
                            log.debug("Raw event data JSON: {}", rawJson);
                            
                            // Extrair ID do objeto (não do evento)
                            // Procurar por "object": {"id": "pi_xxx"
                            int objectStart = rawJson.indexOf("\"object\": {");
                            if (objectStart > 0) {
                                int idStart = rawJson.indexOf("\"id\": \"", objectStart) + 7;
                                int idEnd = rawJson.indexOf("\"", idStart);
                                if (idStart > 7 && idEnd > idStart) {
                                    paymentId = rawJson.substring(idStart, idEnd);
                                    log.info("Extracted Payment Intent ID from JSON: {}", paymentId);
                                    
                                    // Buscar Payment Intent completo da API
                                    if (apiKey != null && !apiKey.isEmpty()) {
                                        Stripe.apiKey = apiKey;
                                    }
                                    
                                    log.info("Buscando Payment Intent completo da API: {}", paymentId);
                                    com.stripe.model.PaymentIntent fullPI = com.stripe.model.PaymentIntent.retrieve(paymentId);
                                    
                                    customerId = fullPI.getCustomer();
                                    amount = fullPI.getAmount() != null 
                                        ? BigDecimal.valueOf(fullPI.getAmount()).divide(BigDecimal.valueOf(100)) 
                                        : null;
                                    currency = fullPI.getCurrency();
                                    status = fullPI.getStatus();
                                    
                                    if (fullPI.getMetadata() != null && !fullPI.getMetadata().isEmpty()) {
                                        metadata.putAll(fullPI.getMetadata());
                                        log.info("✅ Metadata carregado do Payment Intent: {}", metadata);
                                    } else {
                                        log.warn("⚠️ Payment Intent {} não tem metadata", paymentId);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("Erro ao extrair Payment Intent do JSON: {}", e.getMessage(), e);
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            log.error("Error extracting data from Stripe event: type={}, id={}", event.getType(), event.getId(), e);
        }

        log.info("Extracted from event: paymentId={}, subscriptionId={}, customerId={}, amount={}, metadata={}", 
            paymentId, subscriptionId, customerId, amount, metadata);

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

    /**
     * Buscar detalhes do Payment Intent para gerar recibo
     */
    public Map<String, Object> getPaymentIntentDetails(String paymentIntentId) {
        if (apiKey != null && !apiKey.isEmpty()) {
            Stripe.apiKey = apiKey;
        } else {
            throw new RuntimeException("Stripe API key not configured");
        }

        try {
            log.info("Fetching Payment Intent details: id={}", paymentIntentId);

            com.stripe.model.PaymentIntent paymentIntent = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);

            Map<String, Object> receipt = new HashMap<>();
            receipt.put("paymentIntentId", paymentIntent.getId());
            receipt.put("amount", BigDecimal.valueOf(paymentIntent.getAmount()).divide(BigDecimal.valueOf(100)));
            receipt.put("currency", paymentIntent.getCurrency().toUpperCase());
            receipt.put("status", paymentIntent.getStatus());
            receipt.put("created", LocalDateTime.ofInstant(
                Instant.ofEpochSecond(paymentIntent.getCreated()), 
                ZoneId.systemDefault()));
            receipt.put("metadata", paymentIntent.getMetadata());
            
            // Buscar charge associado ao Payment Intent
            if (paymentIntent.getLatestCharge() != null) {
                try {
                    com.stripe.model.Charge charge = com.stripe.model.Charge.retrieve(paymentIntent.getLatestCharge());
                    receipt.put("receiptUrl", charge.getReceiptUrl());
                    receipt.put("receiptNumber", charge.getReceiptNumber());
                } catch (Exception e) {
                    log.warn("Could not retrieve charge details", e);
                }
            }

            log.info("Payment Intent details retrieved successfully");
            return receipt;

        } catch (StripeException e) {
            log.error("Error fetching Payment Intent details", e);
            throw new RuntimeException("Failed to fetch Payment Intent details: " + e.getMessage(), e);
        }
    }
}
