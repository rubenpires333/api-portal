package com.api_portal.backend.modules.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeDowngradeResponse {
    private boolean success;
    private String message;
    private String changeType; // UPGRADE, DOWNGRADE, NO_CHANGE
    private boolean requiresPayment;
    private BigDecimal prorationAmount; // Valor a pagar/receber
    private BigDecimal creditAmount; // Crédito do plano antigo
    private BigDecimal newPlanAmount; // Valor proporcional do novo plano
    private String oldPlanName;
    private String newPlanName;
    private LocalDateTime nextBillingDate;
    private String invoiceUrl; // URL da invoice do Stripe
}
