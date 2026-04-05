package com.api_portal.backend.modules.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionActionDTO {
    private UUID subscriptionId;
    private UUID newPlanId;
    private String action; // CANCEL, UPGRADE, DOWNGRADE
    private String reason;
    private Boolean immediate; // Para cancelamento imediato ou no fim do período
}
