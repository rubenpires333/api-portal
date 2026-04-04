package com.api_portal.backend.modules.billing.model.enums;

public enum WithdrawalMethod {
    VINTI4,              // Pagamento móvel Cabo Verde
    BANK_TRANSFER,       // Transferência bancária
    PAYPAL,              // PayPal
    WISE,                // Wise (TransferWise)
    PLATFORM_CREDIT      // Crédito na plataforma (0% taxa)
}
