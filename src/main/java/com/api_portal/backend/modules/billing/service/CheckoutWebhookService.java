package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.gateway.dto.WebhookEvent;
import com.api_portal.backend.modules.billing.gateway.stripe.StripeGateway;
import com.api_portal.backend.modules.billing.model.CheckoutSession;
import com.api_portal.backend.modules.billing.model.enums.CheckoutSessionStatus;
import com.api_portal.backend.modules.billing.repository.CheckoutSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutWebhookService {

    private final CheckoutSessionRepository checkoutSessionRepository;
    private final PlatformSubscriptionService platformSubscriptionService;
    private final StripeGateway stripeGateway;

    @Transactional
    public void processCheckoutCompleted(WebhookEvent event) {
        log.info("=== PROCESSANDO CHECKOUT COMPLETED (WEBHOOK) ===");
        log.info("Event ID: {}", event.getEventId());
        
        String localSessionIdStr = event.getMetadata().get("localSessionId");
        if (localSessionIdStr == null) {
            log.error("Metadata localSessionId não encontrado no webhook");
            return;
        }
        
        UUID localSessionId = UUID.fromString(localSessionIdStr);
        CheckoutSession session = checkoutSessionRepository.findById(localSessionId)
            .orElseThrow(() -> new IllegalStateException("Checkout session not found: " + localSessionId));
        
        log.info("Sessão local encontrada: id={}, status={}", session.getId(), session.getStatus());
        
        if (session.getStatus() == CheckoutSessionStatus.COMPLETED) {
            log.warn("Sessão já foi processada anteriormente: id={}", session.getId());
            return;
        }
        
        if (session.getStatus() != CheckoutSessionStatus.PENDING) {
            log.error("Sessão em estado inválido para completar: status={}", session.getStatus());
            return;
        }
        
        session.setStripeCustomerId(event.getCustomerId());
        session.setStripeSubscriptionId(event.getSubscriptionId());
        session.setStripePaymentIntentId(event.getPaymentId());
        session.setStatus(CheckoutSessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        checkoutSessionRepository.save(session);
        
        log.info("Sessão atualizada para COMPLETED: id={}", session.getId());
        
        try {
            platformSubscriptionService.activateSubscription(session, event);
            log.info("✅ Subscrição ativada com sucesso: sessionId={}", session.getId());
        } catch (Exception e) {
            log.error("❌ Erro ao ativar subscrição: sessionId={}", session.getId(), e);
            session.setStatus(CheckoutSessionStatus.FAILED);
            session.setFailureReason(e.getMessage());
            checkoutSessionRepository.save(session);
            throw e;
        }
        
        log.info("=== FIM PROCESSAMENTO CHECKOUT COMPLETED ===");
    }
    
    @Transactional
    public void processPaymentIntentSucceeded(WebhookEvent event) {
        log.info("=== PROCESSANDO PAYMENT INTENT SUCCEEDED (WEBHOOK) ===");
        log.info("Event ID: {}", event.getEventId());
        log.info("Payment Intent ID: {}", event.getPaymentId());
        log.info("Subscription ID from event: {}", event.getSubscriptionId());
        log.info("Customer ID from event: {}", event.getCustomerId());
        log.info("Metadata from webhook: {}", event.getMetadata());
        
        // ESTRATÉGIA 1: Tentar usar metadata do webhook
        String localSessionIdStr = event.getMetadata() != null ? event.getMetadata().get("localSessionId") : null;
        
        CheckoutSession session = null;
        
        if (localSessionIdStr != null) {
            log.info("Estratégia 1: Buscando sessão por localSessionId do metadata: {}", localSessionIdStr);
            UUID localSessionId = UUID.fromString(localSessionIdStr);
            session = checkoutSessionRepository.findById(localSessionId).orElse(null);
        }
        
        // ESTRATÉGIA 2: Se metadata não tem localSessionId, buscar por Payment Intent ID
        if (session == null && event.getPaymentId() != null) {
            log.info("Estratégia 2: Buscando sessão por Payment Intent ID: {}", event.getPaymentId());
            session = checkoutSessionRepository.findByStripePaymentIntentId(event.getPaymentId()).orElse(null);
        }
        
        // ESTRATÉGIA 3: Buscar por Subscription ID (novo)
        if (session == null && event.getSubscriptionId() != null) {
            log.info("Estratégia 3: Buscando sessão por Subscription ID: {}", event.getSubscriptionId());
            session = checkoutSessionRepository.findByStripeSubscriptionId(event.getSubscriptionId()).orElse(null);
        }
        
        if (session == null) {
            log.info("Checkout session não encontrada para Payment Intent: {}. Não é subscrição de plataforma.", 
                event.getPaymentId());
            throw new IllegalStateException("Not a platform subscription payment");
        }
        
        log.info("✅ Sessão local encontrada: id={}, status={}", session.getId(), session.getStatus());
        
        if (session.getStatus() == CheckoutSessionStatus.COMPLETED) {
            log.warn("Sessão já foi processada anteriormente: id={}", session.getId());
            return;
        }
        
        if (session.getStatus() != CheckoutSessionStatus.PENDING) {
            log.error("Sessão em estado inválido para completar: status={}", session.getStatus());
            return;
        }
        
        // Atualizar session com IDs do evento (fallback se não tiver)
        if (session.getStripeCustomerId() == null && event.getCustomerId() != null) {
            session.setStripeCustomerId(event.getCustomerId());
            log.info("✅ Customer ID atualizado do evento: {}", event.getCustomerId());
        }
        if (session.getStripeSubscriptionId() == null && event.getSubscriptionId() != null) {
            session.setStripeSubscriptionId(event.getSubscriptionId());
            log.info("✅ Subscription ID atualizado do evento: {}", event.getSubscriptionId());
        }
        
        session.setStripePaymentIntentId(event.getPaymentId());
        session.setStatus(CheckoutSessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        checkoutSessionRepository.save(session);
        
        log.info("Sessão atualizada para COMPLETED: id={}", session.getId());
        
        // Capturar detalhes do pagamento do Stripe
        Map<String, Object> paymentDetails = null;
        if (event.getPaymentId() != null) {
            try {
                log.info("Buscando detalhes do pagamento do Stripe...");
                paymentDetails = stripeGateway.getPaymentIntentDetails(event.getPaymentId());
                log.info("✅ Detalhes do pagamento capturados: cardBrand={}, cardLast4={}, invoiceNumber={}", 
                    paymentDetails.get("cardBrand"), paymentDetails.get("cardLast4"), paymentDetails.get("invoiceNumber"));
            } catch (Exception e) {
                log.warn("⚠️ Não foi possível buscar detalhes do pagamento: {}", e.getMessage());
                // Continuar sem os detalhes - não é crítico
            }
        }
        
        try {
            platformSubscriptionService.activateSubscription(session, event, paymentDetails);
            log.info("✅ Subscrição ativada com sucesso: sessionId={}", session.getId());
        } catch (Exception e) {
            log.error("❌ Erro ao ativar subscrição: sessionId={}", session.getId(), e);
            session.setStatus(CheckoutSessionStatus.FAILED);
            session.setFailureReason(e.getMessage());
            checkoutSessionRepository.save(session);
            throw e;
        }
        
        log.info("=== FIM PROCESSAMENTO PAYMENT INTENT SUCCEEDED ===");
    }

    @Transactional
    public void processInvoicePaymentSucceeded(WebhookEvent event) {
        log.info("=== PROCESSANDO INVOICE PAYMENT SUCCEEDED (WEBHOOK) ===");
        log.info("Event ID: {}", event.getEventId());
        log.info("Invoice ID: {}", event.getPaymentId());
        log.info("Subscription ID from event: {}", event.getSubscriptionId());
        log.info("Customer ID from event: {}", event.getCustomerId());
        log.info("Metadata from webhook: {}", event.getMetadata());
        
        // Buscar sessão por Subscription ID (subscriptions sem Payment Intent)
        CheckoutSession session = null;
        
        if (event.getSubscriptionId() != null) {
            log.info("Buscando sessão por Subscription ID: {}", event.getSubscriptionId());
            session = checkoutSessionRepository.findByStripeSubscriptionId(event.getSubscriptionId()).orElse(null);
        }
        
        // Fallback: buscar por metadata
        if (session == null && event.getMetadata() != null) {
            String localSessionIdStr = event.getMetadata().get("localSessionId");
            if (localSessionIdStr != null) {
                log.info("Buscando sessão por localSessionId do metadata: {}", localSessionIdStr);
                UUID localSessionId = UUID.fromString(localSessionIdStr);
                session = checkoutSessionRepository.findById(localSessionId).orElse(null);
            }
        }
        
        if (session == null) {
            log.info("Checkout session não encontrada para Invoice. Pode ser renovação automática.");
            throw new IllegalStateException("Not a new platform subscription - may be renewal");
        }
        
        log.info("✅ Sessão local encontrada: id={}, status={}", session.getId(), session.getStatus());
        
        if (session.getStatus() == CheckoutSessionStatus.COMPLETED) {
            log.warn("Sessão já foi processada anteriormente: id={}", session.getId());
            return;
        }
        
        if (session.getStatus() != CheckoutSessionStatus.PENDING) {
            log.error("Sessão em estado inválido para completar: status={}", session.getStatus());
            return;
        }
        
        // Atualizar session com IDs do evento
        session.setStripeCustomerId(event.getCustomerId());
        session.setStripeSubscriptionId(event.getSubscriptionId());
        session.setStatus(CheckoutSessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        checkoutSessionRepository.save(session);
        
        log.info("Sessão atualizada para COMPLETED: id={}", session.getId());
        
        try {
            platformSubscriptionService.activateSubscription(session, event);
            log.info("✅ Subscrição ativada com sucesso: sessionId={}", session.getId());
        } catch (Exception e) {
            log.error("❌ Erro ao ativar subscrição: sessionId={}", session.getId(), e);
            session.setStatus(CheckoutSessionStatus.FAILED);
            session.setFailureReason(e.getMessage());
            checkoutSessionRepository.save(session);
            throw e;
        }
        
        log.info("=== FIM PROCESSAMENTO INVOICE PAYMENT SUCCEEDED ===");
    }

    @Transactional
    public void processCheckoutCancelled(String stripeSessionId) {
        log.info("Processando cancelamento de checkout: stripeSessionId={}", stripeSessionId);
        
        checkoutSessionRepository.findByStripeSessionId(stripeSessionId)
            .ifPresent(session -> {
                if (session.getStatus() == CheckoutSessionStatus.PENDING) {
                    session.setStatus(CheckoutSessionStatus.CANCELLED);
                    session.setCancelledAt(LocalDateTime.now());
                    checkoutSessionRepository.save(session);
                    log.info("Sessão cancelada: id={}", session.getId());
                }
            });
    }
}
