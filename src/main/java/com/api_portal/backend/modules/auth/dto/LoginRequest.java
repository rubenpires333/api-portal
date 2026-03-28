package com.api_portal.backend.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request para login de utilizador")
public class LoginRequest {
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Schema(description = "Email do utilizador", example = "admin@apicv.cv")
    private String email;
    
    @NotBlank(message = "Password é obrigatória")
    @Schema(description = "Password do utilizador", example = "Admin@123")
    private String password;
}
