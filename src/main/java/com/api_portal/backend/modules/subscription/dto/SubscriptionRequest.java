package com.api_portal.backend.modules.subscription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {
    
    @NotNull(message = "API ID é obrigatório")
    private UUID apiId;
    
    private String notes; // Notas do consumer sobre o uso pretendido
}
