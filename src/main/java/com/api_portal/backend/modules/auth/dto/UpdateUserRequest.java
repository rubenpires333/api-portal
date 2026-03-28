package com.api_portal.backend.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    private String name;
    
    @Email(message = "Email inválido")
    private String email;
    
    private String phone;
    
    private String company;
    
    private String jobTitle;
}
