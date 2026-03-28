package com.api_portal.backend.modules.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApiCategoryRequest {
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 50, message = "Nome deve ter entre 3 e 50 caracteres")
    private String name;
    
    @NotBlank(message = "Descrição é obrigatória")
    private String description;
    
    private String iconUrl;
    
    private Integer displayOrder = 0;
}
