package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.gateway.dto.WebhookEvent;
import com.api_portal.backend.modules.billing.model.PlatformPlan;
import com.api_portal.backend.modules.billing.model.ProviderPlatformSubscription;
import com.api_portal.backend.modules.billing.repository.PlatformPlanRepository;
import com.api_portal.backend.modules.billing.repository.ProviderPlatformSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformSubscriptionService {

    private final ProviderPlatformSubscriptionRepository subscriptionRepository;
    private final PlatformPlanRepository planRepository;
    private final RevenueShareService revenueShareService;
    
    @Value("${billing.holdback-days:14}")
    private int holdbackDays;

    @Transactional
    public void createOrUpdateSubscription(WebhookEvent event) {
        try {
            log.info("=== CREATING/UPDATING PLATFORM SUBSCRIPTION ===");
            log.info("Event ID: {}", event.getEventId());
            log.info("Event Type: {}", event.getEventType());
            log.info("Metadata: {}", event.getMetadata());
            
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

            PlatformPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));

            log.info("Plan found: {}", plan.getName());

            ProviderPlatformSubscription subscription = subscriptionRepository
                .findByProviderId(providerId)
                .orElse(null);

            if (subscription == null) {
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

    /**
     * Ativar subscrição após confirmação do webhook
     * Este método é chamado APENAS pelo CheckoutWebhookService
     * 
     * Responsabilidades:
     * 1. Criar/atualizar subscrição ✅
     * 2. Creditar wallet da plataforma (100% do valor) ✅
     * 3. Registar transação com holdback de 14 dias ✅
     */
    @Transactional
    public void activateSubscription(com.api_portal.backend.modules.billing.model.CheckoutSession session, 
                                     WebhookEvent event) {
        log.info("Ativando subscrição: sessionId={}, providerId={}", session.getId(), session.getProviderId());
        
        PlatformPlan plan = planRepository.findById(session.getPlanId())
            .orElseThrow(() -> new RuntimeException("Plan not found: " + session.getPlanId()));
        
        ProviderPlatformSubscription subscription = subscriptionRepository
            .findByProviderId(session.getProviderId())
            .orElse(null);
        
        if (subscription == null) {
            subscription = ProviderPlatformSubscription.builder()
                .providerId(session.getProviderId())
                .plan(plan)
                .stripeSubscriptionId(session.getStripeSubscriptionId())
                .stripeCustomerId(session.getStripeCustomerId())
                .status("active")
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                .cancelAtPeriodEnd(false)
                .build();
            
            log.info("Nova subscrição criada: providerId={}, plan={}", 
                     session.getProviderId(), plan.getName());
        } else {
            subscription.setPlan(plan);
            subscription.setStripeSubscriptionId(session.getStripeSubscriptionId());
            subscription.setStripeCustomerId(session.getStripeCustomerId());
            subscription.setStatus("active");
            subscription.setCurrentPeriodStart(LocalDateTime.now());
            subscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));
            
            log.info("Subscrição atualizada: providerId={}, plan={}", 
                     session.getProviderId(), plan.getName());
        }
        
        subscriptionRepository.save(subscription);
        
        // Creditar wallet da PLATAFORMA (não do provider)
        // A plataforma recebe 100% do valor da subscrição
        // Este é um pagamento de provider para plataforma, não há revenue share aqui
        BigDecimal subscriptionAmount = session.getAmount();
        
        log.info("Registrando receita da plataforma: amount={}, providerId={}, holdbackDays={}", 
                 subscriptionAmount, session.getProviderId(), holdbackDays);
        
        revenueShareService.recordPlatformSubscriptionRevenue(
            session.getProviderId(),
            subscriptionAmount,
            session.getCurrency(),
            session.getId(),
            holdbackDays
        );
        
        log.info("Subscrição ativada com sucesso: providerId={}", session.getProviderId());
    }
}
