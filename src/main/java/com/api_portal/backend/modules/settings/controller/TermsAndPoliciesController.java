package com.api_portal.backend.modules.settings.controller;

import com.api_portal.backend.modules.settings.dto.TermsAndPoliciesDTO;
import com.api_portal.backend.modules.settings.dto.UpdateTermsAndPoliciesRequest;
import com.api_portal.backend.modules.settings.service.TermsAndPoliciesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings/terms-and-policies")
@RequiredArgsConstructor
public class TermsAndPoliciesController {

    private final TermsAndPoliciesService service;

    @GetMapping
    public ResponseEntity<TermsAndPoliciesDTO> getActiveTermsAndPolicies() {
        return ResponseEntity.ok(service.getActiveTermsAndPolicies());
    }

    @PutMapping
    public ResponseEntity<TermsAndPoliciesDTO> updateTermsAndPolicies(
            @Valid @RequestBody UpdateTermsAndPoliciesRequest request,
            Authentication authentication) {
        
        // TEMPORÁRIO: Sem validação para teste
        String userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            userId = jwt.getSubject();
        }
        
        return ResponseEntity.ok(service.updateTermsAndPolicies(request, userId));
    }
}
