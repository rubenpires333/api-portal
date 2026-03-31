package com.api_portal.backend.modules.subscription.service;

import com.api_portal.backend.modules.subscription.domain.entity.Subscription;
import com.api_portal.backend.modules.subscription.domain.enums.SubscriptionStatus;
import com.api_portal.backend.modules.subscription.domain.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionResetService {
    
    private final SubscriptionRepository subscriptionRepository;
    
    /**
     * Executa a cada hora para verificar e resetar contadores de requisições
     * baseado no rate_limit_period de cada API
     */
    @Scheduled(cron = "0 0 * * * *") // A cada hora no minuto 0
    @Transactional
    public void resetRequestCounters() {
        log.info("Iniciando verificação de reset de contadores de requisições");
        
        // Buscar todas as subscriptions ativas
        List<Subscription> activeSubscriptions = subscriptionRepository
            .findByStatus(SubscriptionStatus.ACTIVE);
        
        int resetCount = 0;
        LocalDateTime now = LocalDateTime.now();
        
        for (Subscription subscription : activeSubscriptions) {
            if (shouldResetCounter(subscription, now)) {
                subscription.setRequestsUsed(0);
                subscription.setLastResetAt(now);
                subscriptionRepository.save(subscription);
                resetCount++;
                
                log.debug("Reset contador para subscription {} (API: {}, Period: {})",
                    subscription.getId(),
                    subscription.getApi().getName(),
                    subscription.getApi().getRateLimitPeriod());
            }
        }
        
        log.info("Reset de contadores concluído. {} subscriptions resetadas de {} ativas",
            resetCount, activeSubscriptions.size());
    }
    
    /**
     * Verifica se o contador deve ser resetado baseado no período
     */
    private boolean shouldResetCounter(Subscription subscription, LocalDateTime now) {
        // Se nunca foi resetado, não reseta agora (usa o created_at como referência)
        LocalDateTime lastReset = subscription.getLastResetAt();
        if (lastReset == null) {
            lastReset = subscription.getCreatedAt();
        }
        
        String period = subscription.getApi().getRateLimitPeriod();
        if (period == null) {
            return false; // Sem período definido, não reseta
        }
        
        return switch (period.toLowerCase()) {
            case "minute" -> now.isAfter(lastReset.plusMinutes(1));
            case "hour" -> now.isAfter(lastReset.plusHours(1));
            case "day" -> now.isAfter(lastReset.plusDays(1));
            case "week" -> now.isAfter(lastReset.plusWeeks(1));
            case "month" -> now.isAfter(lastReset.plusMonths(1));
            case "year" -> now.isAfter(lastReset.plusYears(1));
            default -> {
                log.warn("Período desconhecido: {} para API {}", 
                    period, subscription.getApi().getName());
                yield false;
            }
        };
    }
    
    /**
     * Método manual para forçar reset de uma subscription específica
     * Útil para testes ou operações administrativas
     */
    @Transactional
    public void forceResetSubscription(String subscriptionId) {
        subscriptionRepository.findById(java.util.UUID.fromString(subscriptionId))
            .ifPresent(subscription -> {
                subscription.setRequestsUsed(0);
                subscription.setLastResetAt(LocalDateTime.now());
                subscriptionRepository.save(subscription);
                log.info("Reset manual forçado para subscription {}", subscriptionId);
            });
    }
}
