package com.api_portal.backend.modules.auth.dto;

import com.api_portal.backend.modules.auth.model.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request para registro de novo utilizador")
public class RegisterRequest {
    
    @Schema(description = "Primeiro nome do utilizador", example = "João")
    private String firstName;
    
    @Schema(description = "Sobrenome do utilizador", example = "Silva")
    private String lastName;
    
    @Schema(description = "Nome completo do utilizador (alternativa a firstName/lastName)", example = "João Silva")
    private String name;
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Schema(description = "Email do utilizador", example = "joao@example.com")
    private String email;
    
    @NotBlank(message = "Password é obrigatória")
    @Size(min = 6, message = "Password deve ter no mínimo 6 caracteres")
    @Schema(description = "Password do utilizador", example = "Password@123")
    private String password;
    
    @Schema(description = "Papel do utilizador (padrão: CONSUMER)", example = "CONSUMER")
    private UserRole role;
    
    /**
     * Retorna o nome completo, seja de firstName/lastName ou name
     */
    public String getFullName() {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        
        StringBuilder fullName = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) {
            fullName.append(firstName);
        }
        if (lastName != null && !lastName.isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName);
        }
        
        return fullName.length() > 0 ? fullName.toString() : email;
    }
    
    /**
     * Retorna o role, padrão CONSUMER se não especificado
     */
    public UserRole getRoleOrDefault() {
        return role != null ? role : UserRole.CONSUMER;
    }
}
