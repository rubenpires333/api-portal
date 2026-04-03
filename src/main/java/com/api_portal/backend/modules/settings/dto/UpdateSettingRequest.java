package com.api_portal.backend.modules.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para atualizar configuração")
public class UpdateSettingRequest {
    
    @NotBlank(message = "Chave é obrigatória")
    @Schema(description = "Chave da configuração", example = "recaptcha.site.key")
    private String key;
    
    @Schema(description = "Novo valor da configuração")
    private String value;
}
