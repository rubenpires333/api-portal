package com.api_portal.backend.modules.auth.dto;

import com.api_portal.backend.modules.auth.model.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados do utilizador autenticado")
public class AuthUserResponse {
    
    @Schema(description = "ID do utilizador no Keycloak", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;
    
    @Schema(description = "Nome do utilizador", example = "João Silva")
    private String name;
    
    @Schema(description = "Email do utilizador", example = "joao@example.com")
    private String email;
    
    @Schema(description = "Email verificado", example = "true")
    private Boolean emailVerified;
    
    @Schema(description = "Roles do utilizador")
    private List<String> roles;
}
