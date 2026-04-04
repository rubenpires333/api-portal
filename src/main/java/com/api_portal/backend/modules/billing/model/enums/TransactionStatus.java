package com.api_portal.backend.modules.billing.model.enums;

public enum TransactionStatus {
    PENDING,    // Em holdback (14 dias)
    AVAILABLE,  // Disponível para levantamento
    RESERVED,   // Reservado para levantamento em curso
    DEBITED,    // Já debitado (levantamento concluído)
    COMPLETED,  // Transação completada (levantamento aprovado)
    CANCELLED   // Cancelado
}
