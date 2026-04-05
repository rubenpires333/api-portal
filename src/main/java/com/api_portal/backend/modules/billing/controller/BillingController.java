package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.dto.CheckoutSessionDTO;
import com.api_portal.backend.modules.billing.model.PlatformPlan;
import com.api_portal.backend.modules.billing.repository.PlatformPlanRepository;
import com.api_portal.backend.modules.billing.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final CheckoutService checkoutService;
    private final PlatformPlanRepository planRepository;

    @Value("${billing.stripe.publishable-key:}")
    private String stripePublishableKey;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "ok");
        health.put("message", "Billing system is operational");
        return ResponseEntity.ok(health);
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("publishableKey", stripePublishableKey);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/plans")
    public ResponseEntity<List<PlatformPlan>> getAvailablePlans() {
        List<PlatformPlan> plans = planRepository.findByActiveTrueOrderByDisplayOrderAsc();
        return ResponseEntity.ok(plans);
    }

    @PostMapping("/checkout/platform")
    public ResponseEntity<CheckoutSessionDTO> createPlatformSubscriptionCheckout(
            @RequestBody Map<String, String> request) {
        
        UUID providerId = UUID.fromString(request.get("providerId"));
        String planName = request.get("planName");
        
        CheckoutSessionDTO session = checkoutService.createPlatformSubscriptionCheckout(providerId, planName);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/payment-intent/platform")
    public ResponseEntity<Map<String, String>> createPlatformPaymentIntent(
            @RequestBody Map<String, String> request) {
        
        UUID providerId = UUID.fromString(request.get("providerId"));
        String planName = request.get("planName");
        
        Map<String, String> paymentIntent = checkoutService.createPlatformPaymentIntent(providerId, planName);
        return ResponseEntity.ok(paymentIntent);
    }

    @PostMapping("/checkout/cancel")
    public ResponseEntity<Void> cancelCheckout(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        checkoutService.cancelCheckoutSession(sessionId);
        return ResponseEntity.ok().build();
    }
}
