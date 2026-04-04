package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.dto.CheckoutSessionDTO;
import com.api_portal.backend.modules.billing.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final CheckoutService checkoutService;

    @PostMapping("/checkout/platform")
    public ResponseEntity<CheckoutSessionDTO> createPlatformSubscriptionCheckout(
            @RequestParam UUID providerId,
            @RequestParam String planName) {
        
        CheckoutSessionDTO session = checkoutService.createPlatformSubscriptionCheckout(providerId, planName);
        return ResponseEntity.ok(session);
    }
}
