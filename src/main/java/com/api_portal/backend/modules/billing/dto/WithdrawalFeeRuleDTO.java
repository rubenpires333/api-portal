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
public class WithdrawalFeeRuleDTO {
    private WithdrawalMethod withdrawalMethod;
    private BigDecimal feePercentage;
    private BigDecimal fixedFee;
    private String fixedFeeCurrency;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private boolean active;
}
