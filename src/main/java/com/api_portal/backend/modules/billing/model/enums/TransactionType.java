package com.api_portal.backend.modules.billing.model.enums;

public enum TransactionType {
    CREDIT_REVENUE,        // Receita de API subscription
    DEBIT_WITHDRAWAL,      // Levantamento de saldo
    DEBIT_PLATFORM_FEE,    // Taxa da plataforma
    CREDIT_REFUND,         // Reembolso ao provider
    DEBIT_WITHDRAWAL_FEE   // Taxa de levantamento
}
