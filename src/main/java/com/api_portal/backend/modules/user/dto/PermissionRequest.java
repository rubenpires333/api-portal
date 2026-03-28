package com.api_portal.backend.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRequest {
    
    @NotBlank(message = "Nome é obrigatório")
    private String name;
    
    @NotBlank(message = "Código é obrigatório")
    private String code;
    
    private String description;
    
    @NotBlank(message = "Recurso é obrigatório")
    private String resource;
    
    @NotBlank(message = "Ação é obrigatória")
    private String action;
}
