package com.api_portal.backend.modules.billing.dto;

import com.api_portal.backend.modules.billing.model.enums.WithdrawalMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequestDTO {
    private BigDecimal amount;
    private WithdrawalMethod method;
    private String destinationDetails;
}
