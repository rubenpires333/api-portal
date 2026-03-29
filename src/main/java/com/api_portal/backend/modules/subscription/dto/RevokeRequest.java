package com.api_portal.backend.modules.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevokeRequest {
    
    @NotBlank(message = "Motivo da revogação é obrigatório")
    private String reason;
}
