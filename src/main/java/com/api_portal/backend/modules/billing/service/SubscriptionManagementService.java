package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.dto.PaymentHistoryDTO;
import com.api_portal.backend.modules.billing.dto.SubscriptionActionDTO;
import com.api_portal.backend.modules.billing.dto.SubscriptionStatusDTO;
import com.api_portal.backend.modules.billing.model.PlatformPlan;
import com.api_portal.backend.modules.billing.model.ProviderPlatformSubscription;
import com.api_portal.backend.modules.billing.repository.PlatformPlanRepository;
import com.api_portal.backend.modules.billing.repository.ProviderPlatformSubscriptionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.SubscriptionUpdateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionManagementService {
    
    private final ProviderPlatformSubscriptionRepository subscriptionRepository;
    private final PlatformPlanRepository planRepository;
    
    /**
     * Obter status detalhado da subscrição do provider
     */
    public SubscriptionStatusDTO getSubscriptionStatus(UUID providerId) {
        log.info("Buscando status da subscrição para provider: {}", providerId);
        
        ProviderPlatformSubscription subscription = subscriptionRepository
            .findByProviderId(providerId)
            .filter(s -> "active".equals(s.getStatus()) || "past_due".equals(s.getStatus()))
            .orElseThrow(() -> new RuntimeException("Subscrição ativa não encontrada"));
        
        PlatformPlan plan = subscription.getPlan();
        
        return SubscriptionStatusDTO.builder()
            .subscriptionId(subscription.getId())
            .planName(plan.getName())
            .planDisplayName(plan.getDisplayName())
            .status(subscription.getStatus())
            .amount(plan.getMonthlyPrice())
            .currency(plan.getCurrency())
            .currentPeriodStart(subscription.getCurrentPeriodStart())
            .currentPeriodEnd(subscription.getCurrentPeriodEnd())
            .cancelAt(subscription.getCancelAtPeriodEnd() ? subscription.getCurrentPeriodEnd() : null)
            .cancelAtPeriodEnd(subscription.getCancelAtPeriodEnd())
            .createdAt(subscription.getCreatedAt())
            .stripeSubscriptionId(subscription.getStripeSubscriptionId())
            .maxApis(plan.getMaxApis())
            .maxRequestsPerMonth(plan.getMaxRequestsPerMonth())
            .maxTeamMembers(plan.getMaxTeamMembers())
            .customDomain(plan.isCustomDomain())
            .prioritySupport(plan.isPrioritySupport())
            .advancedAnalytics(plan.isAdvancedAnalytics())
            .build();
    }
    
    /**
     * Cancelar subscrição
     */
    @Transactional
    public SubscriptionStatusDTO cancelSubscription(UUID providerId, SubscriptionActionDTO actionDTO) {
        log.info("Cancelando subscrição para provider: {}", providerId);
        
        ProviderPlatformSubscription subscription = subscriptionRepository
            .findByProviderId(providerId)
            .filter(s -> "active".equals(s.getStatus()))
            .orElseThrow(() -> new RuntimeException("Subscrição ativa não encontrada"));
        
        try {
            if (Boolean.TRUE.equals(actionDTO.getImmediate())) {
                // Cancelamento imediato - fazer downgrade para STARTER
                Subscription stripeSubscription = Subscription.retrieve(subscription.getStripeSubscriptionId());
                
                SubscriptionCancelParams params = SubscriptionCancelParams.builder()
                    .setProrate(true)
                    .build();
                stripeSubscription.cancel(params);
                
                // Buscar plano STARTER
                PlatformPlan starterPlan = planRepository.findByName("STARTER")
                    .orElseThrow(() -> new RuntimeException("Plano STARTER não encontrado"));
                
                // Fazer downgrade para STARTER
                subscription.setPlan(starterPlan);
                subscription.setStatus("active");
                subscription.setStripeSubscriptionId(null);
                subscription.setCurrentPeriodStart(LocalDateTime.now());
                subscription.setCurrentPeriodEnd(null); // Plano gratuito não tem período
                subscription.setCancelAtPeriodEnd(false);
                
                log.info("Provider {} downgraded to STARTER plan immediately", providerId);
            } else {
                // Cancelar no fim do período
                Subscription stripeSubscription = Subscription.retrieve(subscription.getStripeSubscriptionId());
                
                SubscriptionCancelParams params = SubscriptionCancelParams.builder()
                    .build();
                stripeSubscription.cancel(params);
                
                subscription.setCancelAtPeriodEnd(true);
                
                log.info("Provider {} subscription will be canceled at period end", providerId);
            }
            
            subscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            
            log.info("Subscrição cancelada com sucesso: {}", subscription.getId());
            return getSubscriptionStatus(providerId);
            
        } catch (StripeException e) {
            log.error("Erro ao cancelar subscrição no Stripe", e);
            throw new RuntimeException("Erro ao cancelar subscrição: " + e.getMessage());
        }
    }
    
    /**
     * Fazer upgrade/downgrade de plano
     */
    @Transactional
    public SubscriptionStatusDTO changePlan(UUID providerId, SubscriptionActionDTO actionDTO) {
        log.info("Alterando plano para provider: {} - Novo plano: {}", providerId, actionDTO.getNewPlanId());
        
        ProviderPlatformSubscription currentSubscription = subscriptionRepository
            .findByProviderId(providerId)
            .filter(s -> "active".equals(s.getStatus()))
            .orElseThrow(() -> new RuntimeException("Subscrição ativa não encontrada"));
        
        PlatformPlan newPlan = planRepository.findById(actionDTO.getNewPlanId())
            .orElseThrow(() -> new RuntimeException("Plano não encontrado"));
        
        // Verificar se é upgrade ou downgrade
        boolean isUpgrade = newPlan.getMonthlyPrice().compareTo(currentSubscription.getPlan().getMonthlyPrice()) > 0;
        
        try {
            Subscription stripeSubscription = Subscription.retrieve(currentSubscription.getStripeSubscriptionId());
            
            // Obter o item da subscrição
            SubscriptionItem item = stripeSubscription.getItems().getData().get(0);
            
            // Atualizar subscrição no Stripe
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .addItem(
                    SubscriptionUpdateParams.Item.builder()
                        .setId(item.getId())
                        .setPrice(newPlan.getStripePriceId())
                        .build()
                )
                .setProrationBehavior(isUpgrade ? 
                    SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS : 
                    SubscriptionUpdateParams.ProrationBehavior.NONE)
                .build();
            
            stripeSubscription.update(params);
            
            // Atualizar no banco de dados
            currentSubscription.setPlan(newPlan);
            currentSubscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(currentSubscription);
            
            log.info("Plano alterado com sucesso: {} -> {}", 
                currentSubscription.getPlan().getName(), newPlan.getName());
            
            return getSubscriptionStatus(providerId);
            
        } catch (StripeException e) {
            log.error("Erro ao alterar plano no Stripe", e);
            throw new RuntimeException("Erro ao alterar plano: " + e.getMessage());
        }
    }
    
    /**
     * Obter histórico de pagamentos
     */
    public List<PaymentHistoryDTO> getPaymentHistory(UUID providerId) {
        log.info("Buscando histórico de pagamentos para provider: {}", providerId);
        
        ProviderPlatformSubscription subscription = subscriptionRepository
            .findByProviderId(providerId)
            .orElse(null);
        
        if (subscription == null || subscription.getStripeCustomerId() == null) {
            return new ArrayList<>();
        }
        
        List<PaymentHistoryDTO> history = new ArrayList<>();
        
        try {
            // Buscar invoices do Stripe
            var invoices = Invoice.list(
                com.stripe.param.InvoiceListParams.builder()
                    .setCustomer(subscription.getStripeCustomerId())
                    .setLimit(50L)
                    .build()
            );
            
            for (Invoice invoice : invoices.getData()) {
                PaymentHistoryDTO dto = PaymentHistoryDTO.builder()
                    .invoiceId(invoice.getId())
                    .amount(new java.math.BigDecimal(invoice.getAmountPaid()).divide(new java.math.BigDecimal(100)))
                    .currency(invoice.getCurrency().toUpperCase())
                    .status(mapInvoiceStatus(invoice.getStatus()))
                    .paidAt(invoice.getStatusTransitions().getPaidAt() != null ? 
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(invoice.getStatusTransitions().getPaidAt()), 
                            ZoneId.systemDefault()) : null)
                    .createdAt(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(invoice.getCreated()), 
                        ZoneId.systemDefault()))
                    .receiptUrl(invoice.getHostedInvoiceUrl())
                    .invoicePdf(invoice.getInvoicePdf())
                    .build();
                
                history.add(dto);
            }
            
            return history;
            
        } catch (StripeException e) {
            log.error("Erro ao buscar histórico de pagamentos", e);
            return new ArrayList<>();
        }
    }
    
    private String mapInvoiceStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "paid" -> "PAID";
            case "open", "draft" -> "PENDING";
            case "void", "uncollectible" -> "FAILED";
            default -> stripeStatus.toUpperCase();
        };
    }
}
