package com.api_portal.backend.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request para criar API Key")
public class ApiKeyRequest {
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    @Schema(description = "Nome da API Key", example = "Produção - App Mobile")
    private String name;
    
    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    @Schema(description = "Descrição da API Key", example = "Chave para acesso da aplicação mobile em produção")
    private String description;
    
    @Schema(description = "Dias até expiração (null = sem expiração)", example = "365")
    private Integer expiresInDays;
}
