package com.api_portal.backend.modules.notification.event;

import com.api_portal.backend.modules.notification.domain.enums.NotificationType;
import com.api_portal.backend.modules.notification.service.NotificationService;
import com.api_portal.backend.modules.subscription.domain.entity.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventListener {
    
    private final NotificationService notificationService;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSubscriptionRequested(SubscriptionRequestedEvent event) {
        Subscription subscription = event.getSubscription();
        
        Map<String, Object> data = new HashMap<>();
        data.put("subscriptionId", subscription.getId().toString());
        data.put("apiId", subscription.getApi().getId().toString());
        data.put("apiName", subscription.getApi().getName());
        data.put("apiSlug", subscription.getApi().getSlug());
        data.put("consumerName", subscription.getConsumerName());
        data.put("consumerEmail", subscription.getConsumerEmail());
        data.put("providerName", subscription.getApi().getProviderName());
        data.put("actionUrl", "/provider/subscriptions");
        
        String title = "Nova Solicitação de Subscription";
        String message = subscription.getConsumerName() + " solicitou subscription para a API " + subscription.getApi().getName();
        
        notificationService.sendNotification(
            subscription.getApi().getProviderId(),
            subscription.getApi().getProviderEmail(),
            NotificationType.SUBSCRIPTION_REQUESTED,
            title,
            message,
            data,
            "/provider/subscriptions"
        );
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSubscriptionApproved(SubscriptionApprovedEvent event) {
        Subscription subscription = event.getSubscription();
        
        Map<String, Object> data = new HashMap<>();
        data.put("subscriptionId", subscription.getId().toString());
        data.put("apiId", subscription.getApi().getId().toString());
        data.put("apiName", subscription.getApi().getName());
        data.put("apiSlug", subscription.getApi().getSlug());
        data.put("apiKey", subscription.getApiKey());
        data.put("consumerName", subscription.getConsumerName());
        data.put("actionUrl", "/consumer/apis/" + subscription.getApi().getSlug() + "/test");
        
        String title = "Subscription Aprovada";
        String message = "Sua subscription para a API " + subscription.getApi().getName() + " foi aprovada!";
        
        notificationService.sendNotification(
            subscription.getConsumerId(),
            subscription.getConsumerEmail(),
            NotificationType.SUBSCRIPTION_APPROVED,
            title,
            message,
            data,
            "/consumer/subscriptions"
        );
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSubscriptionRevoked(SubscriptionRevokedEvent event) {
        Subscription subscription = event.getSubscription();
        String reason = event.getReason();
        
        Map<String, Object> data = new HashMap<>();
        data.put("subscriptionId", subscription.getId().toString());
        data.put("apiId", subscription.getApi().getId().toString());
        data.put("apiName", subscription.getApi().getName());
        data.put("apiSlug", subscription.getApi().getSlug());
        data.put("reason", reason != null ? reason : "Não especificado");
        data.put("consumerName", subscription.getConsumerName());
        
        String title = "Subscription Revogada";
        String message = "Sua subscription para a API " + subscription.getApi().getName() + " foi revogada";
        
        notificationService.sendNotification(
            subscription.getConsumerId(),
            subscription.getConsumerEmail(),
            NotificationType.SUBSCRIPTION_REVOKED,
            title,
            message,
            data,
            "/consumer/subscriptions"
        );
    }
}
