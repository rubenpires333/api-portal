package com.api_portal.backend.modules.notification.event;

import com.api_portal.backend.modules.subscription.domain.entity.Subscription;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SubscriptionRequestedEvent extends ApplicationEvent {
    private final Subscription subscription;
    
    public SubscriptionRequestedEvent(Object source, Subscription subscription) {
        super(source);
        this.subscription = subscription;
    }
}
