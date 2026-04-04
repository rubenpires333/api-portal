package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.gateway.PaymentGateway;
import com.api_portal.backend.modules.billing.gateway.PaymentGatewayFactory;
import com.api_portal.backend.modules.billing.gateway.dto.WebhookEvent;
import com.api_portal.backend.modules.billing.model.PaymentWebhook;
import com.api_portal.backend.modules.billing.model.enums.GatewayType;
import com.api_portal.backend.modules.billing.repository.PaymentWebhookRepository;
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

    @PostMapping("/{gateway}")
    public ResponseEntity<Void> handleWebhook(
            @PathVariable String gateway,
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature,
            @RequestHeader(value = "X-Vinti4-Signature", required = false) String vinti4Signature) {

        try {
            PaymentGateway gw = gatewayFactory.get(gateway.toUpperCase());
            String signature = gateway.equalsIgnoreCase("STRIPE") ? stripeSignature : vinti4Signature;

            // Validar e parsear webhook
            WebhookEvent event = gw.parseWebhook(payload, signature);

            // Verificar idempotência
            if (webhookRepository.existsByEventId(event.getEventId())) {
                log.info("Webhook already processed: {}", event.getEventId());
                return ResponseEntity.ok().build();
            }

            // Salvar webhook
            PaymentWebhook webhook = PaymentWebhook.builder()
                .eventId(event.getEventId())
                .gatewayType(GatewayType.valueOf(gateway.toUpperCase()))
                .eventType(event.getEventType())
                .payload(payload)
                .processed(false)
                .build();
            webhookRepository.save(webhook);

            // Processar evento
            processWebhookEvent(event, webhook);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.badRequest().build();
        }
    }

    private void processWebhookEvent(WebhookEvent event, PaymentWebhook webhook) {
        try {
            switch (event.getEventType()) {
                case "payment_intent.succeeded":
                case "invoice.payment_succeeded":
                    handlePaymentSuccess(event);
                    break;
                case "subscription.created":
                case "subscription.updated":
                    handleSubscriptionUpdate(event);
                    break;
                default:
                    log.info("Unhandled event type: {}", event.getEventType());
            }

            webhook.setProcessed(true);
            webhookRepository.save(webhook);
        } catch (Exception e) {
            log.error("Error processing webhook event: {}", event.getEventId(), e);
        }
    }

    private void handlePaymentSuccess(WebhookEvent event) {
        // Extrair dados do metadata
        UUID subscriptionId = UUID.fromString(event.getMetadata().get("subscriptionId"));
        UUID providerId = UUID.fromString(event.getMetadata().get("providerId"));
        BigDecimal amount = event.getAmount();
        String currency = event.getCurrency();

        // Processar revenue share
        revenueShareService.processPayment(subscriptionId, providerId, amount, currency);
    }

    private void handleSubscriptionUpdate(WebhookEvent event) {
        // TODO: Atualizar status da subscription
        log.info("Subscription update: {}", event.getSubscriptionId());
    }
}
