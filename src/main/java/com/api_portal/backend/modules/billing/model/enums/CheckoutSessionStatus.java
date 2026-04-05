package com.api_portal.backend.modules.billing.model.enums;

public enum CheckoutSessionStatus {
    PENDING,      // Sessão criada, aguardando pagamento
    COMPLETED,    // Pagamento confirmado via webhook
    CANCELLED,    // Usuário cancelou
    EXPIRED,      // Sessão expirou (30 minutos)
    FAILED        // Falha no processamento
}
