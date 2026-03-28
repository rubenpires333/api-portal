package com.api_portal.backend.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response com tokens de autenticação")
public class TokenResponse {
    
    @Schema(description = "Access token JWT", example = "eyJhbGciOiJSUzI1NiIs...")
    private String accessToken;
    
    @Schema(description = "Refresh token", example = "eyJhbGciOiJIUzI1NiIs...")
    private String refreshToken;
    
    @Schema(description = "Tipo do token", example = "Bearer")
    private String tokenType;
    
    @Schema(description = "Tempo de expiração em segundos", example = "3600")
    private Long expiresIn;
}
