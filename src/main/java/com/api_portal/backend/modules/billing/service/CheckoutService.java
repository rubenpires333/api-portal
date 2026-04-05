package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.dto.CheckoutSessionDTO;
import com.api_portal.backend.modules.billing.gateway.PaymentGateway;
import com.api_portal.backend.modules.billing.gateway.PaymentGatewayFactory;
import com.api_portal.backend.modules.billing.gateway.dto.CheckoutRequest;
import com.api_portal.backend.modules.billing.model.CheckoutSession;
import com.api_portal.backend.modules.billing.model.PlatformPlan;
import com.api_portal.backend.modules.billing.model.enums.CheckoutSessionStatus;
import com.api_portal.backend.modules.billing.repository.CheckoutSessionRepository;
import com.api_portal.backend.modules.billing.repository.PlatformPlanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final PaymentGatewayFactory gatewayFactory;
    private final PlatformPlanRepository planRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional
    public CheckoutSessionDTO createPlatformSubscriptionCheckout(UUID providerId, String planName) {
        log.info("Iniciando criação de checkout: providerId={}, planName={}", providerId, planName);
        
        PlatformPlan plan = planRepository.findByName(planName.toUpperCase())
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planName));

        CheckoutSession localSession = CheckoutSession.builder()
            .providerId(providerId)
            .planId(plan.getId())
            .planName(plan.getName())
            .amount(plan.getMonthlyPrice())
            .currency(plan.getCurrency())
            .status(CheckoutSessionStatus.PENDING)
            .build();
        
        localSession = checkoutSessionRepository.save(localSession);
        log.info("Sessão local criada: id={}, status=PENDING", localSession.getId());

        PaymentGateway gateway = gatewayFactory.getActive();
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("providerId", providerId.toString());
        metadata.put("planId", plan.getId().toString());
        metadata.put("planName", plan.getName());
        metadata.put("localSessionId", localSession.getId().toString());
        metadata.put("type", "platform_subscription");

        CheckoutRequest request = CheckoutRequest.builder()
            .orderId(localSession.getId().toString())
            .amount(plan.getMonthlyPrice())
            .currency(plan.getCurrency())
            .gatewayPriceId(getGatewayPriceId(plan, gateway.getType()))
            .successUrl(frontendUrl + "/billing/success?session_id={CHECKOUT_SESSION_ID}")
            .cancelUrl(frontendUrl + "/billing/cancel")
            .webhookUrl(frontendUrl + "/api/v1/webhooks/" + gateway.getType().toLowerCase())
            .metadata(metadata)
            .build();

        com.api_portal.backend.modules.billing.gateway.dto.CheckoutSession stripeSession = 
            gateway.createCheckoutSession(request);

        localSession.setStripeSessionId(stripeSession.getSessionId());
        try {
            localSession.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            log.warn("Erro ao serializar metadata", e);
        }
        checkoutSessionRepository.save(localSession);
        
        log.info("Sessão Stripe criada: stripeSessionId={}, localId={}", 
                 stripeSession.getSessionId(), localSession.getId());

        return CheckoutSessionDTO.builder()
            .sessionId(stripeSession.getSessionId())
            .checkoutUrl(stripeSession.getCheckoutUrl())
            .planName(plan.getDisplayName())
            .amount(plan.getMonthlyPrice().toString())
            .currency(plan.getCurrency())
            .build();
    }

    @Transactional
    public Map<String, String> createPlatformPaymentIntent(UUID providerId, String planName) {
        log.info("Criando Payment Intent (Embedded): providerId={}, planName={}", providerId, planName);
        
        PlatformPlan plan = planRepository.findByName(planName.toUpperCase())
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planName));

        CheckoutSession localSession = CheckoutSession.builder()
            .providerId(providerId)
            .planId(plan.getId())
            .planName(plan.getName())
            .amount(plan.getMonthlyPrice())
            .currency(plan.getCurrency())
            .status(CheckoutSessionStatus.PENDING)
            .build();
        
        localSession = checkoutSessionRepository.save(localSession);
        log.info("Sessão local criada para Payment Intent: id={}", localSession.getId());

        PaymentGateway gateway = gatewayFactory.getActive();
        
        if (!"STRIPE".equals(gateway.getType())) {
            throw new IllegalArgumentException("Payment Intent only supported for Stripe");
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("providerId", providerId.toString());
        metadata.put("planId", plan.getId().toString());
        metadata.put("planName", plan.getName());
        metadata.put("localSessionId", localSession.getId().toString());
        metadata.put("type", "platform_subscription");

        com.api_portal.backend.modules.billing.gateway.stripe.StripeGateway stripeGateway = 
            (com.api_portal.backend.modules.billing.gateway.stripe.StripeGateway) gateway;

        Map<String, String> result = stripeGateway.createPaymentIntent(
            plan.getMonthlyPrice(), 
            plan.getCurrency(), 
            metadata
        );
        
        localSession.setStripePaymentIntentId(result.get("paymentIntentId"));
        try {
            localSession.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            log.warn("Erro ao serializar metadata", e);
        }
        checkoutSessionRepository.save(localSession);
        
        log.info("Payment Intent criado: paymentIntentId={}, localSessionId={}", 
                 result.get("paymentIntentId"), localSession.getId());
        
        return result;
    }

    @Transactional
    public void cancelCheckoutSession(String stripeSessionId) {
        log.info("Cancelando sessão: stripeSessionId={}", stripeSessionId);
        
        CheckoutSession session = checkoutSessionRepository.findByStripeSessionId(stripeSessionId)
            .orElseThrow(() -> new IllegalArgumentException("Checkout session not found"));
        
        if (session.getStatus() != CheckoutSessionStatus.PENDING) {
            log.warn("Tentativa de cancelar sessão em status inválido: status={}", session.getStatus());
            return;
        }
        
        session.setStatus(CheckoutSessionStatus.CANCELLED);
        session.setCancelledAt(java.time.LocalDateTime.now());
        checkoutSessionRepository.save(session);
        
        log.info("Sessão cancelada: id={}", session.getId());
    }

    @Transactional
    public void expireOldSessions() {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusMinutes(30);
        
        List<CheckoutSession> oldSessions = checkoutSessionRepository
            .findByStatusAndCreatedAtBefore(CheckoutSessionStatus.PENDING, cutoff);
        
        for (CheckoutSession session : oldSessions) {
            session.setStatus(CheckoutSessionStatus.EXPIRED);
            session.setExpiredAt(java.time.LocalDateTime.now());
            checkoutSessionRepository.save(session);
            log.info("Sessão expirada: id={}", session.getId());
        }
        
        if (!oldSessions.isEmpty()) {
            log.info("Total de sessões expiradas: {}", oldSessions.size());
        }
    }

    private String getGatewayPriceId(PlatformPlan plan, String gatewayType) {
        return switch (gatewayType) {
            case "STRIPE" -> plan.getStripePriceId();
            case "VINTI4" -> plan.getVinti4PriceId();
            default -> throw new IllegalArgumentException("Unsupported gateway: " + gatewayType);
        };
    }
}
