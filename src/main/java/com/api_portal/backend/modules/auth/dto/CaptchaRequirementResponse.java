package com.api_portal.backend.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta indicando se CAPTCHA é necessário")
public class CaptchaRequirementResponse {
    
    @Schema(description = "Indica se CAPTCHA é necessário", example = "true")
    private boolean requiresCaptcha;
    
    @Schema(description = "Indica se o usuário está bloqueado", example = "false")
    private boolean isBlocked;
    
    @Schema(description = "Minutos restantes de bloqueio", example = "0")
    private long blockedMinutesRemaining;
    
    @Schema(description = "Mensagem informativa", example = "CAPTCHA necessário após 3 tentativas falhadas")
    private String message;
}
