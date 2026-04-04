package com.api_portal.backend.modules.billing.dto;

import com.api_portal.backend.modules.billing.model.enums.GatewayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayConfigDTO {
    private GatewayType gatewayType;
    private String apiKey;
    private String webhookSecret;
    private boolean active;
    private boolean testMode;
    private String displayName;  // Opcional - se não fornecido, usa valor padrão
    private String logoUrl;      // Opcional
}
