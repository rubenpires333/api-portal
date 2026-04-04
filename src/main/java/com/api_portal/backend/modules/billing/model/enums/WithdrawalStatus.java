package com.api_portal.backend.modules.billing.model.enums;

public enum WithdrawalStatus {
    PENDING_APPROVAL,  // Aguardando aprovação do admin
    APPROVED,          // Aprovado, aguardando processamento
    PROCESSING,        // Em processamento
    COMPLETED,         // Concluído com sucesso
    REJECTED,          // Rejeitado pelo admin
    CANCELLED          // Cancelado pelo provider
}
