package com.api_portal.backend.modules.notification.domain.enums;

public enum NotificationType {
    // Subscription events
    SUBSCRIPTION_REQUESTED,
    SUBSCRIPTION_APPROVED,
    SUBSCRIPTION_REVOKED,
    
    // API events
    API_VERSION_RELEASED,
    API_DEPRECATED,
    API_APPROVAL_REQUESTED,
    API_APPROVED,
    API_REJECTED,
    
    // Rate limit events
    RATE_LIMIT_WARNING,
    RATE_LIMIT_EXCEEDED,
    
    // Maintenance events
    API_MAINTENANCE,
    API_INCIDENT,
    
    // Billing events (future)
    PAYMENT_REQUIRED,
    PAYMENT_RECEIVED,
    WITHDRAWAL_REQUESTED,
    WITHDRAWAL_APPROVED,
    WITHDRAWAL_REJECTED
}
