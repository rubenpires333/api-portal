package com.api_portal.backend.modules.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformWalletSummaryDTO {
    
    // Receitas totais
    private BigDecimal totalSubscriptionRevenue;      // Total de subscriptions
    private BigDecimal totalApiCommissionRevenue;     // Total de comissões de API (20%)
    private BigDecimal totalWithdrawalFees;           // Total de taxas de levantamento
    private BigDecimal totalRevenue;                  // Total geral
    
    // Subscriptions ativas
    private Long activeSubscriptions;                 // Quantidade de subscriptions ativas
    private BigDecimal monthlyRecurringRevenue;       // MRR - Receita mensal recorrente
    
    // Levantamentos
    private Long pendingWithdrawals;                  // Levantamentos pendentes de aprovação
    private BigDecimal pendingWithdrawalsAmount;      // Valor total pendente
    private Long completedWithdrawals;                // Levantamentos concluídos
    private BigDecimal completedWithdrawalsAmount;    // Valor total pago
    
    // Providers
    private Long totalProviders;                      // Total de providers
    private Long activeProviders;                     // Providers com subscription ativa
    
    // Período
    private String period;                            // Ex: "Últimos 30 dias", "Este mês", etc.
}
