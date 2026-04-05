package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.dto.CheckoutSessionDTO;
import com.api_portal.backend.modules.billing.model.PlatformPlan;
import com.api_portal.backend.modules.billing.model.ProviderPlatformSubscription;
import com.api_portal.backend.modules.billing.repository.PlatformPlanRepository;
import com.api_portal.backend.modules.billing.service.CheckoutService;
import com.api_portal.backend.modules.billing.service.PlatformSubscriptionService;
import com.api_portal.backend.modules.user.domain.User;
import com.api_portal.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final CheckoutService checkoutService;
    private final PlatformPlanRepository planRepository;
    private final PlatformSubscriptionService platformSubscriptionService;
    private final UserRepository userRepository;

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

    @GetMapping("/subscription/current")
    public ResponseEntity<Map<String, Object>> getCurrentSubscription(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakId = jwt.getSubject();
        
        log.info("Buscando subscription para keycloakId: {}", keycloakId);
        
        // Buscar o usuário pelo keycloakId para obter o providerId
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElse(null);
        
        if (user == null) {
            log.warn("Usuário não encontrado para keycloakId: {}", keycloakId);
            Map<String, Object> response = new HashMap<>();
            response.put("planName", "STARTER");
            response.put("status", "active");
            response.put("isDefault", true);
            return ResponseEntity.ok(response);
        }
        
        UUID providerId = user.getId();
        log.info("Provider ID encontrado: {}", providerId);
        
        ProviderPlatformSubscription subscription = platformSubscriptionService.getSubscriptionByProviderId(providerId);
        
        if (subscription == null) {
            log.info("Nenhuma subscription encontrada, retornando STARTER como padrão");
            // Retornar plano STARTER como padrão
            Map<String, Object> response = new HashMap<>();
            response.put("planName", "STARTER");
            response.put("status", "active");
            response.put("isDefault", true);
            return ResponseEntity.ok(response);
        }
        
        log.info("Subscription encontrada: plan={}, status={}", 
            subscription.getPlan().getName(), subscription.getStatus());
        
        Map<String, Object> response = new HashMap<>();
        response.put("planName", subscription.getPlan().getName());
        response.put("displayName", subscription.getPlan().getDisplayName());
        response.put("status", subscription.getStatus());
        response.put("isDefault", false);
        response.put("monthlyPrice", subscription.getPlan().getMonthlyPrice());
        response.put("currency", subscription.getPlan().getCurrency());
        
        return ResponseEntity.ok(response);
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

    @PostMapping("/subscription/change-plan")
    public ResponseEntity<com.api_portal.backend.modules.billing.dto.UpgradeDowngradeResponse> changePlan(
            @RequestBody com.api_portal.backend.modules.billing.dto.UpgradeDowngradeRequest request) {
        
        log.info("Change plan request: providerId={}, newPlan={}", 
            request.getProviderId(), request.getNewPlanName());
        
        com.api_portal.backend.modules.billing.dto.UpgradeDowngradeResponse response = 
            platformSubscriptionService.upgradeOrDowngradePlan(
                request.getProviderId(), 
                request.getNewPlanName()
            );
        
        return ResponseEntity.ok(response);
    }
}
