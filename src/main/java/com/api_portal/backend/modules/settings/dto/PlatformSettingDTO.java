package com.api_portal.backend.modules.settings.dto;

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
@Schema(description = "Configuração da plataforma")
public class PlatformSettingDTO {
    
    @Schema(description = "ID da configuração")
    private UUID id;
    
    @Schema(description = "Chave da configuração", example = "recaptcha.site.key")
    private String key;
    
    @Schema(description = "Valor da configuração (oculto se isSecret=true)", example = "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI")
    private String value;
    
    @Schema(description = "Tipo do valor", example = "STRING")
    private String type;
    
    @Schema(description = "Categoria", example = "SECURITY")
    private String category;
    
    @Schema(description = "Descrição da configuração")
    private String description;
    
    @Schema(description = "Se é um valor secreto (senha, chave API)")
    private Boolean isSecret;
    
    @Schema(description = "Se é público (acessível sem autenticação)")
    private Boolean isPublic;
    
    @Schema(description = "Usuário que atualizou")
    private String updatedBy;
    
    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;
    
    @Schema(description = "Data de atualização")
    private LocalDateTime updatedAt;
}
