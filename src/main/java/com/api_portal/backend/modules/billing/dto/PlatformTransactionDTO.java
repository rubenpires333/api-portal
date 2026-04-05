package com.api_portal.backend.modules.billing.dto;

import com.api_portal.backend.modules.billing.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformTransactionDTO {
    
    private UUID id;
    private BigDecimal amount;
    private TransactionType type;
    private String description;
    private LocalDateTime createdAt;
    
    // Informações do provider
    private ProviderInfo provider;
    
    // Informações adicionais
    private String planName;              // Se for subscription
    private UUID referenceId;             // ID da subscription, payment, etc.
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderInfo {
        private UUID id;
        private String name;
        private String email;
        private String username;
    }
}
