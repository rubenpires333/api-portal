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
            .supportedCurrencies(Arrays.asList("USD", "EUR", "GBP", "BRL"))  // CVE removido - não suportado
            .supportsSubscriptions(true)
            .supportsRefunds(true)
            .supportsWebhooks(true)
            .build();
    }

    /**
     * Criar Subscription com Setup Intent para pagamento embutido
     * Este método cria um customer e uma subscription, retornando o client secret
     * para confirmar o pagamento no frontend
     */
    public Map<String, String> createSubscriptionWithSetupIntent(String priceId, Map<String, String> metadata) {
        if (apiKey != null && !apiKey.isEmpty()) {
            Stripe.apiKey = apiKey;
            log.info("✅ Stripe API key set for Subscription creation");
        } else {
            log.error("❌ Stripe API key is NULL or EMPTY");
            throw new RuntimeException("Stripe API key not configured");
        }

        try {
            log.info("Creating Stripe Subscription with Payment Intent: priceId={}, metadata={}", priceId, metadata);

            // VALIDAÇÃO: Buscar e verificar o Price antes de criar a subscription
            try {
                com.stripe.model.Price price = com.stripe.model.Price.retrieve(priceId);
                log.info("📋 Price Details from Stripe:");
                log.info("  - ID: {}", price.getId());
                log.info("  - Active: {}", price.getActive());
                log.info("  - Currency: {}", price.getCurrency());
                log.info("  - Unit Amount: {} (in cents)", price.getUnitAmount());
                log.info("  - Type: {}", price.getType());
                if (price.getRecurring() != null) {
                    log.info("  - Recurring Interval: {}", price.getRecurring().getInterval());
                    log.info("  - Trial Period Days: {}", price.getRecurring().getTrialPeriodDays());
                }
                
                // VALIDAÇÃO CRÍTICA: Verificar se o preço é > 0
                if (price.getUnitAmount() == null || price.getUnitAmount() == 0) {
                    log.warn("⚠️ Price {} has unit_amount = 0! This will create a free subscription.", priceId);
                }
                
                // VALIDAÇÃO CRÍTICA: Verificar moeda suportada
                String currency = price.getCurrency().toLowerCase();
                if ("cve".equals(currency)) {
                    log.error("❌ Currency CVE (Cabo Verde Escudo) is NOT supported by Stripe for subscriptions!");
                    log.error("   Supported currencies: EUR, USD, GBP, BRL, etc.");
                    log.error("   Please create a new Price with EUR or USD currency.");
                    throw new RuntimeException(
                        "Currency CVE is not supported for subscriptions. " +
                        "Please create a new Price ID with EUR or USD currency in Stripe Dashboard."
                    );
                }
            } catch (RuntimeException e) {
                // Re-throw validation errors
                throw e;
            } catch (Exception e) {
                log.error("❌ Failed to retrieve Price details: {}", e.getMessage());
            }

            // 1. Criar Customer
            Map<String, Object> customerParams = new HashMap<>();
            if (metadata != null && !metadata.isEmpty()) {
                customerParams.put("metadata", metadata);
            }
            
            com.stripe.model.Customer customer = com.stripe.model.Customer.create(customerParams);
            log.info("✅ Customer created: id={}", customer.getId());

            // 2. Criar Subscription com payment_behavior='default_incomplete' e expand
            Map<String, Object> subscriptionParams = new HashMap<>();
            subscriptionParams.put("customer", customer.getId());
            subscriptionParams.put("items", Arrays.asList(
                Map.of("price", priceId)
            ));
            subscriptionParams.put("payment_behavior", "default_incomplete");
            subscriptionParams.put("payment_settings", Map.of(
                "save_default_payment_method", "on_subscription",
                "payment_method_types", Arrays.asList("card")
            ));
            // CRÍTICO: Expandir latest_invoice.payment_intent para garantir que vem no response
            subscriptionParams.put("expand", Arrays.asList("latest_invoice.payment_intent"));
            
            if (metadata != null && !metadata.isEmpty()) {
                subscriptionParams.put("metadata", metadata);
            }

            com.stripe.model.Subscription subscription = com.stripe.model.Subscription.create(subscriptionParams);
            log.info("✅ Subscription created: id={}, status={}", subscription.getId(), subscription.getStatus());

            // 3. Extrair Payment Intent da invoice
            com.stripe.model.Invoice latestInvoice = subscription.getLatestInvoiceObject();
            if (latestInvoice == null) {
                log.error("❌ Latest invoice is NULL - subscription: {}", subscription.getId());
                log.error("Subscription status: {}, latest_invoice ID: {}", 
                    subscription.getStatus(), subscription.getLatestInvoice());
                    
                // Tentar buscar invoice manualmente
                if (subscription.getLatestInvoice() != null) {
                    log.info("Tentando buscar invoice manualmente: {}", subscription.getLatestInvoice());
                    latestInvoice = com.stripe.model.Invoice.retrieve(subscription.getLatestInvoice());
                } else {
                    throw new RuntimeException("Failed to get latest invoice from subscription - no invoice ID");
                }
            }
            
            // LOGS DETALHADOS DA INVOICE
            log.info("📋 Invoice Details:");
            log.info("  - ID: {}", latestInvoice.getId());
            log.info("  - Status: {}", latestInvoice.getStatus());
            log.info("  - Amount Due: {}", latestInvoice.getAmountDue());
            log.info("  - Amount Paid: {}", latestInvoice.getAmountPaid());
            log.info("  - Amount Remaining: {}", latestInvoice.getAmountRemaining());
            log.info("  - Payment Intent ID: {}", latestInvoice.getPaymentIntent());
            log.info("  - Paid: {}", latestInvoice.getPaid());
            
            com.stripe.model.PaymentIntent paymentIntent = latestInvoice.getPaymentIntentObject();
            if (paymentIntent == null && latestInvoice.getPaymentIntent() != null) {
                // Buscar Payment Intent manualmente
                log.info("Payment Intent não expandido, buscando manualmente: {}", latestInvoice.getPaymentIntent());
                paymentIntent = com.stripe.model.PaymentIntent.retrieve(latestInvoice.getPaymentIntent());
            }
            
            // CASO ESPECIAL: Invoice sem Payment Intent (valor 0 ou já paga)
            if (paymentIntent == null) {
                log.warn("⚠️ Payment Intent is NULL in invoice: {}", latestInvoice.getId());
                
                // Se invoice tem amount_due = 0, não precisa de Payment Intent
                if (latestInvoice.getAmountDue() == 0) {
                    log.info("✅ Invoice has amount_due = 0, no payment needed. Subscription is active.");
                    
                    // Retornar sem clientSecret - subscription já está ativa
                    Map<String, String> result = new HashMap<>();
                    result.put("clientSecret", null); // Sem Payment Intent
                    result.put("subscriptionId", subscription.getId());
                    result.put("customerId", customer.getId());
                    result.put("paymentIntentId", null);
                    result.put("status", "active"); // Subscription já ativa
                    
                    log.info("✅ Subscription created without payment (free or trial)");
                    return result;
                }
                
                // Se invoice tem amount_due > 0 mas não tem Payment Intent, é erro
                log.error("❌ Invoice has amount_due > 0 but no Payment Intent!");
                log.error("Invoice status: {}, amount_due: {}, amount_paid: {}", 
                    latestInvoice.getStatus(), latestInvoice.getAmountDue(), latestInvoice.getAmountPaid());
                throw new RuntimeException("Failed to get payment intent from invoice - invoice has amount due but no payment intent");
            }
            
            log.info("✅ Payment Intent from subscription: id={}, status={}, clientSecret={}", 
                paymentIntent.getId(), paymentIntent.getStatus(), 
                paymentIntent.getClientSecret() != null ? "present" : "null");

            Map<String, String> result = new HashMap<>();
            result.put("clientSecret", paymentIntent.getClientSecret());
            result.put("subscriptionId", subscription.getId());
            result.put("customerId", customer.getId());
            result.put("paymentIntentId", paymentIntent.getId());
            
            log.info("✅ Subscription with Payment Intent created successfully");
            return result;

        } catch (StripeException e) {
            log.error("❌ Error creating Subscription with Payment Intent", e);
            throw new RuntimeException("Failed to create Subscription: " + e.getMessage(), e);
        }
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
                        
                        // Tentar pegar metadata da invoice primeiro
                        if (invoice.getMetadata() != null && !invoice.getMetadata().isEmpty()) {
                            metadata.putAll(invoice.getMetadata());
                        }
                        
                        // Se não tiver metadata na invoice, buscar da subscription
                        if (metadata.isEmpty() && invoice.getSubscription() != null) {
                            try {
                                if (apiKey != null && !apiKey.isEmpty()) {
                                    Stripe.apiKey = apiKey;
                                }
                                com.stripe.model.Subscription sub = com.stripe.model.Subscription.retrieve(invoice.getSubscription());
                                if (sub.getMetadata() != null && !sub.getMetadata().isEmpty()) {
                                    metadata.putAll(sub.getMetadata());
                                    log.info("✅ Metadata carregado da Subscription: {}", metadata);
                                }
                            } catch (Exception e) {
                                log.warn("Não foi possível buscar metadata da subscription: {}", e.getMessage());
                            }
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

            Map<String, Object> details = new HashMap<>();
            details.put("paymentIntentId", paymentIntent.getId());
            details.put("amount", BigDecimal.valueOf(paymentIntent.getAmount()).divide(BigDecimal.valueOf(100)));
            details.put("currency", paymentIntent.getCurrency().toUpperCase());
            details.put("status", paymentIntent.getStatus());
            details.put("created", LocalDateTime.ofInstant(
                Instant.ofEpochSecond(paymentIntent.getCreated()), 
                ZoneId.systemDefault()));
            details.put("metadata", paymentIntent.getMetadata());
            
            // Buscar payment method details (card info)
            if (paymentIntent.getPaymentMethod() != null) {
                try {
                    com.stripe.model.PaymentMethod paymentMethod = com.stripe.model.PaymentMethod.retrieve(
                        paymentIntent.getPaymentMethod()
                    );
                    details.put("paymentMethodType", paymentMethod.getType());
                    
                    if ("card".equals(paymentMethod.getType()) && paymentMethod.getCard() != null) {
                        details.put("cardBrand", paymentMethod.getCard().getBrand());
                        details.put("cardLast4", paymentMethod.getCard().getLast4());
                    }
                } catch (Exception e) {
                    log.warn("Could not retrieve payment method details", e);
                }
            }
            
            // Buscar charge associado ao Payment Intent (receipt URL)
            if (paymentIntent.getLatestCharge() != null) {
                try {
                    com.stripe.model.Charge charge = com.stripe.model.Charge.retrieve(paymentIntent.getLatestCharge());
                    details.put("receiptUrl", charge.getReceiptUrl());
                    details.put("receiptNumber", charge.getReceiptNumber());
                } catch (Exception e) {
                    log.warn("Could not retrieve charge details", e);
                }
            }
            
            // Buscar invoice associada (se houver)
            if (paymentIntent.getInvoice() != null) {
                try {
                    com.stripe.model.Invoice invoice = com.stripe.model.Invoice.retrieve(
                        paymentIntent.getInvoice()
                    );
                    details.put("invoiceId", invoice.getId());
                    details.put("invoiceNumber", invoice.getNumber());
                    details.put("invoicePdfUrl", invoice.getInvoicePdf());
                    details.put("invoiceUrl", invoice.getHostedInvoiceUrl());
                } catch (Exception e) {
                    log.warn("Could not retrieve invoice details", e);
                }
            }

            log.info("Payment Intent details retrieved successfully: cardBrand={}, cardLast4={}, invoiceNumber={}", 
                details.get("cardBrand"), details.get("cardLast4"), details.get("invoiceNumber"));
            return details;

        } catch (StripeException e) {
            log.error("Error fetching Payment Intent details", e);
            throw new RuntimeException("Failed to fetch Payment Intent details: " + e.getMessage(), e);
        }
    }

    /**
     * Atualizar subscription existente com proration automático
     * Usado para upgrades e downgrades
     */
    public Map<String, Object> updateSubscriptionWithProration(
            String subscriptionId, 
            String newPriceId, 
            Map<String, String> metadata) {
        
        if (apiKey != null && !apiKey.isEmpty()) {
            Stripe.apiKey = apiKey;
        }

        try {
            log.info("=== UPDATING SUBSCRIPTION WITH PRORATION ===");
            log.info("Subscription ID: {}", subscriptionId);
            log.info("New Price ID: {}", newPriceId);

            // 1. Buscar subscription atual
            com.stripe.model.Subscription currentSubscription = com.stripe.model.Subscription.retrieve(subscriptionId);
            log.info("Current subscription status: {}", currentSubscription.getStatus());
            
            String currentPriceId = currentSubscription.getItems().getData().get(0).getPrice().getId();
            log.info("Current Price ID: {}", currentPriceId);

            // Verificar se é realmente uma mudança
            if (currentPriceId.equals(newPriceId)) {
                log.warn("⚠️ New price is the same as current price. No update needed.");
                Map<String, Object> result = new HashMap<>();
                result.put("subscriptionId", subscriptionId);
                result.put("status", "no_change");
                result.put("message", "Subscription already on this plan");
                return result;
            }

            // 2. Atualizar subscription com proration
            Map<String, Object> params = new HashMap<>();
            
            // Atualizar item da subscription
            Map<String, Object> item = new HashMap<>();
            item.put("id", currentSubscription.getItems().getData().get(0).getId());
            item.put("price", newPriceId);
            params.put("items", Arrays.asList(item));
            
            // Configurar proration
            params.put("proration_behavior", "create_prorations");  // Criar proration automático
            params.put("billing_cycle_anchor", "unchanged");  // Manter data de cobrança
            
            // Atualizar metadata se fornecido
            if (metadata != null && !metadata.isEmpty()) {
                params.put("metadata", metadata);
            }

            // Expandir invoice para ver detalhes do proration
            params.put("expand", Arrays.asList("latest_invoice"));

            com.stripe.model.Subscription updatedSubscription = currentSubscription.update(params);
            
            log.info("✅ Subscription updated: id={}, status={}", 
                updatedSubscription.getId(), updatedSubscription.getStatus());

            // 3. Extrair informações da invoice de proration
            com.stripe.model.Invoice latestInvoice = updatedSubscription.getLatestInvoiceObject();
            
            Map<String, Object> result = new HashMap<>();
            result.put("subscriptionId", updatedSubscription.getId());
            result.put("status", updatedSubscription.getStatus());
            result.put("currentPeriodEnd", updatedSubscription.getCurrentPeriodEnd());
            
            if (latestInvoice != null) {
                result.put("invoiceId", latestInvoice.getId());
                result.put("invoiceUrl", latestInvoice.getHostedInvoiceUrl());
                result.put("amountDue", latestInvoice.getAmountDue());
                result.put("amountPaid", latestInvoice.getAmountPaid());
                result.put("total", latestInvoice.getTotal());
                
                log.info("📋 Proration Invoice:");
                log.info("  - Invoice ID: {}", latestInvoice.getId());
                log.info("  - Amount Due: {} cents", latestInvoice.getAmountDue());
                log.info("  - Total: {} cents", latestInvoice.getTotal());
                log.info("  - Status: {}", latestInvoice.getStatus());
                log.info("  - Invoice URL: {}", latestInvoice.getHostedInvoiceUrl());
            }

            log.info("=== SUBSCRIPTION UPDATE COMPLETED ===");
            return result;

        } catch (StripeException e) {
            log.error("❌ Error updating subscription with proration", e);
            throw new RuntimeException("Failed to update subscription: " + e.getMessage(), e);
        }
    }
}
