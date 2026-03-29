package com.api_portal.backend.modules.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ApiEndpointRequest {
    
    @NotBlank(message = "Path é obrigatório")
    @Pattern(regexp = "^/.*", message = "Path deve começar com /")
    private String path;
    
    @NotBlank(message = "Método HTTP é obrigatório")
    @Pattern(regexp = "^(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD)$", message = "Método HTTP inválido")
    private String method;
    
    @NotBlank(message = "Resumo é obrigatório")
    private String summary;
    
    private String description;
    
    private List<String> tags = new ArrayList<>();
    
    private Boolean requiresAuth = true;
    
    private String authHeadersJson;
    
    private String authQueryParamsJson;
    
    private String requestExample;
    
    private String responseExample;
}
