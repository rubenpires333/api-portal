package com.api_portal.backend.modules.notification.domain.enums;

public enum NotificationType {
    // Subscription events
    SUBSCRIPTION_REQUESTED,
    SUBSCRIPTION_APPROVED,
    SUBSCRIPTION_REVOKED,
    
    // API events
    API_VERSION_RELEASED,
    API_DEPRECATED,
    
    // Rate limit events
    RATE_LIMIT_WARNING,
    RATE_LIMIT_EXCEEDED,
    
    // Maintenance events
    API_MAINTENANCE,
    API_INCIDENT,
    
    // Billing events (future)
    PAYMENT_REQUIRED,
    PAYMENT_RECEIVED
}
