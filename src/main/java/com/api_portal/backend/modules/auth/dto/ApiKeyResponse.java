package com.api_portal.backend.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response com dados da API Key")
public class ApiKeyResponse {
    
    @Schema(description = "ID da API Key")
    private UUID id;
    
    @Schema(description = "Valor da API Key (apenas na criação)", example = "abc123def456...")
    private String keyValue;
    
    @Schema(description = "Nome da API Key", example = "Produção - App Mobile")
    private String name;
    
    @Schema(description = "Descrição da API Key", example = "Chave para acesso da aplicação mobile")
    private String description;
    
    @Schema(description = "Status ativo", example = "true")
    private Boolean active;
    
    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;
    
    @Schema(description = "Data de expiração")
    private LocalDateTime expiresAt;
    
    @Schema(description = "Último uso")
    private LocalDateTime lastUsedAt;
}
