package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.gateway.dto.WebhookEvent;
import com.api_portal.backend.modules.billing.model.PlatformPlan;
import com.api_portal.backend.modules.billing.model.ProviderPlatformSubscription;
import com.api_portal.backend.modules.billing.repository.PlatformPlanRepository;
import com.api_portal.backend.modules.billing.repository.ProviderPlatformSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformSubscriptionService {

    private final ProviderPlatformSubscriptionRepository subscriptionRepository;
    private final PlatformPlanRepository planRepository;

    @Transactional
    public void createOrUpdateSubscription(WebhookEvent event) {
        try {
            log.info("=== CREATING/UPDATING PLATFORM SUBSCRIPTION ===");
            log.info("Event ID: {}", event.getEventId());
            log.info("Event Type: {}", event.getEventType());
            log.info("Metadata: {}", event.getMetadata());
            
            // Extrair dados do metadata
            String planIdStr = event.getMetadata().get("planId");
            String providerIdStr = event.getMetadata().get("providerId");
            
            log.info("Extracted planId: {}", planIdStr);
            log.info("Extracted providerId: {}", providerIdStr);
            
            if (planIdStr == null || providerIdStr == null) {
                log.error("Missing planId or providerId in webhook metadata");
                return;
            }

            UUID planId = UUID.fromString(planIdStr);
            UUID providerId = UUID.fromString(providerIdStr);

            log.info("Parsed planId: {}", planId);
            log.info("Parsed providerId: {}", providerId);

            // Buscar plano
            PlatformPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));

            log.info("Plan found: {}", plan.getName());

            // Verificar se já existe assinatura para este provider
            ProviderPlatformSubscription subscription = subscriptionRepository
                .findByProviderId(providerId)
                .orElse(null);

            if (subscription == null) {
                // Criar nova assinatura
                subscription = ProviderPlatformSubscription.builder()
                    .providerId(providerId)
                    .plan(plan)
                    .stripeSubscriptionId(event.getSubscriptionId())
                    .stripeCustomerId(event.getCustomerId())
                    .status("active")
                    .currentPeriodStart(LocalDateTime.now())
                    .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                    .cancelAtPeriodEnd(false)
                    .build();
                
                log.info("Creating new platform subscription for provider: {}", providerId);
            } else {
                // Atualizar assinatura existente
                subscription.setPlan(plan);
                subscription.setStripeSubscriptionId(event.getSubscriptionId());
                subscription.setStripeCustomerId(event.getCustomerId());
                subscription.setStatus("active");
                subscription.setCurrentPeriodStart(LocalDateTime.now());
                subscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));
                
                log.info("Updating platform subscription for provider: {}", providerId);
            }

            subscriptionRepository.save(subscription);
            log.info("✅ Platform subscription saved successfully for provider: {}", providerId);
            log.info("=== END CREATING/UPDATING PLATFORM SUBSCRIPTION ===");
            
        } catch (Exception e) {
            log.error("❌ Error creating/updating platform subscription", e);
            throw e;
        }
    }

    public ProviderPlatformSubscription getSubscriptionByProviderId(UUID providerId) {
        return subscriptionRepository.findByProviderId(providerId).orElse(null);
    }
}
