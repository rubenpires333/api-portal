package com.api_portal.backend.modules.auth.dto;

import com.api_portal.backend.modules.auth.model.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request para registro de novo utilizador")
public class RegisterRequest {
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    @Schema(description = "Nome completo do utilizador", example = "João Silva")
    private String name;
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Schema(description = "Email do utilizador", example = "joao@example.com")
    private String email;
    
    @NotBlank(message = "Password é obrigatória")
    @Size(min = 8, message = "Password deve ter no mínimo 8 caracteres")
    @Schema(description = "Password do utilizador", example = "Password@123")
    private String password;
    
    @NotNull(message = "Role é obrigatória")
    @Schema(description = "Papel do utilizador", example = "CONSUMER")
    private UserRole role;
}
