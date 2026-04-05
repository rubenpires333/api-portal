package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.gateway.PaymentGateway;
import com.api_portal.backend.modules.billing.gateway.PaymentGatewayFactory;
import com.api_portal.backend.modules.billing.gateway.dto.WebhookEvent;
import com.api_portal.backend.modules.billing.model.PaymentWebhook;
import com.api_portal.backend.modules.billing.model.enums.GatewayType;
import com.api_portal.backend.modules.billing.repository.PaymentWebhookRepository;
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

            // Verificar se é evento de plataforma (tem planId no metadata)
            boolean isPlatformSubscription = event.getMetadata() != null && 
                event.getMetadata().containsKey("planId");

            log.info("Is Platform Subscription: {}", isPlatformSubscription);

            if (isPlatformSubscription) {
                log.info("Processing as PLATFORM subscription event");
                // Eventos de assinatura de plataforma
                if (eventType.contains("payment_intent.succeeded") || 
                    eventType.contains("invoice.payment_succeeded")) {
                    log.info("Calling platformSubscriptionService.createOrUpdateSubscription()");
                    platformSubscriptionService.createOrUpdateSubscription(event);
                } else {
                    log.info("Event type not handled for platform subscription: {}", eventType);
                }
            } else {
                log.info("Processing as API payment (revenue share) event");
                // Eventos de pagamento de API (revenue share)
                if (eventType.equals("payment_intent.succeeded") || 
                    eventType.equals("invoice.payment_succeeded")) {
                    handlePaymentSuccess(event);
                }
            }

            // Marcar como processado
            webhook.setProcessed(true);
            webhookRepository.save(webhook);
            
            log.info("✅ Webhook processed successfully: eventId={}", event.getEventId());
            log.info("=== END PROCESSING WEBHOOK EVENT ===");
            
        } catch (Exception e) {
            log.error("❌ Error processing webhook event: eventId={}, error={}", 
                event.getEventId(), e.getMessage(), e);
            // Não marcar como processado para permitir retry manual
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
