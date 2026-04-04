package com.api_portal.backend.modules.billing.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayMetadata {
    private String displayName;
    private String logoUrl;
    private List<String> supportedCurrencies;
    private boolean supportsSubscriptions;
    private boolean supportsRefunds;
    private boolean supportsWebhooks;
}
