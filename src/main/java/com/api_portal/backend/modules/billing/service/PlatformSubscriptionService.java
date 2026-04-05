package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.gateway.dto.WebhookEvent;
import com.api_portal.backend.modules.billing.model.PlatformPlan;
import com.api_portal.backend.modules.billing.model.ProviderPlatformSubscription;
import com.api_portal.backend.modules.billing.repository.PlatformPlanRepository;
import com.api_portal.backend.modules.billing.repository.ProviderPlatformSubscriptionRepository;
import com.api_portal.backend.modules.notification.domain.enums.NotificationType;
import com.api_portal.backend.modules.notification.service.NotificationService;
import com.api_portal.backend.modules.user.domain.User;
import com.api_portal.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformSubscriptionService {

    private final ProviderPlatformSubscriptionRepository subscriptionRepository;
    private final PlatformPlanRepository planRepository;
    private final RevenueShareService revenueShareService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final com.api_portal.backend.modules.billing.gateway.PaymentGatewayFactory paymentGatewayFactory;
    
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
            
            // Enviar notificação de renovação
            sendSubscriptionRenewedNotification(providerId, subscription);
            
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
        log.info("=== ATIVANDO SUBSCRIÇÃO ===");
        log.info("Session ID: {}", session.getId());
        log.info("Provider ID: {}", session.getProviderId());
        log.info("Session StripeCustomerId: {}", session.getStripeCustomerId());
        log.info("Session StripeSubscriptionId: {}", session.getStripeSubscriptionId());
        log.info("Event CustomerId: {}", event.getCustomerId());
        log.info("Event SubscriptionId: {}", event.getSubscriptionId());
        
        PlatformPlan plan = planRepository.findById(session.getPlanId())
            .orElseThrow(() -> new RuntimeException("Plan not found: " + session.getPlanId()));
        
        ProviderPlatformSubscription subscription = subscriptionRepository
            .findByProviderId(session.getProviderId())
            .orElse(null);
        
        // Usar IDs do event se a session não tiver (fallback)
        String stripeCustomerId = session.getStripeCustomerId() != null ? 
            session.getStripeCustomerId() : event.getCustomerId();
        String stripeSubscriptionId = session.getStripeSubscriptionId() != null ? 
            session.getStripeSubscriptionId() : event.getSubscriptionId();
        
        log.info("IDs finais a serem salvos - CustomerId: {}, SubscriptionId: {}", 
            stripeCustomerId, stripeSubscriptionId);
        
        if (subscription == null) {
            subscription = ProviderPlatformSubscription.builder()
                .providerId(session.getProviderId())
                .plan(plan)
                .stripeSubscriptionId(stripeSubscriptionId)
                .stripeCustomerId(stripeCustomerId)
                .status("active")
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                .cancelAtPeriodEnd(false)
                .build();
            
            log.info("Nova subscrição criada: providerId={}, plan={}", 
                     session.getProviderId(), plan.getName());
        } else {
            subscription.setPlan(plan);
            subscription.setStripeSubscriptionId(stripeSubscriptionId);
            subscription.setStripeCustomerId(stripeCustomerId);
            subscription.setStatus("active");
            subscription.setCurrentPeriodStart(LocalDateTime.now());
            subscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));
            
            log.info("Subscrição atualizada: providerId={}, plan={}", 
                     session.getProviderId(), plan.getName());
        }
        
        ProviderPlatformSubscription savedSubscription = subscriptionRepository.save(subscription);
        log.info("Subscrição salva no banco - ID: {}, StripeCustomerId: {}, StripeSubscriptionId: {}", 
            savedSubscription.getId(), 
            savedSubscription.getStripeCustomerId(), 
            savedSubscription.getStripeSubscriptionId());
        
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
        
        log.info("✅ Subscrição ativada com sucesso: providerId={}", session.getProviderId());
        log.info("=== FIM ATIVAÇÃO SUBSCRIÇÃO ===");
    }

    /**
     * Lidar com falha de pagamento
     */
    @Transactional
    public void handlePaymentFailed(WebhookEvent event) {
        try {
            log.info("=== HANDLING PAYMENT FAILED ===");
            log.info("Event ID: {}", event.getEventId());
            log.info("Subscription ID: {}", event.getSubscriptionId());
            
            String providerIdStr = event.getMetadata().get("providerId");
            if (providerIdStr == null) {
                log.error("Missing providerId in webhook metadata");
                return;
            }
            
            UUID providerId = UUID.fromString(providerIdStr);
            
            ProviderPlatformSubscription subscription = subscriptionRepository
                .findByProviderId(providerId)
                .orElse(null);
            
            if (subscription != null) {
                subscription.setStatus("past_due");
                subscription.setUpdatedAt(LocalDateTime.now());
                subscriptionRepository.save(subscription);
                
                log.info("✅ Subscription marked as past_due for provider: {}", providerId);
                
                // Enviar notificação ao provider
                sendPaymentFailedNotification(providerId, subscription);
            }
            
            log.info("=== END HANDLING PAYMENT FAILED ===");
            
        } catch (Exception e) {
            log.error("❌ Error handling payment failed", e);
        }
    }
    
    /**
     * Enviar notificação de falha de pagamento
     */
    private void sendPaymentFailedNotification(UUID providerId, ProviderPlatformSubscription subscription) {
        try {
            // Buscar usuário do provider
            User user = userRepository.findById(providerId)
                .orElse(null);
            
            if (user == null) {
                log.warn("User not found for provider: {}", providerId);
                return;
            }
            
            String keycloakId = user.getKeycloakId();
            String email = user.getEmail();
            
            // Preparar dados da notificação
            Map<String, Object> data = new HashMap<>();
            data.put("planName", subscription.getPlan().getDisplayName());
            data.put("amount", subscription.getPlan().getMonthlyPrice());
            data.put("currency", subscription.getPlan().getCurrency());
            data.put("subscriptionId", subscription.getId().toString());
            
            String title = "Falha no Pagamento da Subscrição";
            String message = String.format(
                "Não foi possível processar o pagamento da sua subscrição do plano %s. " +
                "Por favor, atualize seu método de pagamento para evitar a interrupção do serviço.",
                subscription.getPlan().getDisplayName()
            );
            
            String actionUrl = "/provider/subscription";
            
            // Enviar notificação
            notificationService.sendNotification(
                keycloakId,
                email,
                NotificationType.PAYMENT_FAILED,
                title,
                message,
                data,
                actionUrl
            );
            
            log.info("✅ Payment failed notification sent to provider: {}", providerId);
            
        } catch (Exception e) {
            log.error("❌ Error sending payment failed notification", e);
        }
    }
    
    /**
     * Lidar com atualização de subscrição
     */
    @Transactional
    public void handleSubscriptionUpdated(WebhookEvent event) {
        try {
            log.info("=== HANDLING SUBSCRIPTION UPDATED ===");
            log.info("Event ID: {}", event.getEventId());
            log.info("Subscription ID: {}", event.getSubscriptionId());
            
            String providerIdStr = event.getMetadata().get("providerId");
            if (providerIdStr == null) {
                log.error("Missing providerId in webhook metadata");
                return;
            }
            
            UUID providerId = UUID.fromString(providerIdStr);
            
            ProviderPlatformSubscription subscription = subscriptionRepository
                .findByProviderId(providerId)
                .orElse(null);
            
            if (subscription != null) {
                // Atualizar status e datas conforme necessário
                subscription.setUpdatedAt(LocalDateTime.now());
                subscriptionRepository.save(subscription);
                
                log.info("✅ Subscription updated for provider: {}", providerId);
            }
            
            log.info("=== END HANDLING SUBSCRIPTION UPDATED ===");
            
        } catch (Exception e) {
            log.error("❌ Error handling subscription updated", e);
        }
    }
    
    /**
     * Lidar com cancelamento de subscrição
     */
    @Transactional
    public void handleSubscriptionDeleted(WebhookEvent event) {
        try {
            log.info("=== HANDLING SUBSCRIPTION DELETED ===");
            log.info("Event ID: {}", event.getEventId());
            log.info("Subscription ID: {}", event.getSubscriptionId());
            
            String providerIdStr = event.getMetadata().get("providerId");
            if (providerIdStr == null) {
                log.error("Missing providerId in webhook metadata");
                return;
            }
            
            UUID providerId = UUID.fromString(providerIdStr);
            
            ProviderPlatformSubscription subscription = subscriptionRepository
                .findByProviderId(providerId)
                .orElse(null);
            
            if (subscription != null) {
                // Buscar plano STARTER (gratuito)
                PlatformPlan starterPlan = planRepository.findByName("STARTER")
                    .orElseThrow(() -> new RuntimeException("Plano STARTER não encontrado"));
                
                // Fazer downgrade para STARTER
                subscription.setPlan(starterPlan);
                subscription.setStatus("active");
                subscription.setStripeSubscriptionId(null);
                subscription.setStripeCustomerId(subscription.getStripeCustomerId()); // Manter customer ID
                subscription.setCurrentPeriodStart(LocalDateTime.now());
                subscription.setCurrentPeriodEnd(null); // Plano gratuito não tem período
                subscription.setCancelAtPeriodEnd(false);
                subscription.setUpdatedAt(LocalDateTime.now());
                subscriptionRepository.save(subscription);
                
                log.info("✅ Subscription downgraded to STARTER for provider: {}", providerId);
                
                // Enviar notificação de cancelamento
                sendSubscriptionCanceledNotification(providerId, subscription);
            }
            
            log.info("=== END HANDLING SUBSCRIPTION DELETED ===");
            
        } catch (Exception e) {
            log.error("❌ Error handling subscription deleted", e);
        }
    }

    /**
     * Enviar notificação de renovação de subscrição
     */
    private void sendSubscriptionRenewedNotification(UUID providerId, ProviderPlatformSubscription subscription) {
        try {
            User user = userRepository.findById(providerId).orElse(null);
            if (user == null) return;
            
            Map<String, Object> data = new HashMap<>();
            data.put("planName", subscription.getPlan().getDisplayName());
            data.put("amount", subscription.getPlan().getMonthlyPrice());
            data.put("currency", subscription.getPlan().getCurrency());
            data.put("nextBillingDate", subscription.getCurrentPeriodEnd().toString());
            
            String title = "Subscrição Renovada com Sucesso";
            String message = String.format(
                "Sua subscrição do plano %s foi renovada com sucesso. " +
                "Próxima cobrança em %s.",
                subscription.getPlan().getDisplayName(),
                subscription.getCurrentPeriodEnd().toLocalDate()
            );
            
            notificationService.sendNotification(
                user.getKeycloakId(),
                user.getEmail(),
                NotificationType.SUBSCRIPTION_RENEWED,
                title,
                message,
                data,
                "/provider/subscription"
            );
            
            log.info("✅ Subscription renewed notification sent to provider: {}", providerId);
            
        } catch (Exception e) {
            log.error("❌ Error sending subscription renewed notification", e);
        }
    }
    
    /**
     * Enviar notificação de cancelamento de subscrição
     */
    private void sendSubscriptionCanceledNotification(UUID providerId, ProviderPlatformSubscription subscription) {
        try {
            User user = userRepository.findById(providerId).orElse(null);
            if (user == null) return;
            
            Map<String, Object> data = new HashMap<>();
            data.put("planName", subscription.getPlan().getDisplayName());
            
            String title = "Subscrição Cancelada";
            String message = String.format(
                "Sua subscrição foi cancelada e você foi movido para o plano STARTER (gratuito). " +
                "Você pode fazer upgrade a qualquer momento."
            );
            
            notificationService.sendNotification(
                user.getKeycloakId(),
                user.getEmail(),
                NotificationType.SUBSCRIPTION_CANCELED,
                title,
                message,
                data,
                "/provider/plans"
            );
            
            log.info("✅ Subscription canceled notification sent to provider: {}", providerId);
            
        } catch (Exception e) {
            log.error("❌ Error sending subscription canceled notification", e);
        }
    }

    /**
     * Fazer upgrade ou downgrade de plano com proration automático
     */
    @Transactional
    public com.api_portal.backend.modules.billing.dto.UpgradeDowngradeResponse upgradeOrDowngradePlan(
            UUID providerId, String newPlanName) {
        
        log.info("=== UPGRADE/DOWNGRADE REQUEST ===");
        log.info("Provider ID: {}", providerId);
        log.info("New Plan: {}", newPlanName);

        // 1. Buscar subscription atual
        ProviderPlatformSubscription currentSubscription = subscriptionRepository
            .findByProviderId(providerId)
            .orElseThrow(() -> new RuntimeException("No active subscription found for provider"));

        PlatformPlan currentPlan = currentSubscription.getPlan();
        log.info("Current Plan: {}", currentPlan.getName());

        // 2. Buscar novo plano
        PlatformPlan newPlan = planRepository.findByName(newPlanName.toUpperCase())
            .orElseThrow(() -> new RuntimeException("Plan not found: " + newPlanName));

        // 3. Verificar se é realmente uma mudança
        if (currentPlan.getId().equals(newPlan.getId())) {
            return com.api_portal.backend.modules.billing.dto.UpgradeDowngradeResponse.builder()
                .success(false)
                .message("Você já está neste plano")
                .changeType("NO_CHANGE")
                .requiresPayment(false)
                .oldPlanName(currentPlan.getName())
                .newPlanName(newPlan.getName())
                .build();
        }

        // 4. Determinar tipo de mudança
        String changeType = newPlan.getMonthlyPrice().compareTo(currentPlan.getMonthlyPrice()) > 0 
            ? "UPGRADE" : "DOWNGRADE";
        log.info("Change Type: {}", changeType);

        // 5. Atualizar subscription no Stripe com proration
        Map<String, String> metadata = new HashMap<>();
        metadata.put("providerId", providerId.toString());
        metadata.put("planId", newPlan.getId().toString());
        metadata.put("planName", newPlan.getName());
        metadata.put("changeType", changeType);
        metadata.put("previousPlanId", currentPlan.getId().toString());
        metadata.put("previousPlanName", currentPlan.getName());

        com.api_portal.backend.modules.billing.gateway.stripe.StripeGateway stripeGateway = 
            (com.api_portal.backend.modules.billing.gateway.stripe.StripeGateway) 
            paymentGatewayFactory.getActive();

        Map<String, Object> updateResult = stripeGateway.updateSubscriptionWithProration(
            currentSubscription.getStripeSubscriptionId(),
            newPlan.getStripePriceId(),
            metadata
        );

        // 6. Atualizar subscription no banco
        currentSubscription.setPlan(newPlan);
        currentSubscription.setUpdatedAt(java.time.LocalDateTime.now());
        subscriptionRepository.save(currentSubscription);

        log.info("✅ Subscription updated in database");

        // 7. Calcular valores de proration
        Long amountDue = (Long) updateResult.get("amountDue");
        Long total = (Long) updateResult.get("total");
        
        java.math.BigDecimal prorationAmount = amountDue != null 
            ? java.math.BigDecimal.valueOf(amountDue).divide(java.math.BigDecimal.valueOf(100))
            : java.math.BigDecimal.ZERO;

        boolean requiresPayment = prorationAmount.compareTo(java.math.BigDecimal.ZERO) > 0;

        // 8. Registrar transação de mudança de plano
        if (requiresPayment) {
            revenueShareService.recordPlatformSubscriptionRevenue(
                providerId,
                prorationAmount,
                newPlan.getCurrency(),
                currentSubscription.getId(),
                holdbackDays
            );
        }

        // 9. Enviar notificação
        sendPlanChangeNotification(providerId, currentPlan, newPlan, changeType, prorationAmount);

        log.info("=== UPGRADE/DOWNGRADE COMPLETED ===");

        return com.api_portal.backend.modules.billing.dto.UpgradeDowngradeResponse.builder()
            .success(true)
            .message(changeType.equals("UPGRADE") 
                ? "Upgrade realizado com sucesso!" 
                : "Downgrade realizado com sucesso!")
            .changeType(changeType)
            .requiresPayment(requiresPayment)
            .prorationAmount(prorationAmount)
            .oldPlanName(currentPlan.getName())
            .newPlanName(newPlan.getName())
            .nextBillingDate(currentSubscription.getCurrentPeriodEnd())
            .invoiceUrl((String) updateResult.get("invoiceUrl"))
            .build();
    }

    /**
     * Enviar notificação de mudança de plano
     */
    private void sendPlanChangeNotification(
            UUID providerId, 
            PlatformPlan oldPlan, 
            PlatformPlan newPlan,
            String changeType,
            java.math.BigDecimal prorationAmount) {
        
        try {
            User user = userRepository.findById(providerId).orElse(null);
            if (user == null) return;
            
            Map<String, Object> data = new HashMap<>();
            data.put("oldPlanName", oldPlan.getDisplayName());
            data.put("newPlanName", newPlan.getDisplayName());
            data.put("changeType", changeType);
            data.put("prorationAmount", prorationAmount.toString());
            
            String title = changeType.equals("UPGRADE") 
                ? "Upgrade de Plano Realizado" 
                : "Downgrade de Plano Realizado";
            
            String message = String.format(
                "Seu plano foi alterado de %s para %s. %s",
                oldPlan.getDisplayName(),
                newPlan.getDisplayName(),
                prorationAmount.compareTo(java.math.BigDecimal.ZERO) > 0
                    ? "Valor proporcional cobrado: €" + prorationAmount
                    : "Crédito aplicado à sua conta."
            );
            
            notificationService.sendNotification(
                user.getKeycloakId(),
                user.getEmail(),
                NotificationType.SUBSCRIPTION_RENEWED,
                title,
                message,
                data,
                "/provider/subscription"
            );
            
            log.info("✅ Plan change notification sent to provider: {}", providerId);
            
        } catch (Exception e) {
            log.error("❌ Error sending plan change notification", e);
        }
    }
}
