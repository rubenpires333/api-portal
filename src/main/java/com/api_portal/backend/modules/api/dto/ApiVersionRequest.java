package com.api_portal.backend.modules.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ApiVersionRequest {
    
    @NotBlank(message = "Versão é obrigatória")
    @Pattern(regexp = "^v?\\d+(\\.\\d+)?(\\.\\d+)?$", message = "Formato de versão inválido (ex: v1, v1.0, v1.0.0)")
    private String version;
    
    private String description;
    
    private Boolean isDefault = false;
    
    private String baseUrl;
    
    private String openApiSpec;
}
