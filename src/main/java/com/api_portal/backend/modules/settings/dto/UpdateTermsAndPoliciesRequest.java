package com.api_portal.backend.modules.settings.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTermsAndPoliciesRequest {
    
    @NotBlank(message = "Termos de Serviço são obrigatórios")
    private String termsOfService;
    
    @NotBlank(message = "Política de Privacidade é obrigatória")
    private String privacyPolicy;
    
    private String version;
}
