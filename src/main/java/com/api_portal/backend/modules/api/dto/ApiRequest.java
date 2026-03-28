package com.api_portal.backend.modules.api.dto;

import com.api_portal.backend.modules.api.domain.enums.ApiVisibility;
import com.api_portal.backend.modules.api.domain.enums.AuthType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class ApiRequest {
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    private String name;
    
    @NotBlank(message = "Descrição curta é obrigatória")
    @Size(max = 500, message = "Descrição curta deve ter no máximo 500 caracteres")
    private String shortDescription;
    
    @NotBlank(message = "Descrição é obrigatória")
    private String description;
    
    @NotNull(message = "Categoria é obrigatória")
    private UUID categoryId;
    
    @NotNull(message = "Visibilidade é obrigatória")
    private ApiVisibility visibility;
    
    @NotBlank(message = "URL base é obrigatória")
    private String baseUrl;
    
    private String documentationUrl;
    
    private String termsOfServiceUrl;
    
    @NotNull(message = "Tipo de autenticação é obrigatório")
    private AuthType authType;
    
    private String logoUrl;
    
    private String iconUrl;
    
    private List<String> tags = new ArrayList<>();
    
    private Integer rateLimit;
    
    private String rateLimitPeriod;
    
    private Boolean requiresApproval = false;
}
