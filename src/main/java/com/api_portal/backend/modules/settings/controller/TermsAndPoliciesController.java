package com.api_portal.backend.modules.settings.controller;

import com.api_portal.backend.modules.settings.dto.TermsAndPoliciesDTO;
import com.api_portal.backend.modules.settings.dto.UpdateTermsAndPoliciesRequest;
import com.api_portal.backend.modules.settings.service.TermsAndPoliciesService;
import com.api_portal.backend.shared.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings/terms-and-policies")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "Gerenciamento de configurações da plataforma")
@SecurityRequirement(name = "bearer-jwt")
public class TermsAndPoliciesController {

    private final TermsAndPoliciesService service;

    @GetMapping
    @Operation(summary = "Obter termos e políticas ativos", description = "Endpoint público para visualização dos termos de serviço e política de privacidade")
    public ResponseEntity<TermsAndPoliciesDTO> getActiveTermsAndPolicies() {
        return ResponseEntity.ok(service.getActiveTermsAndPolicies());
    }

    @PutMapping
    @RequiresPermission("settings.manage")
    @Operation(summary = "Atualizar termos e políticas", description = "Atualiza os termos de serviço e política de privacidade (apenas SUPER_ADMIN)")
    public ResponseEntity<TermsAndPoliciesDTO> updateTermsAndPolicies(
            @Valid @RequestBody UpdateTermsAndPoliciesRequest request,
            Authentication authentication) {
        
        String userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            userId = jwt.getSubject();
        }
        
        return ResponseEntity.ok(service.updateTermsAndPolicies(request, userId));
    }
}
