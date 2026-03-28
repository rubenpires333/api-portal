package com.api_portal.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordRequest {
    
    @NotBlank(message = "Senha atual é obrigatória")
    private String currentPassword;
    
    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 8, message = "Nova senha deve ter no mínimo 8 caracteres")
    private String newPassword;
    
    @NotBlank(message = "Confirmação de senha é obrigatória")
    private String confirmPassword;
}
