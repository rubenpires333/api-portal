package com.api_portal.backend.modules.subscription.controller;

import com.api_portal.backend.modules.subscription.dto.SubscriptionRequest;
import com.api_portal.backend.modules.subscription.dto.SubscriptionResponse;
import com.api_portal.backend.modules.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/consumer/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Consumer Subscriptions", description = "Gestao de subscricoes para consumidores")
@SecurityRequirement(name = "bearer-jwt")
public class ConsumerSubscriptionController {
    
    private final SubscriptionService subscriptionService;
    
    @GetMapping("/my")
    @Operation(summary = "Listar minhas subscricoes")
    public ResponseEntity<List<SubscriptionResponse>> getMySubscriptions(Authentication authentication) {
        return ResponseEntity.ok(subscriptionService.getMySubscriptionsList(authentication));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Detalhes de uma subscricao")
    public ResponseEntity<SubscriptionResponse> getSubscriptionById(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionById(id, authentication));
    }
    
    @GetMapping("/check/{apiId}")
    @Operation(summary = "Verificar se tenho subscricao ativa ou pendente para uma API")
    public ResponseEntity<Map<String, Object>> hasActiveSubscription(
            @PathVariable UUID apiId,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        SubscriptionResponse subscription = subscriptionService.getActiveOrPendingSubscriptionByApiId(apiId, authentication);
        
        if (subscription != null) {
            response.put("hasSubscription", true);
            response.put("subscription", subscription);
        } else {
            response.put("hasSubscription", false);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    @Operation(summary = "Criar nova subscricao")
    public ResponseEntity<SubscriptionResponse> subscribe(
            @Valid @RequestBody SubscriptionRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(subscriptionService.subscribe(request, authentication));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar subscricao")
    public ResponseEntity<Void> cancelSubscription(
            @PathVariable UUID id,
            Authentication authentication) {
        subscriptionService.cancelSubscription(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
