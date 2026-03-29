package com.api_portal.backend.modules.subscription.domain.enums;

public enum SubscriptionStatus {
    PENDING,    // Aguardando aprovação
    ACTIVE,     // Ativa e funcional
    REVOKED,    // Revogada pelo provider
    EXPIRED,    // Expirada por tempo
    CANCELLED   // Cancelada pelo consumer
}
