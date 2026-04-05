package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.gateway.PaymentGateway;
import com.api_portal.backend.modules.billing.gateway.PaymentGatewayFactory;
import com.api_portal.backend.modules.billing.gateway.dto.WebhookEvent;
import com.api_portal.backend.modules.billing.model.PaymentWebhook;
import com.api_portal.backend.modules.billing.model.enums.GatewayType;
import com.api_portal.backend.modules.billing.repository.PaymentWebhookRepository;
import com.api_portal.backend.modules.billing.service.CheckoutWebhookService;
import com.api_portal.backend.modules.billing.service.PlatformSubscriptionService;
import com.api_portal.backend.modules.billing.service.RevenueShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final PaymentGatewayFactory gatewayFactory;
    private final PaymentWebhookRepository webhookRepository;
    private final RevenueShareService revenueShareService;
    private final PlatformSubscriptionService platformSubscriptionService;
    private final CheckoutWebhookService checkoutWebhookService;

    @PostMapping("/{gateway}")
    public ResponseEntity<Void> handleWebhook(
            @PathVariable String gateway,
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature,
            @RequestHeader(value = "X-Vinti4-Signature", required = false) String vinti4Signature) {

        String eventId = null;
        
        try {
            PaymentGateway gw = gatewayFactory.get(gateway.toUpperCase());
            String signature = gateway.equalsIgnoreCase("STRIPE") ? stripeSignature : vinti4Signature;

            // Validar e parsear webhook
            WebhookEvent event = gw.parseWebhook(payload, signature);
            eventId = event.getEventId();

            // CAMADA 1: Verificação rápida em memória/cache (se já processado recentemente)
            // CAMADA 2: Verificação no banco de dados
            if (webhookRepository.existsByEventId(eventId)) {
                log.debug("Webhook already processed (idempotency check): eventId={}", eventId);
                return ResponseEntity.ok().build();
            }

            // CAMADA 3: Tentar salvar com tratamento de race condition
            PaymentWebhook webhook = PaymentWebhook.builder()
                .eventId(eventId)
                .gatewayType(GatewayType.valueOf(gateway.toUpperCase()))
                .eventType(event.getEventType())
                .payload(payload)
                .processed(false)
                .build();
            
            try {
                webhookRepository.saveAndFlush(webhook);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Race condition: outro thread já salvou este evento
                log.info("Webhook duplicate detected (race condition): eventId={}", eventId);
                return ResponseEntity.ok().build();
            }

            // Processar evento de forma assíncrona para responder rápido
            processWebhookEvent(event, webhook);

            // Responder imediatamente com sucesso
            return ResponseEntity.ok().build();
            
        } catch (SecurityException e) {
            log.error("Invalid webhook signature: eventId={}", eventId, e);
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            log.error("Error processing webhook: eventId={}", eventId, e);
            // Retornar 200 mesmo com erro para evitar retries infinitos
            // O webhook fica marcado como não processado para retry manual
            return ResponseEntity.ok().build();
        }
    }

    private void processWebhookEvent(WebhookEvent event, PaymentWebhook webhook) {
        try {
            String eventType = event.getEventType();
            log.info("=== PROCESSING WEBHOOK EVENT ===");
            log.info("Event Type: {}", eventType);
            log.info("Event ID: {}", event.getEventId());
            log.info("Metadata: {}", event.getMetadata());

            // FASE 3: Processar checkout.session.completed (Hosted Checkout)
            if (eventType.equals("checkout.session.completed")) {
                log.info("Processando checkout.session.completed via CheckoutWebhookService");
                checkoutWebhookService.processCheckoutCompleted(event);
                webhook.setProcessed(true);
                webhookRepository.save(webhook);
                log.info("✅ Checkout completed processado com sucesso");
                return;
            }

            // FASE 3: Processar payment_intent.succeeded (Embedded Payment)
            if (eventType.equals("payment_intent.succeeded")) {
                // Tentar processar como subscrição de plataforma
                // O CheckoutWebhookService vai verificar se existe CheckoutSession correspondente
                log.info("Tentando processar payment_intent.succeeded como subscrição de plataforma");
                try {
                    checkoutWebhookService.processPaymentIntentSucceeded(event);
                    webhook.setProcessed(true);
                    webhookRepository.save(webhook);
                    log.info("✅ Payment Intent succeeded processado como subscrição de plataforma");
                    return;
                } catch (IllegalStateException e) {
                    // Não é subscrição de plataforma, continuar processamento normal
                    log.info("Payment Intent não é subscrição de plataforma, processando como API payment");
                }
            }

            // FASE 3.1: Processar invoice.payment_succeeded (Subscriptions sem Payment Intent - free/trial)
            if (eventType.equals("invoice.payment_succeeded")) {
                // Tentar processar como nova subscrição de plataforma (sem Payment Intent)
                log.info("Tentando processar invoice.payment_succeeded como nova subscrição de plataforma");
                try {
                    checkoutWebhookService.processInvoicePaymentSucceeded(event);
                    webhook.setProcessed(true);
                    webhookRepository.save(webhook);
                    log.info("✅ Invoice payment succeeded processado como nova subscrição de plataforma");
                    return;
                } catch (IllegalStateException e) {
                    // Não é nova subscrição, pode ser renovação automática
                    log.info("Invoice não é nova subscrição, pode ser renovação automática ou API payment");
                }
            }

            // Verificar se é evento de plataforma (tem planId no metadata)
            boolean isPlatformSubscription = event.getMetadata() != null && 
                event.getMetadata().containsKey("planId");

            log.info("Is Platform Subscription: {}", isPlatformSubscription);

            if (isPlatformSubscription) {
                log.info("Processing as PLATFORM subscription event");
                if (eventType.contains("invoice.payment_succeeded")) {
                    log.info("Calling platformSubscriptionService.createOrUpdateSubscription() - Renovação automática");
                    platformSubscriptionService.createOrUpdateSubscription(event);
                } else if (eventType.contains("invoice.payment_failed")) {
                    log.info("Processando falha de pagamento de subscrição");
                    platformSubscriptionService.handlePaymentFailed(event);
                } else if (eventType.contains("customer.subscription.updated")) {
                    log.info("Processando atualização de subscrição");
                    platformSubscriptionService.handleSubscriptionUpdated(event);
                } else if (eventType.contains("customer.subscription.deleted")) {
                    log.info("Processando cancelamento de subscrição");
                    platformSubscriptionService.handleSubscriptionDeleted(event);
                } else {
                    log.info("Event type not handled for platform subscription: {}", eventType);
                }
            } else {
                log.info("Processing as API payment (revenue share) event");
                if (eventType.equals("payment_intent.succeeded") || 
                    eventType.equals("invoice.payment_succeeded")) {
                    handlePaymentSuccess(event);
                }
            }

            webhook.setProcessed(true);
            webhookRepository.save(webhook);
            
            log.info("✅ Webhook processed successfully: eventId={}", event.getEventId());
            log.info("=== END PROCESSING WEBHOOK EVENT ===");
            
        } catch (Exception e) {
            log.error("❌ Error processing webhook event: eventId={}, error={}", 
                event.getEventId(), e.getMessage(), e);
        }
    }

    private void handlePaymentSuccess(WebhookEvent event) {
        try {
            // Verificar se metadata existe e tem os campos necessários
            if (event.getMetadata() == null || 
                !event.getMetadata().containsKey("subscriptionId") ||
                !event.getMetadata().containsKey("providerId")) {
                log.warn("Payment success event missing required metadata: eventId={}", event.getEventId());
                return;
            }

            // Extrair dados do metadata para revenue share
            UUID subscriptionId = UUID.fromString(event.getMetadata().get("subscriptionId"));
            UUID providerId = UUID.fromString(event.getMetadata().get("providerId"));
            BigDecimal amount = event.getAmount();
            String currency = event.getCurrency();

            // Processar revenue share
            revenueShareService.processPayment(subscriptionId, providerId, amount, currency);
        } catch (Exception e) {
            log.error("Error handling payment success: eventId={}", event.getEventId(), e);
        }
    }
}
