package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.dto.PaymentHistoryDTO;
import com.api_portal.backend.modules.billing.dto.SubscriptionActionDTO;
import com.api_portal.backend.modules.billing.dto.SubscriptionStatusDTO;
import com.api_portal.backend.modules.billing.service.SubscriptionManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/provider/subscription")
@RequiredArgsConstructor
@Tag(name = "Subscription Management", description = "Gerenciamento de subscrições do provider")
public class SubscriptionManagementController {
    
    private final SubscriptionManagementService subscriptionService;
    
    /**
     * Obter status da subscrição atual
     */
    @GetMapping("/status/{providerId}")
    @Operation(summary = "Obter status da subscrição")
    public ResponseEntity<SubscriptionStatusDTO> getSubscriptionStatus(@PathVariable UUID providerId) {
        log.info("Provider {} solicitando status da subscrição", providerId);
        
        SubscriptionStatusDTO status = subscriptionService.getSubscriptionStatus(providerId);
        return ResponseEntity.ok(status);
    }
    
    /**
     * Cancelar subscrição
     */
    @PostMapping("/cancel/{providerId}")
    @Operation(summary = "Cancelar subscrição")
    public ResponseEntity<SubscriptionStatusDTO> cancelSubscription(
            @PathVariable UUID providerId,
            @RequestBody SubscriptionActionDTO actionDTO) {
        log.info("Provider {} solicitando cancelamento de subscrição", providerId);
        
        SubscriptionStatusDTO status = subscriptionService.cancelSubscription(providerId, actionDTO);
        return ResponseEntity.ok(status);
    }
    
    /**
     * Alterar plano (upgrade/downgrade)
     */
    @PostMapping("/change-plan/{providerId}")
    @Operation(summary = "Alterar plano")
    public ResponseEntity<SubscriptionStatusDTO> changePlan(
            @PathVariable UUID providerId,
            @RequestBody SubscriptionActionDTO actionDTO) {
        log.info("Provider {} solicitando alteração de plano", providerId);
        
        SubscriptionStatusDTO status = subscriptionService.changePlan(providerId, actionDTO);
        return ResponseEntity.ok(status);
    }
    
    /**
     * Obter histórico de pagamentos
     */
    @GetMapping("/payment-history/{providerId}")
    @Operation(summary = "Obter histórico de pagamentos")
    public ResponseEntity<List<PaymentHistoryDTO>> getPaymentHistory(@PathVariable UUID providerId) {
        log.info("Provider {} solicitando histórico de pagamentos", providerId);
        
        List<PaymentHistoryDTO> history = subscriptionService.getPaymentHistory(providerId);
        return ResponseEntity.ok(history);
    }
}
