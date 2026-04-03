package com.api_portal.backend.modules.notification.event;

import com.api_portal.backend.modules.subscription.domain.entity.Subscription;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SubscriptionRevokedEvent extends ApplicationEvent {
    private final Subscription subscription;
    private final String reason;
    
    public SubscriptionRevokedEvent(Object source, Subscription subscription, String reason) {
        super(source);
        this.subscription = subscription;
        this.reason = reason;
    }
}
