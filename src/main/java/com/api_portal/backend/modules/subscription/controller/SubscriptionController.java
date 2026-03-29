package com.api_portal.backend.modules.subscription.controller;

import com.api_portal.backend.modules.subscription.domain.enums.SubscriptionStatus;
import com.api_portal.backend.modules.subscription.dto.RevokeRequest;
import com.api_portal.backend.modules.subscription.dto.SubscriptionRequest;
import com.api_portal.backend.modules.subscription.dto.SubscriptionResponse;
import com.api_portal.backend.modules.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Gestão de subscrições de APIs")
@SecurityRequirement(name = "bearer-jwt")
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    
    @PostMapping
    @Operation(summary = "Subscrever uma API (Consumer)")
    public ResponseEntity<SubscriptionResponse> subscribe(
            @Valid @RequestBody SubscriptionRequest request,
            Authentication authentication) {
        SubscriptionResponse response = subscriptionService.subscribe(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Listar minhas subscrições (Consumer)")
    public ResponseEntity<Page<SubscriptionResponse>> getMySubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SubscriptionResponse> subscriptions = subscriptionService.getMySubscriptions(authentication, pageable);
        return ResponseEntity.ok(subscriptions);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Detalhes de uma subscrição (Consumer)")
    public ResponseEntity<SubscriptionResponse> getSubscriptionById(
            @PathVariable UUID id,
            Authentication authentication) {
        SubscriptionResponse response = subscriptionService.getSubscriptionById(id, authentication);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar subscrição (Consumer)")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @PathVariable UUID id,
            Authentication authentication) {
        SubscriptionResponse response = subscriptionService.cancelSubscription(id, authentication);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/provider")
    @Operation(summary = "Listar subscrições das minhas APIs (Provider)")
    public ResponseEntity<Page<SubscriptionResponse>> getProviderSubscriptions(
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SubscriptionResponse> subscriptions = subscriptionService.getProviderSubscriptions(
            authentication, status, pageable);
        return ResponseEntity.ok(subscriptions);
    }
    
    @PutMapping("/provider/{id}/approve")
    @Operation(summary = "Aprovar subscrição (Provider)")
    public ResponseEntity<SubscriptionResponse> approveSubscription(
            @PathVariable UUID id,
            Authentication authentication) {
        SubscriptionResponse response = subscriptionService.approveSubscription(id, authentication);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/provider/{id}/revoke")
    @Operation(summary = "Revogar subscrição (Provider)")
    public ResponseEntity<SubscriptionResponse> revokeSubscription(
            @PathVariable UUID id,
            @Valid @RequestBody RevokeRequest request,
            Authentication authentication) {
        SubscriptionResponse response = subscriptionService.revokeSubscription(id, request, authentication);
        return ResponseEntity.ok(response);
    }
}
