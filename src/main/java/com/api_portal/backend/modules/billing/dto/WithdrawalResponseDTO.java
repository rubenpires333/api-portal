package com.api_portal.backend.modules.billing.dto;

import com.api_portal.backend.modules.billing.model.enums.WithdrawalMethod;
import com.api_portal.backend.modules.billing.model.enums.WithdrawalStatus;
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
public class WithdrawalResponseDTO {
    
    private UUID id;
    private BigDecimal requestedAmount;
    private BigDecimal feePercentage;
    private BigDecimal feeAmount;
    private BigDecimal netAmount;
    private WithdrawalMethod method;
    private String destinationDetails;
    private WithdrawalStatus status;
    private UUID approvedBy;
    private String rejectionReason;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    
    // Informações do Provider/User
    private UUID providerId;
    private String providerName;
    private String providerEmail;
    private String providerUsername;
}
