package com.api_portal.backend.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request para renovar token")
public class RefreshRequest {
    
    @NotBlank(message = "Refresh token é obrigatório")
    @Schema(description = "Refresh token obtido no login", example = "eyJhbGciOiJIUzI1NiIs...")
    private String refreshToken;
}
