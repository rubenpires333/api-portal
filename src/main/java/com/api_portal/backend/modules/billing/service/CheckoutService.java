package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.dto.CheckoutSessionDTO;
import com.api_portal.backend.modules.billing.gateway.PaymentGateway;
import com.api_portal.backend.modules.billing.gateway.PaymentGatewayFactory;
import com.api_portal.backend.modules.billing.gateway.dto.CheckoutRequest;
import com.api_portal.backend.modules.billing.gateway.dto.CheckoutSession;
import com.api_portal.backend.modules.billing.model.PlatformPlan;
import com.api_portal.backend.modules.billing.repository.PlatformPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final PaymentGatewayFactory gatewayFactory;
    private final PlatformPlanRepository planRepository;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public CheckoutSessionDTO createPlatformSubscriptionCheckout(UUID providerId, String planName) {
        PlatformPlan plan = planRepository.findByName(planName.toUpperCase())
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planName));

        PaymentGateway gateway = gatewayFactory.getActive();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("providerId", providerId.toString());
        metadata.put("planId", plan.getId().toString());
        metadata.put("type", "platform_subscription");

        CheckoutRequest request = CheckoutRequest.builder()
            .orderId(UUID.randomUUID().toString())
            .amount(plan.getMonthlyPrice())
            .currency(plan.getCurrency())
            .gatewayPriceId(getGatewayPriceId(plan, gateway.getType()))
            .successUrl(frontendUrl + "/billing/success")
            .cancelUrl(frontendUrl + "/billing/cancel")
            .webhookUrl(frontendUrl + "/api/v1/webhooks/" + gateway.getType().toLowerCase())
            .metadata(metadata)
            .build();

        CheckoutSession session = gateway.createCheckoutSession(request);

        return CheckoutSessionDTO.builder()
            .sessionId(session.getSessionId())
            .checkoutUrl(session.getCheckoutUrl())
            .planName(plan.getDisplayName())
            .amount(plan.getMonthlyPrice().toString())
            .currency(plan.getCurrency())
            .build();
    }

    private String getGatewayPriceId(PlatformPlan plan, String gatewayType) {
        return switch (gatewayType) {
            case "STRIPE" -> plan.getStripePriceId();
            case "VINTI4" -> plan.getVinti4PriceId();
            default -> throw new IllegalArgumentException("Unsupported gateway: " + gatewayType);
        };
    }

    public Map<String, String> createPlatformPaymentIntent(UUID providerId, String planName) {
        PlatformPlan plan = planRepository.findByName(planName.toUpperCase())
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planName));

        PaymentGateway gateway = gatewayFactory.getActive();
        
        if (!"STRIPE".equals(gateway.getType())) {
            throw new IllegalArgumentException("Payment Intent only supported for Stripe gateway");
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("providerId", providerId.toString());
        metadata.put("planId", plan.getId().toString());
        metadata.put("planName", plan.getName());
        metadata.put("type", "platform_subscription");

        log.info("=== CREATING PAYMENT INTENT ===");
        log.info("Provider ID: {}", providerId);
        log.info("Plan ID: {}", plan.getId());
        log.info("Plan Name: {}", plan.getName());
        log.info("Metadata: {}", metadata);

        // Cast para StripeGateway para acessar método específico
        com.api_portal.backend.modules.billing.gateway.stripe.StripeGateway stripeGateway = 
            (com.api_portal.backend.modules.billing.gateway.stripe.StripeGateway) gateway;

        Map<String, String> result = stripeGateway.createPaymentIntent(plan.getMonthlyPrice(), plan.getCurrency(), metadata);
        
        log.info("Payment Intent Result: {}", result);
        log.info("=== END CREATING PAYMENT INTENT ===");
        
        return result;
    }

    public Map<String, Object> getPaymentReceipt(String paymentIntentId) {
        PaymentGateway gateway = gatewayFactory.getActive();
        
        if (!"STRIPE".equals(gateway.getType())) {
            throw new IllegalArgumentException("Receipt only supported for Stripe gateway");
        }

        com.api_portal.backend.modules.billing.gateway.stripe.StripeGateway stripeGateway = 
            (com.api_portal.backend.modules.billing.gateway.stripe.StripeGateway) gateway;

        return stripeGateway.getPaymentIntentDetails(paymentIntentId);
    }
}
