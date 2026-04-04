package com.api_portal.backend.modules.billing.dto;

import com.api_portal.backend.modules.billing.model.GatewayConfig;
import com.api_portal.backend.modules.billing.model.enums.GatewayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayConfigResponseDTO {
    private UUID id;
    private GatewayType gatewayType;
    private String apiKey;
    private String webhookSecret;
    private boolean active;
    private boolean testMode;
    private String displayName;
    private String logoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GatewayConfigResponseDTO fromEntity(GatewayConfig config) {
        String apiKey = config.getSettings() != null ? config.getSettings().get("api_key") : "";
        String webhookSecret = config.getSettings() != null ? config.getSettings().get("webhook_secret") : "";
        String testModeStr = config.getSettings() != null ? config.getSettings().get("test_mode") : "true";
        boolean testMode = Boolean.parseBoolean(testModeStr);

        return GatewayConfigResponseDTO.builder()
            .id(config.getId())
            .gatewayType(config.getGatewayType())
            .apiKey(maskSensitiveData(apiKey))
            .webhookSecret(maskSensitiveData(webhookSecret))
            .active(config.isActive())
            .testMode(testMode)
            .displayName(config.getDisplayName())
            .logoUrl(config.getLogoUrl())
            .createdAt(config.getCreatedAt())
            .updatedAt(config.getUpdatedAt())
            .build();
    }

    private static String maskSensitiveData(String data) {
        if (data == null || data.length() < 8) {
            return "****";
        }
        return data.substring(0, 4) + "****" + data.substring(data.length() - 4);
    }
}
